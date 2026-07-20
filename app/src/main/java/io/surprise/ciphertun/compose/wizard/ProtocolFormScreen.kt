package io.surprise.ciphertun.compose.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.surprise.ciphertun.config.OutboundProfile
import io.surprise.ciphertun.config.ProfileWizardRepository
import io.surprise.ciphertun.config.ProtocolType
import io.surprise.ciphertun.config.TlsConfig
import io.surprise.ciphertun.config.TransportConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolFormScreen(
    protocol: ProtocolType,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var remark by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(protocol.defaultPort.toString()) }

    var method by remember { mutableStateOf("2022-blake3-aes-128-gcm") }
    var password by remember { mutableStateOf("") }
    var uuid by remember { mutableStateOf("") }
    var alterId by remember { mutableStateOf("0") }
    var security by remember { mutableStateOf("auto") }
    var flow by remember { mutableStateOf("none") }
    var authString by remember { mutableStateOf("") }
    var obfs by remember { mutableStateOf("") }
    var upMbps by remember { mutableStateOf("100") }
    var downMbps by remember { mutableStateOf("100") }
    var congestionControl by remember { mutableStateOf("bbr") }
    var privateKey by remember { mutableStateOf("") }
    var peerPublicKey by remember { mutableStateOf("") }
    var presharedKey by remember { mutableStateOf("") }
    var localAddress by remember { mutableStateOf("10.0.0.2/32") }
    var mtu by remember { mutableStateOf("1408") }
    var username by remember { mutableStateOf("") }

    var tlsEnabled by remember { mutableStateOf(protocol != ProtocolType.SOCKS && protocol != ProtocolType.HTTP) }
    var serverName by remember { mutableStateOf("") }
    var insecure by remember { mutableStateOf(false) }

    var transportType by remember { mutableStateOf("none") }
    var wsPath by remember { mutableStateOf("/") }
    var wsHost by remember { mutableStateOf("") }
    var grpcService by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(protocol.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            LabeledField("Name", remark) { remark = it }
            LabeledField("Server", server) { server = it }
            LabeledField("Port", port, numeric = true) { port = it }

            when (protocol) {
                ProtocolType.SHADOWSOCKS -> {
                    DropdownField(
                        label = "Method",
                        value = method,
                        options = listOf(
                            "2022-blake3-aes-128-gcm",
                            "2022-blake3-aes-256-gcm",
                            "aes-256-gcm",
                            "chacha20-poly1305"
                        ),
                        onSelected = { method = it }
                    )
                    LabeledField("Password", password) { password = it }
                }

                ProtocolType.VMESS -> {
                    LabeledField("UUID", uuid) { uuid = it }
                    LabeledField("Alter ID", alterId, numeric = true) { alterId = it }
                    DropdownField(
                        label = "Security",
                        value = security,
                        options = listOf("auto", "aes-128-gcm", "chacha20-poly1305", "none"),
                        onSelected = { security = it }
                    )
                    TransportSection(
                        transportType = transportType,
                        onTransportTypeChange = { transportType = it },
                        wsPath = wsPath,
                        onWsPathChange = { wsPath = it },
                        wsHost = wsHost,
                        onWsHostChange = { wsHost = it },
                        grpcService = grpcService,
                        onGrpcServiceChange = { grpcService = it }
                    )
                    TlsSection(
                        enabled = tlsEnabled,
                        onEnabledChange = { tlsEnabled = it },
                        serverName = serverName,
                        onServerNameChange = { serverName = it },
                        insecure = insecure,
                        onInsecureChange = { insecure = it }
                    )
                }

                ProtocolType.VLESS -> {
                    LabeledField("UUID", uuid) { uuid = it }
                    DropdownField(
                        label = "Flow",
                        value = flow,
                        options = listOf("none", "xtls-rprx-vision"),
                        onSelected = { flow = it }
                    )
                    TransportSection(
                        transportType = transportType,
                        onTransportTypeChange = { transportType = it },
                        wsPath = wsPath,
                        onWsPathChange = { wsPath = it },
                        wsHost = wsHost,
                        onWsHostChange = { wsHost = it },
                        grpcService = grpcService,
                        onGrpcServiceChange = { grpcService = it }
                    )
                    TlsSection(
                        enabled = tlsEnabled,
                        onEnabledChange = { tlsEnabled = it },
                        serverName = serverName,
                        onServerNameChange = { serverName = it },
                        insecure = insecure,
                        onInsecureChange = { insecure = it }
                    )
                }

                ProtocolType.TROJAN -> {
                    LabeledField("Password", password) { password = it }
                    TransportSection(
                        transportType = transportType,
                        onTransportTypeChange = { transportType = it },
                        wsPath = wsPath,
                        onWsPathChange = { wsPath = it },
                        wsHost = wsHost,
                        onWsHostChange = { wsHost = it },
                        grpcService = grpcService,
                        onGrpcServiceChange = { grpcService = it }
                    )
                    TlsSection(
                        enabled = tlsEnabled,
                        onEnabledChange = { tlsEnabled = it },
                        serverName = serverName,
                        onServerNameChange = { serverName = it },
                        insecure = insecure,
                        onInsecureChange = { insecure = it }
                    )
                }

                ProtocolType.HYSTERIA -> {
                    LabeledField("Auth String", authString) { authString = it }
                    LabeledField("Obfs", obfs) { obfs = it }
                    LabeledField("Up Mbps", upMbps, numeric = true) { upMbps = it }
                    LabeledField("Down Mbps", downMbps, numeric = true) { downMbps = it }
                    TlsSection(
                        enabled = tlsEnabled,
                        onEnabledChange = { tlsEnabled = it },
                        serverName = serverName,
                        onServerNameChange = { serverName = it },
                        insecure = insecure,
                        onInsecureChange = { insecure = it }
                    )
                }

                ProtocolType.HYSTERIA2 -> {
                    LabeledField("Password", password) { password = it }
                    LabeledField("Obfs Password", obfs) { obfs = it }
                    TlsSection(
                        enabled = tlsEnabled,
                        onEnabledChange = { tlsEnabled = it },
                        serverName = serverName,
                        onServerNameChange = { serverName = it },
                        insecure = insecure,
                        onInsecureChange = { insecure = it }
                    )
                }

                ProtocolType.TUIC -> {
                    LabeledField("UUID", uuid) { uuid = it }
                    LabeledField("Password", password) { password = it }
                    DropdownField(
                        label = "Congestion Control",
                        value = congestionControl,
                        options = listOf("bbr", "cubic", "new_reno"),
                        onSelected = { congestionControl = it }
                    )
                    TlsSection(
                        enabled = tlsEnabled,
                        onEnabledChange = { tlsEnabled = it },
                        serverName = serverName,
                        onServerNameChange = { serverName = it },
                        insecure = insecure,
                        onInsecureChange = { insecure = it }
                    )
                }

                ProtocolType.WIREGUARD -> {
                    LabeledField("Private Key", privateKey) { privateKey = it }
                    LabeledField("Peer Public Key", peerPublicKey) { peerPublicKey = it }
                    LabeledField("Preshared Key", presharedKey) { presharedKey = it }
                    LabeledField("Local Address", localAddress) { localAddress = it }
                    LabeledField("MTU", mtu, numeric = true) { mtu = it }
                }

                ProtocolType.SOCKS, ProtocolType.HTTP -> {
                    LabeledField("Username", username) { username = it }
                    LabeledField("Password", password) { password = it }
                }
            }

            Button(
                onClick = {
                    val outboundProfile = buildProfile(
                        protocol = protocol,
                        remark = remark,
                        server = server,
                        port = port.toIntOrNull() ?: protocol.defaultPort,
                        method = method,
                        password = password,
                        uuid = uuid,
                        alterId = alterId.toIntOrNull() ?: 0,
                        security = security,
                        flow = flow,
                        authString = authString,
                        obfs = obfs,
                        upMbps = upMbps.toIntOrNull() ?: 100,
                        downMbps = downMbps.toIntOrNull() ?: 100,
                        congestionControl = congestionControl,
                        privateKey = privateKey,
                        peerPublicKey = peerPublicKey,
                        presharedKey = presharedKey,
                        localAddress = localAddress,
                        mtu = mtu.toIntOrNull() ?: 1408,
                        username = username,
                        tls = TlsConfig(
                            enabled = tlsEnabled,
                            serverName = serverName,
                            insecure = insecure
                        ),
                        transport = buildTransport(transportType, wsPath, wsHost, grpcService)
                    )
                    scope.launch {
                        ProfileWizardRepository.saveProfile(context, outboundProfile)
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

private fun buildTransport(
    type: String,
    wsPath: String,
    wsHost: String,
    grpcService: String
): TransportConfig = when (type) {
    "ws" -> TransportConfig.Ws(path = wsPath, host = wsHost)
    "grpc" -> TransportConfig.Grpc(serviceName = grpcService)
    else -> TransportConfig.None
}

private fun buildProfile(
    protocol: ProtocolType,
    remark: String,
    server: String,
    port: Int,
    method: String,
    password: String,
    uuid: String,
    alterId: Int,
    security: String,
    flow: String,
    authString: String,
    obfs: String,
    upMbps: Int,
    downMbps: Int,
    congestionControl: String,
    privateKey: String,
    peerPublicKey: String,
    presharedKey: String,
    localAddress: String,
    mtu: Int,
    username: String,
    tls: TlsConfig,
    transport: TransportConfig
): OutboundProfile = when (protocol) {
    ProtocolType.SHADOWSOCKS -> OutboundProfile.Shadowsocks(remark, server, port, method, password)
    ProtocolType.VMESS -> OutboundProfile.VMess(remark, server, port, uuid, alterId, security, transport, tls)
    ProtocolType.VLESS -> OutboundProfile.VLess(remark, server, port, uuid, flow, transport, tls)
    ProtocolType.TROJAN -> OutboundProfile.Trojan(remark, server, port, password, transport, tls)
    ProtocolType.HYSTERIA -> OutboundProfile.Hysteria(remark, server, port, authString, obfs, upMbps, downMbps, tls)
    ProtocolType.HYSTERIA2 -> OutboundProfile.Hysteria2(remark, server, port, password, obfs, tls)
    ProtocolType.TUIC -> OutboundProfile.Tuic(remark, server, port, uuid, password, congestionControl, tls)
    ProtocolType.WIREGUARD -> OutboundProfile.WireGuard(remark, server, port, privateKey, peerPublicKey, presharedKey, localAddress, mtu)
    ProtocolType.SOCKS -> OutboundProfile.Socks(remark, server, port, username, password)
    ProtocolType.HTTP -> OutboundProfile.Http(remark, server, port, username, password)
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    numeric: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TlsSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    serverName: String,
    onServerNameChange: (String) -> Unit,
    insecure: Boolean,
    onInsecureChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("TLS", modifier = Modifier.padding(end = 12.dp))
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
    if (enabled) {
        LabeledField("Server Name (SNI)", serverName, onValueChange = onServerNameChange)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Allow Insecure", modifier = Modifier.padding(end = 12.dp))
            Switch(checked = insecure, onCheckedChange = onInsecureChange)
        }
    }
}

@Composable
private fun TransportSection(
    transportType: String,
    onTransportTypeChange: (String) -> Unit,
    wsPath: String,
    onWsPathChange: (String) -> Unit,
    wsHost: String,
    onWsHostChange: (String) -> Unit,
    grpcService: String,
    onGrpcServiceChange: (String) -> Unit
) {
    DropdownField(
        label = "Transport",
        value = transportType,
        options = listOf("none", "ws", "grpc"),
        onSelected = onTransportTypeChange
    )
    when (transportType) {
        "ws" -> {
            LabeledField("Path", wsPath, onValueChange = onWsPathChange)
            LabeledField("Host", wsHost, onValueChange = onWsHostChange)
        }
        "grpc" -> {
            LabeledField("Service Name", grpcService, onValueChange = onGrpcServiceChange)
        }
    }
}
