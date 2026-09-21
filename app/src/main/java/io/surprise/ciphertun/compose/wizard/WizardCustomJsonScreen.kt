package io.surprise.ciphertun.compose.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.nekohasekai.libbox.Libbox
import io.surprise.ciphertun.compose.base.SelectableMessageDialog
import io.surprise.ciphertun.config.ProfileWizardRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * "Custom (JSON)" — the escape hatch for anyone who already has a full
 * sing-box config and just wants to paste it in, matching NPV's raw-JSON
 * option. Validated with the real Libbox.checkConfig (not just JSON syntax),
 * so a broken config is caught before it's saved, same as the guided forms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardCustomJsonScreen(
    initialName: String,
    onNavigateBack: () -> Unit,
    onSaved: (profileId: Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialName) }
    var json by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    errorMessage?.let { message ->
        SelectableMessageDialog(
            title = "Couldn't save",
            message = message,
            onDismiss = { errorMessage = null },
        )
    }

    fun format() {
        try {
            val pretty = JSONObject(json).toString(2)
            json = pretty
            validationError = null
        } catch (e: Exception) {
            validationError = "Can't format — this isn't valid JSON."
        }
    }

    fun save() {
        if (name.isBlank()) {
            errorMessage = "Give this config a name."
            return
        }
        if (json.isBlank()) {
            errorMessage = "Paste a config first."
            return
        }
        isSaving = true
        scope.launch {
            try {
                val profile = ProfileWizardRepository.saveRawConfig(context, name, json)
                isSaving = false
                onSaved(profile.id)
            } catch (e: Exception) {
                isSaving = false
                // Libbox.checkConfig's message is the actual sing-box parser
                // error — genuinely more useful here than a generic one.
                errorMessage = e.message ?: "Invalid config"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom (JSON)") },
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Remarks") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Sing-box JSON", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { format() }) { Text("Format") }
                }
                Text(
                    "Used exactly as written. The wizard's own routing/DNS defaults are not applied to custom JSON configs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                validationError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = json,
                    onValueChange = {
                        json = it
                        validationError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions.Default,
                )
            }

            Button(
                onClick = { save() },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Save")
                }
            }
        }
    }
}
