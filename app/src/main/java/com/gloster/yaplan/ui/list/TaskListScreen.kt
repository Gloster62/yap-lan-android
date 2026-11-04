package com.gloster.yaplan.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.IndeterminateCheckBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gloster.yaplan.data.Task
import com.gloster.yaplan.ui.components.CategoryDot
import com.gloster.yaplan.ui.components.ChoiceChip
import com.gloster.yaplan.ui.components.LocalAppSnackbar
import com.gloster.yaplan.ui.components.PrimaryButton
import com.gloster.yaplan.ui.components.ProgressCard
import com.gloster.yaplan.ui.components.TaskRow
import com.gloster.yaplan.ui.components.WidgetPromoCard
import com.gloster.yaplan.ui.components.YapSnackbarHost
import com.gloster.yaplan.ui.edit.NewTaskSheet
import com.gloster.yaplan.ui.formatHeaderDate
import com.gloster.yaplan.ui.theme.PlexMono
import com.gloster.yaplan.ui.theme.Yap
import com.gloster.yaplan.widget.ADD_WIDGET_BY_HAND
import com.gloster.yaplan.widget.TaskListWidgetReceiver
import com.gloster.yaplan.widget.TaskWidgetReceiver
import com.gloster.yaplan.widget.rememberWidgetPromo
import com.gloster.yaplan.widget.requestTaskListWidget
import kotlinx.coroutines.launch

@Composable
fun TaskListScreen(
    onOpenTask: (Long) -> Unit,
    startWithNewTask: Boolean = false,
    viewModel: TaskListViewModel = viewModel(factory = TaskListViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showNewTask by rememberSaveable { mutableStateOf(startWithNewTask) }
    val context = LocalContext.current
    val snackbar = LocalAppSnackbar.current
    val scope = rememberCoroutineScope()
    val promo = rememberWidgetPromo("tasks", TaskListWidgetReceiver::class.java, TaskWidgetReceiver::class.java)

    Scaffold(
        containerColor = Yap.Background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { YapSnackbarHost() },
        floatingActionButton = {
            if (state.hasAnyTask) {
                ExtendedFloatingActionButton(
                    onClick = { showNewTask = true },
                    containerColor = Yap.Accent,
                    contentColor = Yap.OnAccent,
                    shape = RoundedCornerShape(6.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Görev ekle", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (state.loaded) {
                TaskListContent(
                    state = state,
                    onTab = viewModel::setTab,
                    onCategory = viewModel::setCategory,
                    onToggle = viewModel::toggleDone,
                    onDelete = viewModel::delete,
                    onOpenTask = onOpenTask,
                    onAddTask = { showNewTask = true },
                    promo = {
                        if (promo.visible) {
                            // Turned-off notifications are the moment to offer the widget instead.
                            val notificationsOff = !NotificationManagerCompat.from(context).areNotificationsEnabled()
                            WidgetPromoCard(
                                title = if (notificationsOff) "Bildirimleri sevmiyor musun?" else "Görevlerin ana ekranda",
                                body = if (notificationsOff) {
                                    "Görevlerini ana ekrandaki widget'tan takip et."
                                } else {
                                    "Yapılacaklar widget'ıyla uygulamayı açmadan işaretle."
                                },
                                onAdd = {
                                    if (!requestTaskListWidget(context)) {
                                        scope.launch { snackbar.showSnackbar(ADD_WIDGET_BY_HAND) }
                                    }
                                },
                                onDismiss = promo.dismiss,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    },
                )
            }
            CelebrationBadge(
                celebration = state.celebration,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 168.dp),
            )
        }
    }

    if (showNewTask) {
        NewTaskSheet(onDismiss = { showNewTask = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskListContent(
    state: TaskListUiState,
    onTab: (TaskTab) -> Unit,
    onCategory: (Long?) -> Unit,
    onToggle: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onOpenTask: (Long) -> Unit,
    onAddTask: () -> Unit,
    promo: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Header(state)
        if (!state.hasAnyTask) {
            FirstRunEmpty(onAddTask, Modifier.weight(1f))
            return@Column
        }
        ProgressCard(state.progress.done, state.progress.total, "bugün bitti", progressMessage(state.progress))
        promo()
        Tabs(state.filter.tab, state.tabCounts, onTab)
        if (state.chips.isNotEmpty()) {
            CategoryChips(state.chips, state.filter.categoryId, onCategory)
        } else {
            Box(Modifier.height(12.dp))
        }
        HorizontalDivider(color = Yap.Divider)
        if (state.sections.isEmpty()) {
            FilterEmpty(Modifier.weight(1f))
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            state.sections.forEach { section ->
                stickyHeader(key = "header-${section.kind}") {
                    SectionHeader(section)
                }
                items(section.tasks, key = { it.id }) { task ->
                    SwipeToDelete(
                        taskTitle = task.title,
                        onDelete = { onDelete(task) },
                        modifier = Modifier.animateItem(),
                    ) {
                        TaskRow(
                            task = task,
                            category = task.categoryId?.let(state.categoriesById::get),
                            now = state.now,
                            today = state.today,
                            onClick = { onOpenTask(task.id) },
                            onToggle = { onToggle(task) },
                            progress = state.checklist[task.id],
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: TaskListUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                append("YAP LAN")
                withStyle(SpanStyle(color = Yap.Accent)) { append("!") }
            },
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
        Text(formatHeaderDate(state.today), fontSize = 13.sp, color = Yap.TextMuted)
    }
}

@Composable
private fun Tabs(selected: TaskTab, counts: Map<TaskTab, Int>, onSelect: (TaskTab) -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Yap.Surface)
            .border(1.dp, Yap.Border, shape)
            .selectableGroup(),
    ) {
        TaskTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .background(if (isSelected) Yap.AccentContainer else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val color = if (isSelected) Yap.Accent else Yap.TextMuted
                    Text(tab.label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${counts[tab] ?: 0}", color = color, fontFamily = PlexMono, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                if (isSelected) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Yap.Accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChips(chips: List<CategoryChip>, selectedId: Long?, onSelect: (Long?) -> Unit) {
    LazyRow(
        modifier = Modifier
            .height(56.dp)
            .selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(chips, key = { it.category?.id ?: -1L }) { chip ->
            val id = chip.category?.id
            val selected = id == selectedId
            val name = chip.category?.name ?: "Hepsi"
            ChoiceChip(
                selected = selected,
                onClick = { onSelect(id) },
                modifier = Modifier.semantics {
                    contentDescription = (if (id == null) "Tüm kategoriler" else "$name kategorisi") +
                        ", ${chip.openCount} yapılacak"
                },
            ) {
                chip.category?.let { CategoryDot(Color(it.color), size = 9.dp) }
                Text(name)
                Text(
                    "${chip.openCount}",
                    fontFamily = PlexMono,
                    fontSize = 12.sp,
                    color = if (selected) Yap.Accent else Yap.TextFaint,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(section: TaskSection) {
    val color = when (section.kind) {
        SectionKind.OVERDUE -> Yap.Danger
        SectionKind.TODAY -> Yap.Text
        SectionKind.DONE -> Yap.TextFaint
        else -> Yap.TextMuted
    }
    Column(Modifier.background(Yap.Background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .padding(horizontal = 16.dp)
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(section.kind.label, modifier = Modifier.weight(1f), color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${section.tasks.size}", color = color, fontFamily = PlexMono, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        HorizontalDivider(color = Yap.Divider)
    }
}

@Composable
private fun SwipeToDelete(
    taskTitle: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Not rememberSaveable: the list would restore a "dismissed" state when "Geri al" brings the
    // same task back, and SwipeToDismissBox would delete it again straight away.
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
    SwipeToDismissBox(
        state = state,
        modifier = modifier.semantics {
            customActions = listOf(CustomAccessibilityAction("“$taskTitle” görevini sil") { onDelete(); true })
        },
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete() },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Yap.DeleteZone)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    Modifier
                        .height(40.dp)
                        .background(Yap.Danger, RoundedCornerShape(6.dp))
                        .padding(start = 10.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Yap.OnAccent, modifier = Modifier.size(18.dp))
                    Text("Sil", color = Yap.OnAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
    ) {
        content()
    }
}

@Composable
private fun CelebrationBadge(celebration: Celebration?, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf(celebration) }
    if (celebration != null) last = celebration
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(celebration) {
        if (celebration != null) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    AnimatedVisibility(
        visible = celebration != null,
        modifier = modifier,
        enter = fadeIn(tween(200)) + scaleIn(tween(260), initialScale = 0.96f) +
            slideInVertically(tween(260)) { it / 6 },
        exit = fadeOut(tween(200)),
    ) {
        val shown = last ?: return@AnimatedVisibility
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .background(Yap.Accent, RoundedCornerShape(8.dp))
                .padding(horizontal = 22.dp, vertical = 14.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Yap.OnAccent, modifier = Modifier.size(24.dp))
                Text(
                    if (shown.allDoneToday) "HEPSİNİ YAPTIN LAN!" else "YAPTIN LAN!",
                    color = Yap.OnAccent,
                    fontSize = if (shown.allDoneToday) 20.sp else 24.sp,
                    lineHeight = 28.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
            Text(
                buildString {
                    append("“${shown.taskTitle}” bitti")
                    if (shown.isToday) append(" · bugün ${shown.progress.done}/${shown.progress.total}")
                },
                color = Yap.OnAccent,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FirstRunEmpty(onAddTask: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Outlined.CheckBox, contentDescription = null, tint = Yap.EmptyIcon, modifier = Modifier.size(56.dp))
        Text("Henüz görev yok", fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "İlk görevini ekle, zamanı gelince YAP LAN! sana hatırlatsın.",
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Yap.TextMuted,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            "İlk görevini ekle",
            onClick = onAddTask,
            modifier = Modifier.padding(top = 14.dp),
            leading = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
        )
    }
}

@Composable
private fun FilterEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Outlined.IndeterminateCheckBox, contentDescription = null, tint = Yap.EmptyIcon, modifier = Modifier.size(40.dp))
        Text("Burada görev yok", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Başka bir sekme ya da kategori seç.", fontSize = 14.sp, color = Yap.TextMuted, textAlign = TextAlign.Center)
    }
}
