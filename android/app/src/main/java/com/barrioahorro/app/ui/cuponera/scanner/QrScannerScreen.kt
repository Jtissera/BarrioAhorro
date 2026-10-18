package com.barrioahorro.app.ui.cuponera.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barrioahorro.app.data.cuponera.ScanRedemptionDto
import com.barrioahorro.app.ui.cuponera.ContributionTopBar
import com.barrioahorro.app.ui.theme.CardWhite
import com.barrioahorro.app.ui.theme.Cream
import com.barrioahorro.app.ui.theme.ErrorRed
import com.barrioahorro.app.ui.theme.Ink
import com.barrioahorro.app.ui.theme.InkBlack
import com.barrioahorro.app.ui.theme.OnPrimary
import com.barrioahorro.app.ui.theme.Primary
import com.barrioahorro.app.ui.theme.PrimaryContainer
import com.barrioahorro.app.ui.theme.Secondary
import com.barrioahorro.app.ui.theme.WarmGray
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    viewModel: QrScannerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var deniedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (!granted) deniedOnce = true
    }

    // Si el usuario concede el permiso desde los ajustes, al volver se detecta.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasPermission = context.hasCameraPermission() }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    when (val current = state) {
        is ScannerUiState.Finished -> ScanResultContent(
            outcome = current.outcome,
            data = current.data,
            onScanAgain = viewModel::scanAgain,
            onBack = onBack,
        )

        is ScannerUiState.Failed -> ScanErrorContent(
            message = current.message,
            onRetry = viewModel::retry,
            onScanAgain = viewModel::scanAgain,
            onBack = onBack,
        )

        else -> if (hasPermission) {
            CameraContent(
                processing = current is ScannerUiState.Processing,
                onCode = viewModel::onCodeScanned,
                onBack = onBack,
            )
        } else {
            val activity = context.findActivity()
            val canAskAgain = !deniedOnce ||
                    (activity != null &&
                            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA))

            PermissionContent(
                canAskAgain = canAskAgain,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                },
                onBack = onBack,
            )
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ---- Cámara ----------------------------------------------------------------------

@Composable
private fun CameraContent(
    processing: Boolean,
    onCode: (String) -> Unit,
    onBack: () -> Unit,
) {
    var torchOn by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        CameraPreview(
            paused = processing,
            torchOn = torchOn,
            onCode = onCode,
            onTorchAvailable = { hasTorch = it },
            onError = { cameraFailed = true },
            modifier = Modifier.fillMaxSize(),
        )

        ViewfinderOverlay(
            modifier = Modifier
                .align(Alignment.Center)
                .size(260.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Text(
                text = "Escáner QR",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (hasTorch) {
                IconButton(onClick = { torchOn = !torchOn }) {
                    Icon(
                        imageVector = if (torchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = if (torchOn) "Apagar linterna" else "Encender linterna",
                        tint = Color.White,
                    )
                }
            }
        }

        Text(
            text = "Apuntá la cámara al código QR del vecino",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 40.dp),
        )

        if (processing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text("Validando canje…", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (cameraFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No pudimos iniciar la cámara. Cerrá y volvé a abrir el escáner.",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    paused: Boolean,
    torchOn: Boolean,
    onCode: (String) -> Unit,
    onTorchAvailable: (Boolean) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    val latestOnCode by rememberUpdatedState(onCode)
    val latestOnTorchAvailable by rememberUpdatedState(onTorchAvailable)
    val latestOnError by rememberUpdatedState(onError)
    val latestPaused by rememberUpdatedState(paused)

    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val analyzer = QrAnalyzer(
            isActive = { !latestPaused },
            onCode = { latestOnCode(it) },
        )
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener(
            {
                try {
                    val cameraProvider = providerFuture.get()
                    provider = cameraProvider

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }

                    cameraProvider.unbindAll()
                    val bound = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                    camera = bound
                    latestOnTorchAvailable(bound.cameraInfo.hasFlashUnit())
                } catch (e: Exception) {
                    latestOnError()
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            provider?.unbindAll()
            executor.shutdown()
            analyzer.close()
        }
    }

    LaunchedEffect(camera, torchOn) {
        camera?.cameraControl?.enableTorch(torchOn)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Solo QR: ignora cualquier otro formato de código de barras. */
private class QrAnalyzer(
    private val isActive: () -> Boolean,
    private val onCode: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build(),
    )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || !isActive()) {
            imageProxy.close()
            return
        }

        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { codes ->
                codes.firstNotNullOfOrNull { it.rawValue }?.let(onCode)
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() = scanner.close()
}

@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val length = size.minDimension * 0.18f
        val stroke = 5.dp.toPx()

        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(Color.White, Offset(x, y), Offset(x + dx * length, y), stroke, StrokeCap.Round)
            drawLine(Color.White, Offset(x, y), Offset(x, y + dy * length), stroke, StrokeCap.Round)
        }

        corner(0f, 0f, 1f, 1f)
        corner(size.width, 0f, -1f, 1f)
        corner(0f, size.height, 1f, -1f)
        corner(size.width, size.height, -1f, -1f)
    }
}

// ---- Sin permiso -----------------------------------------------------------------

@Composable
private fun PermissionContent(
    canAskAgain: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        ContributionTopBar(title = "Escáner QR", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.NoPhotography, contentDescription = null, tint = Primary)
            }
            Text(
                text = "Necesitamos acceso a la cámara",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Para validar los cupones de los vecinos en el mostrador, Barrio Ahorro necesita usar tu cámara.",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
                textAlign = TextAlign.Center,
            )
            if (!canAskAgain) {
                Text(
                    text = "Activá el permiso de cámara desde los ajustes del teléfono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGray,
                    textAlign = TextAlign.Center,
                )
            }
            Button(
                onClick = if (canAskAgain) onRequest else onOpenSettings,
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(if (canAskAgain) "Permitir cámara" else "Abrir ajustes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---- Resultado del canje ---------------------------------------------------------

private data class ResultCopy(val title: String, val message: String)

private fun ScanOutcome.copy(): ResultCopy = when (this) {
    ScanOutcome.VALIDATED -> ResultCopy("Canje validado", "Ya podés entregarle el producto al vecino.")
    ScanOutcome.ALREADY_USED -> ResultCopy("No se pudo validar", "Este código ya fue usado.")
    ScanOutcome.EXPIRED -> ResultCopy("No se pudo validar", "Este código venció.")
    ScanOutcome.CANCELLED -> ResultCopy("No se pudo validar", "Este canje fue cancelado.")
    ScanOutcome.OTHER_BUSINESS -> ResultCopy("No se pudo validar", "Este código es de otro comercio.")
    ScanOutcome.NOT_FOUND -> ResultCopy("No se pudo validar", "No reconocemos este código QR.")
    ScanOutcome.INSUFFICIENT_POINTS ->
        ResultCopy("No se pudo validar", "El vecino no tiene puntos suficientes para este canje.")
    ScanOutcome.UNKNOWN -> ResultCopy("No se pudo validar", "No pudimos validar este código.")
}

private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val SCAN_ZONE: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

private fun formatDateTime(iso: String?): String? = iso?.let {
    runCatching { OffsetDateTime.parse(it).atZoneSameInstant(SCAN_ZONE).format(DATE_TIME) }.getOrDefault(it)
}

@Composable
private fun ScanResultContent(
    outcome: ScanOutcome,
    data: ScanRedemptionDto,
    onScanAgain: () -> Unit,
    onBack: () -> Unit,
) {
    val success = outcome == ScanOutcome.VALIDATED
    val copy = outcome.copy()
    val dateLabel = when (outcome) {
        ScanOutcome.VALIDATED -> "Validado el"
        ScanOutcome.ALREADY_USED -> "Usado el"
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        ContributionTopBar(title = "Resultado del canje", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(if (success) Secondary else ErrorRed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (success) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(44.dp),
                )
            }

            Text(
                text = copy.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
                textAlign = TextAlign.Center,
            )
            Text(
                text = copy.message,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
                textAlign = TextAlign.Center,
            )

            if (data.productName != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CardWhite,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DetailRow("Producto", data.productName)
                        data.neighbor?.let { DetailRow("Vecino", it) }
                        data.points?.let { DetailRow("Puntos", "$it pts") }
                        data.receiptCode?.let { DetailRow("Código de canje", it) }
                        if (dateLabel != null) {
                            formatDateTime(data.date)?.let { DetailRow(dateLabel, it) }
                        }
                    }
                }
            }
        }

        ResultActions(
            primaryLabel = if (success) "Escanear otro" else "Escanear de nuevo",
            onPrimary = onScanAgain,
            onBack = onBack,
        )
    }
}

@Composable
private fun ScanErrorContent(
    message: String,
    onRetry: () -> Unit,
    onScanAgain: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
    ) {
        ContributionTopBar(title = "Resultado del canje", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(ErrorRed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(44.dp))
            }
            Text(
                text = "No pudimos validar el canje",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = InkBlack,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGray,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "El canje no se registró. Reintentá con el mismo código.",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray,
                textAlign = TextAlign.Center,
            )
        }

        ResultActions(primaryLabel = "Reintentar", onPrimary = onRetry, onBack = onBack, secondaryLabel = "Escanear otro código", onSecondary = onScanAgain)
    }
}

@Composable
private fun ResultActions(
    primaryLabel: String,
    onPrimary: () -> Unit,
    onBack: () -> Unit,
    secondaryLabel: String = "Volver a la cuponera",
    onSecondary: () -> Unit = onBack,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Button(
            onClick = onPrimary,
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text(primaryLabel, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onSecondary) {
            Text(secondaryLabel, color = Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = WarmGray,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
    }
}