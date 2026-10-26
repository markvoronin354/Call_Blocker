package com.markvoronin.callblocker

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.markvoronin.callblocker.ui.theme.MyApplicationTheme
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FilterScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun FilterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val roleManager = remember { context.getSystemService(RoleManager::class.java) }
    val settings = remember { ScreenerSettings(context) }
    val log = remember { DecisionLog(context) }

    var isActive by remember { mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) }
    var allowVerified by remember { mutableStateOf(settings.allowVerified) }
    var blockAction by remember { mutableStateOf(settings.blockAction) }
    var decisions by remember { mutableStateOf(log.load()) }

    // The role can change in system settings and calls can arrive while we're in the background.
    val lifecycleOwner = LocalActivity.current as ComponentActivity
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isActive = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                decisions = log.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isActive = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isActive) "Filtering is on" else "Filtering is off",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (isActive) {
                            "Unsaved numbers are blocked unless your carrier verified them."
                        } else {
                            "Make this your Caller ID & spam app. Android allows only one, so " +
                                "it replaces your current call blocker."
                        }
                    )
                    if (!isActive) {
                        Button(onClick = {
                            roleRequest.launch(
                                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                            )
                        }) { Text("Turn on") }
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Allow verified callers", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Let unsaved numbers through when they show the green check in Recents.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = allowVerified,
                    onCheckedChange = {
                        allowVerified = it
                        settings.allowVerified = it
                    },
                )
            }
        }

        item {
            Column(Modifier.selectableGroup()) {
                Text("When blocking", style = MaterialTheme.typography.titleSmall)
                BlockAction.entries.forEach { action ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = blockAction == action,
                                onClick = {
                                    blockAction = action
                                    settings.blockAction = action
                                },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = blockAction == action, onClick = null)
                        Text(
                            when (action) {
                                BlockAction.REJECT -> "Reject the call"
                                BlockAction.SILENCE -> "Silence it (no ring, still shows as missed)"
                            },
                            Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                        )
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Recent decisions", style = MaterialTheme.typography.titleMedium)
                if (decisions.isNotEmpty()) {
                    TextButton(onClick = {
                        log.clear()
                        decisions = emptyList()
                    }) { Text("Clear") }
                }
            }
        }

        if (decisions.isEmpty()) {
            item {
                Text(
                    "Nothing yet. Calls from unsaved numbers will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        items(decisions) { decision ->
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(decision.number)
                    Text(
                        if (decision.allowed) "Allowed" else "Blocked",
                        color = if (decision.allowed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    "${decision.verificationLabel} · " +
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                            .format(Date(decision.time)),
                    style = MaterialTheme.typography.bodySmall,
                )
                HorizontalDivider(Modifier.padding(top = 12.dp))
            }
        }
    }
}
