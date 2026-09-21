package io.surprise.ciphertun.compose.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.surprise.ciphertun.compat.menuAnchorCompat
import io.surprise.ciphertun.compose.base.SelectableMessageDialog
import io.surprise.ciphertun.config.OutboundProfile
import io.surprise.ciphertun.config.ProfileWizardRepository
import io.surprise.ciphertun.config.ProtocolType
import io.surprise.ciphertun.config.TlsConfig
import io.surprise.ciphertun.config.TransportConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardCredentialFormScreen(
    protocol: ProtocolType,
    initialName: String,
    onNavigateBack: () -> Unit,
    onSaved: (profileId: Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var remark by remember { mutableStateOf(initialName) }
    var server by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(if (protocol.defaultPort > 0) protocol.defaultPort.toString() else "") }
    var localAddress by remember { mutableStateOf("10.0.0.1, fd59:7153:2388:b5fd:0000:0000:0000:0001") }

    var password by remember { mutableStateOf("") }
    var uuid by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var publicKey by remember { mutableStateOf("") }
    var presharedKey by remember { mutableStateOf("") }

    var method by remember { mutableStateOf("2022-blake3-aes-128-gcm") }
    var vmessSecurity by remember { mutableStateOf("auto") }
    var alterId by remember { mutableStateOf("0") }
    var flow by remember { mutableStateOf("none") }
    var authString by remember { mutableStateOf("") }
    var obfs by remember { mutableStateOf("") }
    var obfsPassword by remember { mutableStateOf("") }
    var congestionControl by remember { mutableStateOf("bbr") }
    var socksAuth by remember { mutableStateOf("noauth") }
    var shadowTlsVersion by remember { mutableStateOf("3") }
    var dataDirectory by remember { mutableStateOf("") }
    var wireguardMtu by remember { mutableStateOf("1408") }
    var wireguardReserved by remember { mutableStateOf("") }
    var wireguardAllowedIps by remember { mutableStateOf("0.0.0.0/0,::/0") }
    var hysteria2UpMbps by remember { mutableStateOf("") }
    var hysteria2DownMbps by remember { mutableStateOf("") }
    var snellVersion by remember { mutableStateOf(4) }
    var snellPsk by remember { mutableStateOf("") }
    var snellUserkey by remember { mutableStateOf("") }
    var snellObfsMode by remember { mutableStateOf("none") }
    var snellObfsHost by remember { mutableStateOf("bing.com") }
    var snellMode by remember { mutableStateOf("default") }
    var ovpnNetwork by remember { mutableStateOf("udp") }
    var ovpnUsername by remember { mutableStateOf("") }
    var ovpnPassword by remember { mutableStateOf("") }
    var ovpnCaCertificate by remember { mutableStateOf("") }
    var ovpnClientCertificate by remember { mutableStateOf("") }
    var ovpnClientKey by remember { mutableStateOf("") }
    var ovpnWrapType by remember { mutableStateOf("none") }
    var ovpnWrapKey by remember { mutableStateOf("") }
    var ovpnWrapDirection by remember { mutableStateOf("") }
    var ocFlavor by remember { mutableStateOf("anyconnect") }
    var ocUsername by remember { mutableStateOf("") }
    var ocPassword by remember { mutableStateOf("") }
    var ocAuthGroup by remember { mutableStateOf("") }
    var ocInsecure by remember { mutableStateOf(false) }
    var ocCertificateAuthority by remember { mutableStateOf("") }
    var ocClientCertificate by remember { mutableStateOf("") }
    var ocClientKey by remember { mutableStateOf("") }

    // Transport ("Transfer protocol" in NetMod)
    var transportType by remember { mutableStateOf("tcp") }
    var transportPath by remember { mutableStateOf("/") }
    var transportHost by remember { mutableStateOf("") }
    var grpcServiceName by remember { mutableStateOf("") }

    // TLS
    var tlsType by remember { mutableStateOf(if (protocolUsesTlsByDefault(protocol)) "tls" else "none") }
    var tlsServerName by remember { mutableStateOf("") }
    var tlsInsecure by remember { mutableStateOf(false) }
    var tlsAlpn by remember { mutableStateOf("") }
    var tlsFingerprint by remember { mutableStateOf("") }
    var realityPublicKey by remember { mutableStateOf("") }
    var realityShortId by remember { mutableStateOf("") }
    var pinnedCertSha256 by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    errorMessage?.let { message ->
        SelectableMessageDialog(
            title = "Couldn't save",
            message = message,
            onDismiss = { errorMessage = null },
        )
    }

    fun buildTransport(): TransportConfig = when (transportType) {
        "ws" -> TransportConfig.Ws(path = transportPath, host = transportHost)
        "grpc" -> TransportConfig.Grpc(serviceName = grpcServiceName)
        "http" -> TransportConfig.Http(host = transportHost, path = transportPath)
        "httpupgrade" -> TransportConfig.HttpUpgrade(path = transportPath, host = transportHost)
        else -> TransportConfig.None
    }

    fun buildTls(): TlsConfig = TlsConfig(
        enabled = tlsType != "none",
        serverName = tlsServerName,
        insecure = tlsInsecure,
        alpn = tlsAlpn.split(",").map { it.trim() }.filter { it.isNotBlank() },
        utlsFingerprint = tlsFingerprint,
        realityPublicKey = if (tlsType == "reality") realityPublicKey else "",
        realityShortId = if (tlsType == "reality") realityShortId else "",
        pinnedCertSha256 = pinnedCertSha256.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() },
    )

    fun buildOutboundProfile(): OutboundProfile {
        val tls = buildTls()
        val transport = buildTransport()
        val portInt = port.toIntOrNull() ?: protocol.defaultPort
        return when (protocol) {
            ProtocolType.SHADOWSOCKS -> OutboundProfile.Shadowsocks(
                remark = remark, server = server, serverPort = portInt,
                method = method, password = password,
            )
            ProtocolType.VMESS -> OutboundProfile.VMess(
                remark = remark, server = server, serverPort = portInt,
                uuid = uuid, alterId = alterId.toIntOrNull() ?: 0,
                security = vmessSecurity, transport = transport, tls = tls,
            )
            ProtocolType.VLESS -> OutboundProfile.VLess(
                remark = remark, server = server, serverPort = portInt,
                uuid = uuid, flow = flow, transport = transport, tls = tls,
            )
            ProtocolType.TROJAN -> OutboundProfile.Trojan(
                remark = remark, server = server, serverPort = portInt,
                password = password, transport = transport, tls = tls,
            )
            ProtocolType.HYSTERIA -> OutboundProfile.Hysteria(
                remark = remark, server = server, serverPort = portInt,
                authString = authString, obfs = obfs, tls = tls,
            )
            ProtocolType.HYSTERIA2 -> OutboundProfile.Hysteria2(
                remark = remark, server = server, serverPort = portInt,
                password = password, obfsPassword = obfsPassword,
                upMbps = hysteria2UpMbps.toIntOrNull() ?: 0,
                downMbps = hysteria2DownMbps.toIntOrNull() ?: 0,
                tls = tls,
            )
            ProtocolType.TUIC -> OutboundProfile.Tuic(
                remark = remark, server = server, serverPort = portInt,
                uuid = uuid, password = password,
                congestionControl = congestionControl, tls = tls,
            )
            ProtocolType.WIREGUARD -> OutboundProfile.WireGuard(
                remark = remark, server = server, serverPort = portInt,
                privateKey = privateKey, peerPublicKey = publicKey,
                presharedKey = presharedKey, localAddress = localAddress,
                allowedIps = wireguardAllowedIps,
                mtu = wireguardMtu.toIntOrNull() ?: 1408,
                reserved = wireguardReserved,
            )
            ProtocolType.SOCKS -> OutboundProfile.Socks(
                remark = remark, server = server, serverPort = portInt,
                username = if (socksAuth == "password") username else "",
                password = if (socksAuth == "password") password else "",
            )
            ProtocolType.HTTP -> OutboundProfile.Http(
                remark = remark, server = server, serverPort = portInt,
                username = username, password = password,
            )
            ProtocolType.SSH -> OutboundProfile.Ssh(
                remark = remark, server = server, serverPort = portInt,
                username = username, password = password, privateKey = privateKey,
            )
            ProtocolType.SHADOWTLS -> OutboundProfile.ShadowTls(
                remark = remark, server = server, serverPort = portInt,
                password = password, version = shadowTlsVersion.toIntOrNull() ?: 3, tls = tls,
            )
            ProtocolType.ANYTLS -> OutboundProfile.AnyTls(
                remark = remark, server = server, serverPort = portInt,
                password = password, tls = tls,
            )
            ProtocolType.TOR -> OutboundProfile.Tor(
                remark = remark, dataDirectory = dataDirectory,
            )
            ProtocolType.SNELL -> OutboundProfile.Snell(
                remark = remark, server = server, serverPort = portInt,
                version = snellVersion, psk = snellPsk, userkey = snellUserkey,
                obfsMode = snellObfsMode, obfsHost = snellObfsHost, mode = snellMode,
            )
            ProtocolType.OPENVPN -> OutboundProfile.OpenVpnClient(
                remark = remark, server = server, serverPort = portInt,
                network = ovpnNetwork, username = ovpnUsername, password = ovpnPassword,
                caCertificate = ovpnCaCertificate, clientCertificate = ovpnClientCertificate,
                clientKey = ovpnClientKey, controlWrapType = ovpnWrapType,
                controlWrapKey = ovpnWrapKey, controlWrapDirection = ovpnWrapDirection,
            )
            ProtocolType.OPENCONNECT -> OutboundProfile.OpenConnectClient(
                remark = remark, server = server, serverPort = portInt,
                flavor = ocFlavor, username = ocUsername, password = ocPassword,
                authGroup = ocAuthGroup, insecure = ocInsecure,
                certificateAuthority = ocCertificateAuthority,
                clientCertificate = ocClientCertificate, clientKey = ocClientKey,
            )
        }
    }

    fun save() {
        if (remark.isBlank()) {
            errorMessage = "Give this server a name."
            return
        }
        if (protocol != ProtocolType.TOR && protocol != ProtocolType.WIREGUARD && server.isBlank()) {
            errorMessage = "Server address is required."
            return
        }
        if (protocol == ProtocolType.WIREGUARD && server.isBlank()) {
            errorMessage = "Endpoint is required."
            return
        }
        isSaving = true
        scope.launch {
            try {
                val outboundProfile = buildOutboundProfile()
                val profile = ProfileWizardRepository.saveProfile(context, outboundProfile)
                isSaving = false
                onSaved(profile.id)
            } catch (e: Exception) {
                isSaving = false
                errorMessage = e.message ?: "Unknown error"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add ${protocol.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FormSection {
                LabeledField("Remarks", remark, onValueChange = { remark = it })

                if (protocol == ProtocolType.WIREGUARD) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LabeledField(
                            "Endpoint", server, onValueChange = { server = it },
                            modifier = Modifier.weight(0.65f),
                        )
                        LabeledField(
                            "Port", port, onValueChange = { port = it },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(0.35f),
                        )
                    }
                    LabeledField(
                        "Local address", localAddress,
                        onValueChange = { localAddress = it },
                    )
                } else if (protocol != ProtocolType.TOR) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LabeledField(
                            "Address", server, onValueChange = { server = it },
                            modifier = Modifier.weight(0.65f),
                        )
                        LabeledField(
                            "Port", port, onValueChange = { port = it },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(0.35f),
                        )
                    }
                }
            }

            when (protocol) {
                ProtocolType.WIREGUARD -> FormSection {
                    LabeledField(
                        "Private key", privateKey, onValueChange = { privateKey = it },
                        isPassword = true,
                    )
                    LabeledField("Public key", publicKey, onValueChange = { publicKey = it })
                    LabeledField(
                        "Pre shared key (optional)", presharedKey,
                        onValueChange = { presharedKey = it }, isPassword = true,
                    )
                    LabeledField(
                        "Allowed IPs", wireguardAllowedIps,
                        onValueChange = { wireguardAllowedIps = it },
                    )
                    Text(
                        "0.0.0.0/0,::/0 routes all traffic through this peer (full tunnel).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LabeledField(
                        "MTU", wireguardMtu, onValueChange = { wireguardMtu = it },
                        keyboardType = KeyboardType.Number,
                    )
                    LabeledField(
                        "Reserved (e.g. 0,0,0, optional)", wireguardReserved,
                        onValueChange = { wireguardReserved = it },
                    )
                }

                ProtocolType.VMESS -> FormSection {
                    DropdownField(
                        label = "Encrypt method",
                        value = vmessSecurity,
                        options = listOf("auto", "none", "zero", "aes-128-gcm", "chacha20-poly1305"),
                        onSelected = { vmessSecurity = it },
                    )
                    LabeledField("User ID", uuid, onValueChange = { uuid = it })
                    LabeledField(
                        "Alter ID", alterId, onValueChange = { alterId = it },
                        keyboardType = KeyboardType.Number,
                    )
                }

                ProtocolType.VLESS -> FormSection {
                    DropdownField(
                        label = "Flow",
                        value = flow,
                        options = listOf("none", "xtls-rprx-vision"),
                        onSelected = { flow = it },
                    )
                    LabeledField("User ID", uuid, onValueChange = { uuid = it })
                }

                ProtocolType.TROJAN -> FormSection {
                    LabeledField(
                        "Password", password, onValueChange = { password = it },
                        isPassword = true,
                    )
                }

                ProtocolType.SHADOWSOCKS -> FormSection {
                    DropdownField(
                        label = "Encrypt method",
                        value = method,
                        options = listOf(
                            "2022-blake3-aes-128-gcm", "2022-blake3-aes-256-gcm",
                            "2022-blake3-chacha20-poly1305", "aes-128-gcm", "aes-256-gcm",
                            "chacha20-ietf-poly1305", "none",
                        ),
                        onSelected = { method = it },
                    )
                    LabeledField(
                        "Password", password, onValueChange = { password = it },
                        isPassword = true,
                    )
                }

                ProtocolType.SOCKS -> FormSection {
                    DropdownField(
                        label = "Authentication",
                        value = socksAuth,
                        options = listOf("noauth", "password"),
                        onSelected = { socksAuth = it },
                    )
                    if (socksAuth == "password") {
                        LabeledField("Username", username, onValueChange = { username = it })
                        LabeledField(
                            "Password", password, onValueChange = { password = it },
                            isPassword = true,
                        )
                    }
                }

                ProtocolType.HTTP -> FormSection {
                    LabeledField("Username (optional)", username, onValueChange = { username = it })
                    LabeledField(
                        "Password (optional)", password, onValueChange = { password = it },
                        isPassword = true,
                    )
                }

                ProtocolType.SSH -> FormSection {
                    LabeledField("Username", username, onValueChange = { username = it })
                    LabeledField(
                        "Password", password, onValueChange = { password = it },
                        isPassword = true,
                    )
                    LabeledField(
                        "Private Key (optional)", privateKey,
                        onValueChange = { privateKey = it }, isPassword = true,
                    )
                }

                ProtocolType.HYSTERIA -> FormSection {
                    LabeledField(
                        "Auth String", authString, onValueChange = { authString = it },
                        isPassword = true,
                    )
                    LabeledField("Obfuscation", obfs, onValueChange = { obfs = it })
                }

                ProtocolType.HYSTERIA2 -> FormSection {
                    LabeledField(
                        "Password", password, onValueChange = { password = it },
                        isPassword = true,
                    )
                    LabeledField(
                        "Obfuscation Password (optional)", obfsPassword,
                        onValueChange = { obfsPassword = it }, isPassword = true,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        LabeledField(
                            "Upload Mbps (0 = auto)", hysteria2UpMbps,
                            onValueChange = { hysteria2UpMbps = it },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                        LabeledField(
                            "Download Mbps (0 = auto)", hysteria2DownMbps,
                            onValueChange = { hysteria2DownMbps = it },
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                ProtocolType.TUIC -> FormSection {
                    LabeledField("UUID", uuid, onValueChange = { uuid = it })
                    LabeledField(
                        "Password", password, onValueChange = { password = it },
                        isPassword = true,
                    )
                    DropdownField(
                        label = "Congestion Control",
                        value = congestionControl,
                        options = listOf("bbr", "cubic", "new_reno"),
                        onSelected = { congestionControl = it },
                    )
                }

                ProtocolType.SHADOWTLS -> FormSection {
                    LabeledField(
                        "Password", password, onValueChange = { password = it },
                        isPassword = true,
                    )
                    DropdownField(
                        label = "Version",
                        value = shadowTlsVersion,
                        options = listOf("1", "2", "3"),
                        onSelected = { shadowTlsVersion = it },
                    )
                }

                ProtocolType.ANYTLS -> FormSection {
                    LabeledField(
                        "Password", password, onValueChange = { password = it },
                        isPassword = true,
                    )
                }

                ProtocolType.TOR -> FormSection {
                    LabeledField(
                        "Data Directory (optional)", dataDirectory,
                        onValueChange = { dataDirectory = it },
                    )
                    Text(
                        "Tor doesn't need a server address — it routes through the Tor network directly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ProtocolType.SNELL -> FormSection {
                    DropdownField(
                        "Version", snellVersion.toString(), listOf("4", "6"),
                        onSelected = { snellVersion = it.toIntOrNull() ?: 4 },
                    )
                    LabeledField(
                        "PSK", snellPsk, onValueChange = { snellPsk = it },
                        isPassword = true,
                    )
                    LabeledField(
                        "User Key (optional)", snellUserkey,
                        onValueChange = { snellUserkey = it },
                    )
                    if (snellVersion == 4) {
                        DropdownField(
                            "Obfuscation", snellObfsMode, listOf("none", "http"),
                            onSelected = { snellObfsMode = it },
                        )
                        if (snellObfsMode == "http") {
                            LabeledField(
                                "Obfuscation Host", snellObfsHost,
                                onValueChange = { snellObfsHost = it },
                            )
                        }
                    } else {
                        // version 6 requires a longer PSK and swaps obfs for
                        // traffic-shaping mode.
                        Text(
                            "Version 6 requires a PSK between 12 and 255 bytes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DropdownField(
                            "Mode", snellMode, listOf("default", "unshaped", "unsafe-raw"),
                            onSelected = { snellMode = it },
                        )
                    }
                }

                ProtocolType.OPENVPN -> FormSection {
                    DropdownField(
                        "Transport", ovpnNetwork, listOf("udp", "tcp"),
                        onSelected = { ovpnNetwork = it },
                    )
                    Text(
                        "Username/password auth (leave blank if this server only uses certificates)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LabeledField(
                        "Username (optional)", ovpnUsername,
                        onValueChange = { ovpnUsername = it },
                    )
                    LabeledField(
                        "Password (optional)", ovpnPassword,
                        onValueChange = { ovpnPassword = it }, isPassword = true,
                    )
                    Text(
                        "From your .ovpn file's <ca>, <cert>, <key>, and <tls-auth>/<tls-crypt> blocks",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MultilineField(
                        "CA Certificate", ovpnCaCertificate,
                        onValueChange = { ovpnCaCertificate = it },
                    )
                    MultilineField(
                        "Client Certificate (optional)", ovpnClientCertificate,
                        onValueChange = { ovpnClientCertificate = it },
                    )
                    MultilineField(
                        "Client Key (optional)", ovpnClientKey,
                        onValueChange = { ovpnClientKey = it },
                    )
                    DropdownField(
                        "Control Channel Wrapping", ovpnWrapType,
                        listOf("none", "tls_auth", "tls_crypt", "tls_crypt_v2"),
                        onSelected = { ovpnWrapType = it },
                    )
                    if (ovpnWrapType != "none") {
                        MultilineField(
                            "Wrapping Key", ovpnWrapKey,
                            onValueChange = { ovpnWrapKey = it },
                        )
                        if (ovpnWrapType == "tls_auth") {
                            DropdownField(
                                "Key Direction", ovpnWrapDirection.ifBlank { "(bidirectional)" },
                                listOf("(bidirectional)", "client", "server"),
                                onSelected = {
                                    ovpnWrapDirection = if (it == "(bidirectional)") "" else it
                                },
                            )
                        }
                    }
                }

                ProtocolType.OPENCONNECT -> FormSection {
                    Text(
                        "Server should be the gateway's HTTPS address, e.g. vpn.example.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DropdownField(
                        "Gateway Type", ocFlavor,
                        listOf("anyconnect", "gp", "fortinet", "f5", "pulse", "nc"),
                        onSelected = { ocFlavor = it },
                    )
                    LabeledField(
                        "Username (optional)", ocUsername,
                        onValueChange = { ocUsername = it },
                    )
                    LabeledField(
                        "Password (optional)", ocPassword,
                        onValueChange = { ocPassword = it }, isPassword = true,
                    )
                    LabeledField(
                        "Auth Group / Realm (optional)", ocAuthGroup,
                        onValueChange = { ocAuthGroup = it },
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(checked = ocInsecure, onCheckedChange = { ocInsecure = it })
                        Text("Allow insecure TLS certificate")
                    }
                    MultilineField(
                        "CA Certificate (optional)", ocCertificateAuthority,
                        onValueChange = { ocCertificateAuthority = it },
                    )
                    MultilineField(
                        "Client Certificate (optional)", ocClientCertificate,
                        onValueChange = { ocClientCertificate = it },
                    )
                    MultilineField(
                        "Client Key (optional)", ocClientKey,
                        onValueChange = { ocClientKey = it },
                    )
                }
            }

            if (protocolSupportsTransport(protocol)) {
                FormSection {
                    DropdownField(
                        label = "Transfer protocol",
                        value = transportType,
                        options = listOf("tcp", "ws", "grpc", "http", "httpupgrade"),
                        onSelected = { transportType = it },
                    )
                    when (transportType) {
                        "ws", "httpupgrade" -> {
                            LabeledField("Path", transportPath, onValueChange = { transportPath = it })
                            LabeledField(
                                "Host (optional)", transportHost,
                                onValueChange = { transportHost = it },
                            )
                        }
                        "http" -> {
                            LabeledField("Path", transportPath, onValueChange = { transportPath = it })
                            LabeledField(
                                "Host (optional)", transportHost,
                                onValueChange = { transportHost = it },
                            )
                        }
                        "grpc" -> LabeledField(
                            "Service Name", grpcServiceName,
                            onValueChange = { grpcServiceName = it },
                        )
                    }
                }
            }

            if (protocolSupportsTls(protocol)) {
                FormSection {
                    DropdownField(
                        label = "TLS type",
                        value = tlsType,
                        options = listOf("none", "tls", "reality"),
                        onSelected = { tlsType = it },
                    )
                    if (tlsType != "none") {
                        LabeledField(
                            "Server Name (SNI, optional)", tlsServerName,
                            onValueChange = { tlsServerName = it },
                        )
                        LabeledField(
                            "ALPN (comma separated, optional)", tlsAlpn,
                            onValueChange = { tlsAlpn = it },
                        )
                        LabeledField(
                            "uTLS Fingerprint (optional)", tlsFingerprint,
                            onValueChange = { tlsFingerprint = it },
                        )
                        LabeledField(
                            "Pinned Cert SHA-256 (base64, comma-separated, optional)", pinnedCertSha256,
                            onValueChange = { pinnedCertSha256 = it },
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Allow insecure (skip cert verification)")
                            Switch(checked = tlsInsecure, onCheckedChange = { tlsInsecure = it })
                        }
                    }
                    if (tlsType == "reality") {
                        LabeledField(
                            "Reality Public Key", realityPublicKey,
                            onValueChange = { realityPublicKey = it },
                        )
                        LabeledField(
                            "Reality Short ID (optional)", realityShortId,
                            onValueChange = { realityShortId = it },
                        )
                    }
                }
            }

            Button(
                onClick = { save() },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Save & Connect")
                }
            }
        }
    }
}

private fun protocolUsesTlsByDefault(protocol: ProtocolType): Boolean = when (protocol) {
    ProtocolType.SHADOWSOCKS, ProtocolType.WIREGUARD, ProtocolType.SOCKS,
    ProtocolType.HTTP, ProtocolType.SSH, ProtocolType.TOR, ProtocolType.SNELL,
    ProtocolType.OPENVPN, ProtocolType.OPENCONNECT,
    ProtocolType.HYSTERIA, ProtocolType.HYSTERIA2, ProtocolType.TUIC -> false
    else -> true
}

private fun protocolSupportsTls(protocol: ProtocolType): Boolean = when (protocol) {
    ProtocolType.VMESS, ProtocolType.VLESS, ProtocolType.TROJAN, ProtocolType.SOCKS,
    ProtocolType.SHADOWTLS, ProtocolType.ANYTLS -> true
    else -> false
}

private fun protocolSupportsTransport(protocol: ProtocolType): Boolean = when (protocol) {
    ProtocolType.VMESS, ProtocolType.VLESS, ProtocolType.TROJAN -> true
    else -> false
}

@Composable
private fun FormSection(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
        ),
        visualTransformation = if (isPassword) {
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun MultilineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = false,
        minLines = 3,
        maxLines = 8,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .then(menuAnchorCompat(true)),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
