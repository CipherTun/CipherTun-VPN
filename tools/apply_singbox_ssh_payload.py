#!/usr/bin/env python3
"""Apply the shared TCP payload/HTTP CONNECT extension to sing-box.

The patch is intentionally anchored and fails closed if upstream APIs change.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()


def replace_once(path: str, old: str, new: str) -> None:
    file = root / path
    if not file.is_file():
        raise SystemExit(f"PATCH ERROR: required upstream file missing: {path}")
    data = file.read_text()
    count = data.count(old)
    if count != 1:
        raise SystemExit(
            f"PATCH ERROR: expected one anchor in {path}, found {count}; upstream source changed"
        )
    file.write_text(data.replace(old, new, 1))


replace_once(
    "option/dialer.go",
    '''type DialerOptions struct {
\tDetour string `json:"detour,omitempty" reference:"outbound"`
\tAbstractDialerOptions
}''',
    '''type DialerOptions struct {
\tDetour string `json:"detour,omitempty" reference:"outbound"`
\tAbstractDialerOptions
\tProxyServer     string `json:"proxy_server,omitempty"`
\tProxyServerPort uint16 `json:"proxy_server_port,omitempty"`
\tPayload         string `json:"payload,omitempty"`
\tPayloadSplit    int    `json:"payload_split,omitempty"`
\tPayloadDelayMS  int    `json:"payload_delay_ms,omitempty"`
}''',
)

replace_once(
    "common/dialer/dialer.go",
    "\treturn dialer, nil\n}\n\nfunc NewDNSQueryOptions",
    "\tif dialOptions.ProxyServer != \"\" || dialOptions.Payload != \"\" {\n"
    "\t\tdialer = NewPayloadDialer(dialer, dialOptions)\n"
    "\t}\n"
    "\treturn dialer, nil\n}\n\nfunc NewDNSQueryOptions",
)

payload_source = r'''package dialer

import (
	"bufio"
	"context"
	"errors"
	"io"
	"net"
	"strconv"
	"strings"
	"time"

	"github.com/sagernet/sing-box/option"
	M "github.com/sagernet/sing/common/metadata"
	N "github.com/sagernet/sing/common/network"
)

// payloadDialer applies custom TCP bytes before the selected protocol handshake.
// HTTP CONNECT is verified before the caller begins its own protocol handshake.
type payloadDialer struct {
	upstream N.Dialer
	proxy    M.Socksaddr
	payload  string
	split    int
	delay    time.Duration
}

func NewPayloadDialer(upstream N.Dialer, options option.DialerOptions) N.Dialer {
	var proxy M.Socksaddr
	if options.ProxyServer != "" {
		port := options.ProxyServerPort
		if port == 0 {
			port = 8080
		}
		proxy = M.Socksaddr{Fqdn: options.ProxyServer, Port: port}
	}
	return &payloadDialer{
		upstream: upstream,
		proxy:    proxy,
		payload:  options.Payload,
		split:    options.PayloadSplit,
		delay:    time.Duration(options.PayloadDelayMS) * time.Millisecond,
	}
}

func (d *payloadDialer) DialContext(ctx context.Context, network string, destination M.Socksaddr) (net.Conn, error) {
	if !strings.HasPrefix(network, "tcp") {
		return nil, errors.New("custom payload and HTTP CONNECT are supported only for TCP connections")
	}
	if d.proxy.Port != 0 && strings.TrimSpace(d.payload) == "" {
		return nil, errors.New("HTTP CONNECT proxy requires a non-empty payload")
	}
	if d.split < 0 {
		return nil, errors.New("payload_split cannot be negative")
	}
	if d.delay < 0 || d.delay > 30*time.Second {
		return nil, errors.New("payload_delay_ms must be between 0 and 30000")
	}

	dialTarget := destination
	if d.proxy.Port != 0 {
		dialTarget = d.proxy
	}
	conn, err := d.upstream.DialContext(ctx, network, dialTarget)
	if err != nil {
		return nil, err
	}

	if d.payload != "" {
		host := destination.Fqdn
		if host == "" {
			host = destination.Addr.String()
		}
		payload := expandPayload(d.payload, host, destination.Port)
		if err = writePayload(ctx, conn, payload, d.split, d.delay); err != nil {
			conn.Close()
			return nil, err
		}
	}

	if d.proxy.Port != 0 {
		wrapped, responseErr := readConnectResponse(conn)
		if responseErr != nil {
			conn.Close()
			return nil, responseErr
		}
		conn = wrapped
	}
	return conn, nil
}

func expandPayload(template, host string, port uint16) []byte {
	portText := strconv.Itoa(int(port))
	hostPort := net.JoinHostPort(host, portText)
	value := strings.NewReplacer(
		"[host]", host,
		"[port]", portText,
		"[host_port]", hostPort,
		"[crlf]", "\r\n",
		"[lf]", "\n",
		"[cr]", "\r",
		"%host%", host,
		"%port%", portText,
	).Replace(template)
	return []byte(value)
}

func writePayload(ctx context.Context, conn net.Conn, payload []byte, split int, delay time.Duration) error {
	if split > 0 && split < len(payload) {
		if err := writeAll(conn, payload[:split]); err != nil {
			return err
		}
		if delay > 0 {
			timer := time.NewTimer(delay)
			defer timer.Stop()
			select {
			case <-ctx.Done():
				return ctx.Err()
			case <-timer.C:
			}
		}
		return writeAll(conn, payload[split:])
	}
	return writeAll(conn, payload)
}

func writeAll(conn net.Conn, data []byte) error {
	for len(data) > 0 {
		n, err := conn.Write(data)
		if err != nil {
			return err
		}
		if n == 0 {
			return io.ErrShortWrite
		}
		data = data[n:]
	}
	return nil
}

// payloadBufferedConn preserves bytes read past the HTTP header terminator.
type payloadBufferedConn struct {
	net.Conn
	reader *bufio.Reader
}

func (c *payloadBufferedConn) Read(p []byte) (int, error) { return c.reader.Read(p) }

func readConnectResponse(conn net.Conn) (net.Conn, error) {
	reader := bufio.NewReaderSize(conn, 4096)
	line, err := reader.ReadString('\n')
	if err != nil {
		return nil, err
	}
	fields := strings.Fields(strings.TrimSpace(line))
	if len(fields) < 2 || !strings.HasPrefix(fields[0], "HTTP/") {
		return nil, errors.New("proxy returned a non-HTTP response")
	}
	status, err := strconv.Atoi(fields[1])
	if err != nil || status < 200 || status >= 300 {
		return nil, errors.New("proxy CONNECT failed: " + strings.TrimSpace(line))
	}
	headerBytes := len(line)
	for {
		line, err = reader.ReadString('\n')
		if err != nil {
			return nil, err
		}
		headerBytes += len(line)
		if headerBytes > 32768 {
			return nil, errors.New("proxy response headers exceed 32 KiB")
		}
		if line == "\r\n" || line == "\n" {
			break
		}
	}
	return &payloadBufferedConn{Conn: conn, reader: reader}, nil
}
'''

payload_file = root / "common/dialer/payload.go"
if payload_file.exists():
    raise SystemExit("PATCH ERROR: common/dialer/payload.go already exists; refusing to overwrite")
payload_file.write_text(payload_source)
