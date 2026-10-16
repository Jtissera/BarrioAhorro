package com.barrioahorro.app.ui.screens.business

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.barrioahorro.app.ui.components.profile.BottomActionSurface
import com.barrioahorro.app.ui.components.profile.CreamPillButton
import com.barrioahorro.app.ui.components.profile.ErrorHint
import com.barrioahorro.app.ui.components.profile.PrimaryPillButton
import com.barrioahorro.app.ui.components.profile.ProfileTopBar
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.WarmGray

/**
 * "Fotos del comercio": manages the photos shown on the public profile.
 * Edits stay in a local draft until "Guardar cambios"; leaving with pending edits asks before discarding them.
 */
@Composable
fun EditBusinessPhotosScreen(
    state: EditBusinessPhotosUiState,
    onAddPhoto: (Uri) -> Unit,
    onReplacePhoto: (String, Uri) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onSetCover: (String) -> Unit,
    onSave: () -> Unit,
    onSavedMessageShown: () -> Unit,
    onRetryLoad: () -> Unit,
    onBack: () -> Unit,
) {
    val hasChanges = state.draft.hasChanges
    var confirmDiscard by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val requestBack: () -> Unit = { if (hasChanges) confirmDiscard = true else onBack() }

    BackHandler(enabled = hasChanges, onBack = { confirmDiscard = true })

    LaunchedEffect(state.showSavedMessage) {
        if (state.showSavedMessage) {
            onSavedMessageShown()
            snackbarHostState.showSnackbar("Fotos guardadas")
        }
    }

    val showContent = !state.isLoading && state.loadError == null

    // MainActivity's Scaffold already applies the system insets; they are not added again here.
    Scaffold(
        containerColor = Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { ProfileTopBar(title = "Fotos del comercio", onBack = requestBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showContent) {
                BottomActionSurface {
                    state.error?.let {
                        ErrorHint(text = it)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    PrimaryPillButton(
                        text = "Guardar cambios",
                        onClick = onSave,
                        icon = Icons.Outlined.CheckCircle,
                        enabled = hasChanges && !state.isSaving,
                        isLoading = state.isSaving,
                    )
                }
            }
        },
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }

            state.loadError != null -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ErrorHint(text = state.loadError)
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryPillButton(text = "Reintentar", onClick = onRetryLoad, height = 48.dp)
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            ) {
                item {
                    BusinessPhotosSection(
                        photos = state.draft.photos,
                        isUpdating = state.isSaving,
                        errorMessage = null,
                        onAddPhoto = onAddPhoto,
                        onReplacePhoto = onReplacePhoto,
                        onDeletePhoto = onRemovePhoto,
                        onSetCover = onSetCover,
                        businessName = state.businessName,
                    )
                }
            }
        }
    }

    if (confirmDiscard) {
        DiscardChangesDialog(
            onDiscard = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun DiscardChangesDialog(onDiscard: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        DiscardChangesDialogContent(onDiscard = onDiscard, onDismiss = onDismiss)
    }
}

/** "¿Descartar cambios?" confirmation, styled like the "¿Quitar esta foto?" dialog. */
@Composable
private fun DiscardChangesDialogContent(onDiscard: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 24.dp,
        modifier = Modifier.widthIn(max = 340.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(48.dp).background(PrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.EditOff, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "¿Descartar cambios?",
                style = MaterialTheme.typography.titleLarge,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tenés cambios en tus fotos que todavía no guardaste. Si salís ahora, se van a perder.",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            PrimaryPillButton(text = "Descartar", onClick = onDiscard, height = 50.dp)
            Spacer(modifier = Modifier.height(12.dp))
            CreamPillButton(text = "Seguir editando", onClick = onDismiss, height = 50.dp)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF7F7F7F)
@Composable
private fun DiscardChangesDialogPreview() {
    DiscardChangesDialogContent(onDiscard = {}, onDismiss = {})
}
