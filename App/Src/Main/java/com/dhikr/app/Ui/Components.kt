package com.dhikr.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dhikr.app.data.Counter
import com.dhikr.app.data.DailyMessage
import kotlinx.coroutines.delay

@Composable
fun Ring(progress: Float, done: Boolean, size: Dp, stroke: Dp, content: @Composable BoxScope.() -> Unit) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(350), label = "ring")
    val track = MaterialTheme.colorScheme.outlineVariant
    val color = if (done) Gold else MaterialTheme.colorScheme.primary
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(stroke / 2)) {
            val st = Stroke(stroke.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = st)
            drawArc(color, -90f, 360f * p, false, style = st)
        }
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CounterCard(c: Counter, onTap: () -> Unit, onLong: () -> Unit, onFocus: () -> Unit) {
    val done = c.completed
    val progress = if (c.target > 0) c.count / c.target.toFloat() else (c.count % 100) / 100f
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (done) Gold.copy(alpha = .14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (done) Gold else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).combinedClickable(onClick = onTap, onLongClick = onLong)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Ring(progress, done, 84.dp, 7.dp) {
                Text(c.count.ar(), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = if (done) Gold else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    when { done -> "✓ تم الهدف (${c.target.ar()})"; c.target > 0 -> "الهدف: ${c.target.ar()}"; else -> "بدون هدف" },
                    color = if (done) Gold else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
            IconButton(onClick = onFocus) {
                Icon(Icons.Outlined.CenterFocusStrong, "وضع التركيز", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AddCounterDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عدّاد جديد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(40) }, label = { Text("الاسم") }, singleLine = true)
                OutlinedTextField(target, { target = it.filter(Char::isDigit).take(6) },
                    label = { Text("الهدف (اختياري)") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onAdd(name, target.toIntOrNull() ?: 0) }) { Text("إضافة") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@Composable
fun DailyBanner(onOpen: () -> Unit, onClose: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary, shadowElevation = 6.dp
    ) {
        Row(Modifier.clickable(onClick = onOpen).padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoStories, null, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(12.dp))
            Text("رسالة اليوم", Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = MaterialTheme.colorScheme.onPrimary) }
        }
    }
}

@Composable
fun DailyMessageOverlay(m: DailyMessage, onCopy: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(28.dp)) {
            IconButton(onClose, Modifier.align(Alignment.TopStart)) { Icon(Icons.Default.Close, "إغلاق") }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(m.type, color = Gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp)); HorizontalDivider(Modifier.width(60.dp), color = Gold)
                Spacer(Modifier.height(28.dp))
                Text(m.text, fontSize = 28.sp, lineHeight = 48.sp, textAlign = TextAlign.Center)
                if (m.source.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    Text("﴿ ${m.source} ﴾".takeIf { m.type == "آية" } ?: m.source,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                }
            }
            FilledTonalButton(onCopy, Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("نسخ")
            }
        }
    }
}

/** Silent, non-musical celebration: expanding rings + gold check. Auto-dismisses. */
@Composable
fun CelebrationOverlay(name: String, onDone: () -> Unit) {
    val t = rememberInfiniteTransition(label = "c")
    val wave by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing)), label = "w")
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = .45f)); delay(2000); onDone() }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .7f))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(300.dp)) {
            for (k in 0..1) {
                val p = (wave + k * .5f) % 1f
                drawCircle(Gold.copy(alpha = (1f - p) * .6f), radius = size.minDimension / 2 * p, center = Offset(size.width / 2, size.height / 2), style = Stroke(4.dp.toPx()))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(pop.value)) {
            Box(Modifier.size(96.dp).background(Gold, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, null, Modifier.size(56.dp), tint = Color.White)
            }
            Spacer(Modifier.height(20.dp))
            Text("أحسنت! أتممتَ الهدف", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (name.isNotBlank()) Text("«$name»", color = Gold, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp)); Text("تقبّل الله منك", color = Color.White.copy(.8f))
        }
    }
}
