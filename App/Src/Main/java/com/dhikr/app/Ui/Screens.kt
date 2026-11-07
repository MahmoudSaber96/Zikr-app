package com.dhikr.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikr.app.MainViewModel
import com.dhikr.app.Screen
import com.dhikr.app.data.Counter
import com.dhikr.app.data.Stats
import kotlinx.coroutines.launch

/* ------------------------------ HOME ------------------------------ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val counters by vm.counters.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showDaily by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Counter?>(null) }
    val clipboard = LocalClipboardManager.current
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ذِكر", fontSize = 28.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton({ vm.screen = Screen.Settings }) { Icon(Icons.Outlined.Settings, "الإعدادات") } },
                actions = { IconButton({ vm.screen = Screen.Achievements }) { Icon(Icons.Outlined.EmojiEvents, "الإنجازات", tint = Gold) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        },
        floatingActionButton = {
            ExtendedFloatingActionButton({ showAdd = true }, containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("عدّاد جديد")
            }
        },
        bottomBar = { if (settings.bannerDismissedDay != vm.today) DailyBanner({ showDaily = true }, vm::dismissBanner) }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("إجمالي الأذكار", stats.taps.ar(), Modifier.weight(1f))
                    StatTile("الأهداف المنجزة", stats.targets.ar(), Modifier.weight(1f))
                }
            }
            if (counters.isEmpty()) item {
                Text("ابدأ بإضافة أول عدّاد لك\nمثل: «الاستغفار» أو «أذكار الصباح»",
                    Modifier.fillMaxWidth().padding(top = 48.dp), textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 28.sp)
            }
            items(counters, key = { it.id }) { c ->
                CounterCard(c,
                    onTap = { vm.tap(c.id) },
                    onLong = {
                        if (c.count > 0) { vm.reset(c); scope.launch { snack.showSnackbar("تمت إعادة «${c.name}» إلى الصفر") } }
                        else toDelete = c
                    },
                    onFocus = { vm.screen = Screen.Focus(c.id) })
            }
        }
    }
    if (showAdd) AddCounterDialog({ showAdd = false }) { n, t -> vm.add(n, t); showAdd = false }
    toDelete?.let { c ->
        AlertDialog(onDismissRequest = { toDelete = null },
            title = { Text("حذف العدّاد؟") }, text = { Text("سيتم حذف «${c.name}» نهائيًا.") },
            confirmButton = { TextButton({ vm.delete(c); toDelete = null }) { Text("حذف", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ toDelete = null }) { Text("إلغاء") } })
    }
    if (showDaily) DailyMessageOverlay(vm.dailyMessage,
        onCopy = {
            val m = vm.dailyMessage
            clipboard.setText(AnnotatedString("${m.text}\n— ${m.source}"))
            scope.launch { snack.showSnackbar("تم النسخ") }
        }, onClose = { showDaily = false })
}

/* ------------------------------ FOCUS ------------------------------ */
@Composable
fun FocusScreen(vm: MainViewModel, id: Long) {
    val counters by vm.counters.collectAsStateWithLifecycle()
    val c = counters.firstOrNull { it.id == id }
    var paused by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    val view = LocalView.current

    // Anti-cheat: leaving the app OR screen-off => ON_STOP => pause until user explicitly resumes.
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) paused = true }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    BackHandler { vm.screen = Screen.Home }

    if (c == null) { LaunchedEffect(Unit) { vm.screen = Screen.Home }; return }
    val done = c.completed
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !paused) { vm.tap(c.id) },
        contentAlignment = Alignment.Center) {
        // Only the number. Exit = system back gesture/button.
        Text(c.count.ar(), fontSize = 140.sp, fontWeight = FontWeight.Light,
            color = if (done) Gold else MaterialTheme.colorScheme.onBackground)
        if (paused) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = .97f))
            .clickable(enabled = true, onClick = {}), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.PauseCircle, null, Modifier.size(72.dp), tint = Gold)
                Spacer(Modifier.height(16.dp))
                Text("توقّف العدّاد مؤقتًا", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("نرجو منك العودة لمتابعة ذكرك بحضور قلب، وسنُكمل من حيث توقفت.",
                    textAlign = TextAlign.Center, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Button({ paused = false }) { Text("متابعة") }
            }
        }
    }
}

/* ------------------------------ ACHIEVEMENTS ------------------------------ */
private data class Badge(val title: String, val desc: String, val ok: (Stats) -> Boolean)
private val badges = listOf(
    Badge("الهدف الأول", "أكملتَ أول هدف") { it.targets >= 1 },
    Badge("١٠٠ ذكر", "أتممتَ مئة ذكر") { it.taps >= 100 },
    Badge("٥٠٠ ذكر", "خمسمئة ذكر") { it.taps >= 500 },
    Badge("١٠٠٠ ذكر", "ألف ذكر") { it.taps >= 1000 },
    Badge("١٠ أهداف", "أنجزتَ عشرة أهداف") { it.targets >= 10 },
    Badge("٥٠ هدفًا", "أنجزتَ خمسين هدفًا") { it.targets >= 50 },
    Badge("٥٠٠٠ ذكر", "خمسة آلاف ذكر") { it.taps >= 5000 },
    Badge("١٠٬٠٠٠ ذكر", "عشرة آلاف ذكر") { it.taps >= 10000 })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(vm: MainViewModel) {
    val stats by vm.stats.collectAsStateWithLifecycle()
    BackHandler { vm.screen = Screen.Home }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text("الإنجازات") }, navigationIcon = {
            IconButton({ vm.screen = Screen.Home }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }) { pad ->
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(badges) { b ->
                val on = b.ok(stats)
                Surface(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, if (on) Gold else MaterialTheme.colorScheme.outlineVariant),
                    color = if (on) Gold.copy(.14f) else MaterialTheme.colorScheme.surface, modifier = Modifier.alpha(if (on) 1f else .55f)) {
                    Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (on) Icons.Default.EmojiEvents else Icons.Outlined.Lock, null, Modifier.size(44.dp),
                            tint = if (on) Gold else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text(b.title, fontWeight = FontWeight.Bold)
                        Text(b.desc, fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/* ------------------------------ SETTINGS ------------------------------ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    BackHandler { vm.screen = Screen.Home }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text("الإعدادات") }, navigationIcon = {
            IconButton({ vm.screen = Screen.Home }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchRow("صوت النقر عند كل ذِكر", "نقرة قصيرة صامتة النغمة", s.tick, vm::setTick)
            SwitchRow("صوت إتمام الهدف", "نقرتان مميزتان عند بلوغ الهدف", s.completeSound, vm::setComplete)
            Text("التطبيق بلا إعلانات ويعمل دون اتصال بالإنترنت، وبياناتك محفوظة على جهازك فقط.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
@Composable private fun SwitchRow(t: String, sub: String, v: Boolean, on: (Boolean) -> Unit) =
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(t, fontWeight = FontWeight.SemiBold); Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Switch(v, on)
        }
    }

/* ------------------------------ ONBOARDING ------------------------------ */
@Composable
fun Onboarding(onFinish: () -> Unit) {
    val pages = listOf(
        Triple(Icons.Outlined.TouchApp, "عدّاداتك أنت", "أنشئ عدّادات بأسمائك المفضلة وحدّد لكل منها هدفًا اختياريًا."),
        Triple(Icons.Outlined.Replay, "نقرة وضغطة", "نقرة واحدة تزيد العدّ. الضغط المطوّل يُصفّر العدّاد، وعلى الصفر يعرض الحذف."),
        Triple(Icons.Outlined.CenterFocusStrong, "وضع التركيز", "شاشة خالية إلا من الرقم. إن غادرتَ التطبيق يتوقف العدّ حتى تعود."),
        Triple(Icons.Outlined.AutoStories, "رسالة اليوم", "آية أو حديث أو حكمة تتجدد كل يوم، مع إنجازات تشجّعك على المداومة."))
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onFinish) { Text("تخطّي") } }
        HorizontalPager(state, Modifier.weight(1f)) { i ->
            val (ic, t, d) = pages[i]
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(140.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(ic, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(32.dp))
                Text(t, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(d, textAlign = TextAlign.Center, lineHeight = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            repeat(pages.size) { Box(Modifier.size(if (it == state.currentPage) 24.dp else 8.dp, 8.dp)
                .background(if (it == state.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))) }
        }
        Button({ if (state.currentPage == pages.lastIndex) onFinish() else scope.launch { state.animateScrollToPage(state.currentPage + 1) } },
            Modifier.fillMaxWidth().height(52.dp)) { Text(if (state.currentPage == pages.lastIndex) "ابدأ" else "التالي") }
    }
}
