package com.gnutux.tahakom.feature.androidtv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gnutux.tahakom.R
import com.gnutux.tahakom.ui.icons.TahakomIcon
import com.gnutux.tahakom.ui.theme.tokens

/**
 * شاشة إقران Android TV (تجريبي): تتصل بالتلفاز فيعرض رمزاً من 6 خانات، يُدخله المستخدم.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidTvPairScreen(
    host: String,
    onBack: () -> Unit,
    onPaired: () -> Unit,
    viewModel: AndroidTvPairViewModel = hiltViewModel(),
) {
    val c = tokens.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(host) { viewModel.start(host) }
    LaunchedEffect(state.stage) { if (state.stage == PairStage.DONE) onPaired() }

    Scaffold(
        containerColor = c.bg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.atv_pair_title), color = c.text, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(8.dp))
                        ExperimentalChip()
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg),
                navigationIcon = {
                    Box(
                        Modifier.padding(start = 8.dp).size(40.dp).clip(RoundedCornerShape(tokens.shape.sm))
                            .background(c.surface).clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) { TahakomIcon("back", c.textDim, size = 20.dp) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.atv_pair_hint), color = c.textFaint, fontSize = 13.5.sp, textAlign = TextAlign.Center)
            when (state.stage) {
                PairStage.CONNECTING -> {
                    Spacer(Modifier.size(16.dp))
                    CircularProgressIndicator(color = c.accent)
                    Text(stringResource(R.string.atv_pair_connecting), color = c.textDim, fontSize = 13.sp)
                }
                PairStage.ENTER_CODE, PairStage.SUBMITTING, PairStage.ERROR -> {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.trim().uppercase().take(6) },
                        label = { Text(stringResource(R.string.atv_pair_code)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        shape = RoundedCornerShape(tokens.shape.md),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent, unfocusedBorderColor = c.line,
                            focusedTextColor = c.text, unfocusedTextColor = c.text,
                            focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                            focusedLabelColor = c.accent, unfocusedLabelColor = c.textFaint, cursorColor = c.accent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val busy = state.stage == PairStage.SUBMITTING
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(c.accent)
                            .clickable(enabled = !busy && code.length >= 4) { viewModel.submit(code) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = c.accentText)
                        else Text(stringResource(R.string.atv_pair_connect), color = c.accentText, fontWeight = FontWeight.Bold)
                    }
                    if (state.stage == PairStage.ERROR) {
                        Text(stringResource(R.string.atv_pair_failed), color = c.ir, fontSize = 12.5.sp, textAlign = TextAlign.Center)
                    }
                }
                PairStage.DONE -> Text(stringResource(R.string.atv_pair_ok), color = c.accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExperimentalChip() {
    val c = tokens.colors
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(c.bridgeSoft).padding(horizontal = 9.dp, vertical = 3.dp),
    ) { Text(stringResource(R.string.experimental), color = c.bridge, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}
