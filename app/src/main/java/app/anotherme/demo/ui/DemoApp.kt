package app.anotherme.demo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.anotherme.demo.*
import app.anotherme.demo.data.EntrustedTask
import app.anotherme.demo.domain.Motion
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val T = DemoTheme
private val clockFormat = DateTimeFormatter.ofPattern("HH:mm")
private fun etaText(minute: Long) = LocalTime.ofSecondOfDay(Math.floorMod(minute, 1440L) * 60L).format(clockFormat)

@Composable
fun DemoApp(model: DemoViewModel) {
    val ui by model.ui.collectAsState()
    val tasks by model.tasks.collectAsState()
    var editing by remember { mutableStateOf<EntrustedTask?>(null) }
    var deleting by remember { mutableStateOf<EntrustedTask?>(null) }
    LaunchedEffect(ui.motion) {
        if (ui.motion != Motion.PARKED) { editing = null; deleting = null }
    }
    BackHandler(ui.mode != ScreenMode.MIN || editing != null || deleting != null) {
        when { editing != null -> editing = null; deleting != null -> deleting = null; else -> model.minimize() }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = T.amber, onPrimary = T.black,
        background = T.black, surface = T.panel, onSurface = T.white, outline = T.line)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(T.black), contentAlignment = Alignment.Center) {
            val original = LocalDensity.current
            val scale = minOf(maxWidth.value / T.referenceWidth, maxHeight.value / T.referenceHeight)
            CompositionLocalProvider(LocalDensity provides Density(original.density * scale, 1f)) {
                Row(Modifier.requiredSize(T.referenceWidth.dp, T.referenceHeight.dp).background(T.black)) {
                    Rail(ui, tasks, model)
                    if (ui.mode != ScreenMode.MIN) {
                        Column(Modifier.width(T.half - T.rail).fillMaxHeight().padding(T.contentPadding)) {
                            ProposalPanel(ui, model, Modifier.weight(1f))
                            DemoControls(ui, model)
                        }
                        if (ui.mode == ScreenMode.FULL) {
                            Box(Modifier.width(T.hairline).fillMaxHeight().background(T.line))
                            Column(Modifier.weight(1f).fillMaxHeight().padding(T.contentPadding)) {
                                Timeline(ui)
                                TaskList(tasks, ui.motion == Motion.PARKED,
                                    onAdd = { editing = EntrustedTask() },
                                    onEdit = { editing = it }, onDelete = { deleting = it },
                                    modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SmallButton(if (ui.style == ButtonStyle.SATIN_BAR) "스타일 A · 새틴 바" else "스타일 B · 다크 키", model::toggleStyle)
                                    Spacer(Modifier.weight(1f))
                                    Label("차종별 버튼 표현 예시", muted = true, tiny = true)
                                }
                            }
                        }
                    }
                }
                editing?.let { task ->
                    TaskEditor(task, ui.motion == Motion.PARKED, onDismiss = { editing = null },
                        onSave = { updated, done -> model.save(updated, { editing = null }, done) })
                }
                deleting?.let { task ->
                    AlertDialog(onDismissRequest = { deleting = null }, title = { Text("맡긴 일을 삭제할까요?") },
                        text = { Text("${task.date} ${task.time} · ${task.request}") },
                        confirmButton = { TextButton(onClick = { model.delete(task.id) { deleting = null } }) { Text("삭제") } },
                        dismissButton = { TextButton(onClick = { deleting = null }) { Text("취소") } })
                }
                ui.error?.let { error ->
                    AlertDialog(onDismissRequest = model::clearError, title = { Text("저장소 확인") },
                        text = { Text(error) }, confirmButton = { TextButton(onClick = model::clearError) { Text("확인") } })
                }
            }
        }
    }
}

@Composable
private fun Rail(ui: DemoUiState, tasks: List<EntrustedTask>, model: DemoViewModel) {
    Column(Modifier.width(T.rail).fillMaxHeight().border(T.hairline, T.line)
        .padding(T.outerPadding), horizontalAlignment = Alignment.CenterHorizontally) {
        BarKey("맡긴 일", ui.style, tasks.isNotEmpty(), model::expand)
        Spacer(Modifier.height(T.gap))
        BarKey(if (ui.proactive) "먼저 챙김 · 켬" else "먼저 챙김 · 끔", ui.style, ui.proactive, model::toggleProactive)
        Spacer(Modifier.height(T.gap))
        BarKey(when (ui.mode) { ScreenMode.MIN -> "펼치기"; ScreenMode.MID -> "전체 화면"; ScreenMode.FULL -> "접기" },
            ui.style, ui.mode != ScreenMode.MIN, model::cycleMode)
        Spacer(Modifier.weight(1f))
        Dial(ui, tasks, model::askAi)
        Spacer(Modifier.height(T.smallGap))
        Label(when { ui.inCall -> "통화 중 · 기다림"; ui.guidance -> "길 안내 중 · 기다림";
            ui.hasAttention -> "할 말이 있어요"; else -> "AI · 눌러서 확인" }, muted = true, tiny = true)
    }
}

@Composable
private fun BarKey(text: String, style: ButtonStyle, active: Boolean, onClick: () -> Unit) {
    val satin = style == ButtonStyle.SATIN_BAR
    val shape = RoundedCornerShape(T.keyCorner)
    Column(Modifier.fillMaxWidth().height(T.keyHeight).clip(shape)
        .background(if (satin) T.black else T.panel)
        .then(if (satin) Modifier else Modifier.border(T.hairline, T.silverMid, shape))
        .clickable(role = Role.Button, onClick = onClick).padding(T.smallGap),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Text(text, color = T.white, lineHeight = T.smallLineHeight, fontSize = T.small, fontWeight = FontWeight.Medium, maxLines = 1)
        Box(Modifier.fillMaxWidth().height(T.keyFaceHeight).clip(shape)
            .background(Brush.verticalGradient(if (satin) listOf(T.silverTop, T.silverMid, T.silverBottom)
                else listOf(T.darkTop, T.darkBottom))), contentAlignment = Alignment.Center) {
            Box(Modifier.size(T.indicatorWidth, T.indicatorHeight)
                .background(if (active) T.amber else if (satin) T.darkBottom else T.muted, shape))
        }
    }
}

@Composable
private fun Dial(ui: DemoUiState, tasks: List<EntrustedTask>, onClick: () -> Unit) {
    val nextTime = tasks.sortedBy { "${it.date}T${it.time}" }.firstOrNull()?.time ?: "15:00"
    Box(Modifier.size(T.dial).semantics { contentDescription = "AI 버튼" }
        .clip(CircleShape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val ring = T.dialRing.toPx()
            drawCircle(Brush.linearGradient(listOf(T.silverMid, T.silverTop, T.silverMid)), radius = size.minDimension / 2)
            drawCircle(T.panel, radius = size.minDimension / 2 - ring)
            for (i in 0..25) {
                val angle = Math.toRadians((135 + i * 10).toDouble())
                val r = size.minDimension / 2 - ring * 2
                val endR = r - ring / 2
                drawLine(if (ui.hasAttention && i > 20) T.amber else T.line,
                    Offset(center.x + kotlin.math.cos(angle).toFloat() * r, center.y + kotlin.math.sin(angle).toFloat() * r),
                    Offset(center.x + kotlin.math.cos(angle).toFloat() * endR, center.y + kotlin.math.sin(angle).toFloat() * endR),
                    T.indicatorHeight.toPx(), StrokeCap.Round)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Label(if (ui.hasAttention) "약속 15:00" else "다음 약속", muted = true, tiny = true)
            Text(if (ui.hasAttention) "+8분" else nextTime, color = if (ui.hasAttention) T.amber else T.white,
                lineHeight = T.dialLineHeight, fontSize = T.dialValue, fontWeight = FontWeight.SemiBold)
            Label(if (ui.hasAttention) "도착 ${ui.eta}" else "또 다른 나", muted = true, tiny = true)
        }
    }
}

@Composable
private fun ProposalPanel(ui: DemoUiState, model: DemoViewModel, modifier: Modifier) {
    Column(modifier.fillMaxWidth()) {
        Label("또 다른 나, 아이(AI)  ·  14:48", muted = true)
        Spacer(Modifier.height(T.outerPadding))
        when {
            ui.proposal != null -> {
                val p = ui.proposal
                Text("${p.lateMinutes}분 늦을 예정이에요.", color = T.amber, lineHeight = T.titleLineHeight, fontSize = T.title,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(T.smallGap))
                Text("${p.appointment.recipient}님께 도착 예정 시간을\n문자로 보낼까요?", color = T.white, lineHeight = T.subtitleLineHeight, fontSize = T.subtitle)
                Spacer(Modifier.height(T.outerPadding))
                Detail("받는 사람", p.appointment.recipient)
                Detail("보낼 내용", "도착 예정 ${etaText(p.etaMinute)} · ${p.lateMinutes}분 늦음")
                Spacer(Modifier.height(T.smallGap))
                Label("시연: 실제 발송 안 됨", amber = true, tiny = true)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(T.gap)) {
                    ConfirmButton("보내 줘", true, { model.respond(true) }, Modifier.weight(1f))
                    ConfirmButton("괜찮아", false, { model.respond(false) }, Modifier.weight(1f))
                }
            }
            ui.result != null -> {
                Text("확인했어요.", color = T.white, lineHeight = T.titleLineHeight, fontSize = T.title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(T.outerPadding))
                Text(ui.result, color = T.white, lineHeight = T.subtitleLineHeight, fontSize = T.subtitle)
                Spacer(Modifier.height(T.outerPadding))
                Label("시연: 실제 발송 안 됨", amber = true)
            }
            ui.inCall || ui.guidance -> {
                Text("잠시 기다릴게요.", color = T.white, lineHeight = T.titleLineHeight, fontSize = T.title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(T.outerPadding))
                Text(if (ui.inCall) "통화에 집중하세요." else "길 안내가 끝난 뒤에\n필요한 도움을 전할게요.", color = T.muted, lineHeight = T.subtitleLineHeight, fontSize = T.subtitle)
                Spacer(Modifier.height(T.outerPadding))
                Label("도착 예상 ${ui.eta} · 최신 정보로 확인 중", amber = true)
            }
            else -> {
                Text("필요한 순간,\n다음 일을 함께.", color = T.white, lineHeight = T.titleLineHeight, fontSize = T.title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(T.outerPadding))
                Text("15:00 강남역 약속\n지금 도착 예상 ${ui.eta}", color = T.muted, lineHeight = T.subtitleLineHeight, fontSize = T.subtitle)
                Spacer(Modifier.height(T.outerPadding))
                Label(if (ui.proactive) "먼저 챙김이 켜져 있어요." else "먼저 챙김이 꺼져 있어요. AI 버튼으로 확인하세요.", muted = true)
                Spacer(Modifier.height(T.smallGap))
                Label("아래 재생 버튼으로 지연 상황을 시연해요.", muted = true, tiny = true)
            }
        }
    }
}

@Composable
private fun DemoControls(ui: DemoUiState, model: DemoViewModel) {
    Column(Modifier.fillMaxWidth().padding(top = T.outerPadding)) {
        HorizontalDivider(color = T.line)
        Spacer(Modifier.height(T.smallGap))
        Label("시연 데이터 · ${ui.stage}", muted = true, tiny = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(T.smallGap)) {
            SmallButton(if (ui.running) "재생 중…" else "14:48 상황 재생", model::playScenario, !ui.running)
            SmallButton("초기화", model::reset)
            SmallButton(if (ui.motion == Motion.PARKED) "P · 주차" else "D · 주행", model::toggleMotion, !ui.running)
        }
    }
}

@Composable
private fun Timeline(ui: DemoUiState) {
    Label("오늘 시간 관계 · 시연 예시", muted = true)
    Spacer(Modifier.height(T.outerPadding))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Label("14:48 지금"); Label("14:55 처음 예상", muted = true); Label("15:00 약속"); Label("${ui.eta} 도착", amber = ui.eta == "15:08")
    }
    Canvas(Modifier.fillMaxWidth().height(T.contentPadding)) {
        drawLine(T.line, Offset(0f, center.y), Offset(size.width, center.y), T.hairline.toPx())
        if (ui.eta == "15:08") drawLine(T.amber, Offset(size.width * .65f, center.y), Offset(size.width, center.y), T.timelineStroke.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun TaskList(tasks: List<EntrustedTask>, parked: Boolean, onAdd: () -> Unit,
                     onEdit: (EntrustedTask) -> Unit, onDelete: (EntrustedTask) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("맡긴 일 · ${tasks.size}", color = T.white, lineHeight = T.subtitleLineHeight, fontSize = T.subtitle)
            Spacer(Modifier.weight(1f))
            SmallButton("+ 맡긴 일 추가", onAdd, parked)
        }
        Label(if (parked) "시간 · 목적지 · 연락 상대 · 부탁을 확인해 저장해요." else "주차 중에만 추가·수정·삭제할 수 있어요.", muted = true, tiny = true)
        Spacer(Modifier.height(T.smallGap))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (tasks.isEmpty()) {
                Spacer(Modifier.height(T.contentPadding))
                Text("아직 맡긴 일이 없어요.", color = T.muted, lineHeight = T.bodyLineHeight, fontSize = T.body)
                Label("시연 약속은 재생에만 쓰며 목록에 저장되지 않아요.", muted = true, tiny = true)
            }
            tasks.sortedBy { "${it.date}T${it.time}" }.forEach { task ->
                Column(Modifier.fillMaxWidth().padding(vertical = T.listRowPadding)) {
                    Label("${task.date} ${task.time} · ${task.recipient}", muted = true, tiny = true)
                    Text(task.request, color = T.white, lineHeight = T.bodyLineHeight, fontSize = T.body)
                    Label(task.destination, muted = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Label("경로 연결 필요", amber = true, tiny = true)
                        Spacer(Modifier.weight(1f))
                        SmallButton("수정", { onEdit(task) }, parked)
                        SmallButton("삭제", { onDelete(task) }, parked)
                    }
                }
                HorizontalDivider(color = T.line)
            }
        }
    }
}

@Composable
private fun Detail(label: String, value: String) {
    HorizontalDivider(color = T.line)
    Row(Modifier.fillMaxWidth().padding(vertical = T.gap), horizontalArrangement = Arrangement.SpaceBetween) {
        Label(label, muted = true)
        Spacer(Modifier.width(T.gap))
        Text(value, color = T.white, lineHeight = T.bodyLineHeight, fontSize = T.body, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ConfirmButton(text: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier.height(T.actionHeight), enabled = enabled,
        shape = RoundedCornerShape(T.actionCorner),
        colors = ButtonDefaults.buttonColors(containerColor = if (primary) T.amber else T.darkTop,
            contentColor = if (primary) T.black else T.white)) {
        Text(text, lineHeight = T.bodyLineHeight, fontSize = T.body, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(horizontal = T.smallGap)) {
        Text(text, lineHeight = T.smallLineHeight, fontSize = T.small, color = if (enabled) T.white else T.muted.copy(alpha = .45f))
    }
}

@Composable
private fun Label(text: String, muted: Boolean = false, amber: Boolean = false, tiny: Boolean = false) {
    Text(text, color = if (amber) T.amber else if (muted) T.muted else T.white, lineHeight = if (tiny) T.tinyLineHeight else T.smallLineHeight, fontSize = if (tiny) T.tiny else T.small)
}

@Composable
private fun TaskEditor(initial: EntrustedTask, parked: Boolean, onDismiss: () -> Unit,
                       onSave: (EntrustedTask, () -> Unit) -> Unit) {
    var date by remember { mutableStateOf(initial.date) }
    var time by remember { mutableStateOf(initial.time) }
    var destination by remember { mutableStateOf(initial.destination) }
    var recipient by remember { mutableStateOf(initial.recipient) }
    var request by remember { mutableStateOf(initial.request) }
    var confirming by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val editorScroll = rememberScrollState()
    LaunchedEffect(confirming) { editorScroll.scrollTo(0) }
    val dateValid = runCatching { LocalDate.parse(date) }.isSuccess && date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    val timeValid = runCatching { LocalTime.parse(time) }.isSuccess && time.matches(Regex("\\d{2}:\\d{2}"))
    val valid = dateValid && timeValid && destination.isNotBlank() && recipient.isNotBlank() && request.isNotBlank()
    Dialog(onDismissRequest = { if (!saving) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.width(T.formWidth).heightIn(max = T.formHeight), shape = RoundedCornerShape(T.actionCorner), color = T.panel) {
            Column(Modifier.padding(T.contentPadding).verticalScroll(editorScroll), verticalArrangement = Arrangement.spacedBy(T.gap)) {
                Text(if (confirming) "맡길 내용을 확인해 주세요" else "맡긴 일 입력 · 주차 중", lineHeight = T.subtitleLineHeight, fontSize = T.subtitle, color = T.white)
                if (!confirming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(T.gap)) {
                        OutlinedTextField(date, { date = it.take(10) }, label = { Text("날짜 (YYYY-MM-DD)") },
                            isError = !dateValid, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(time, { time = it.take(5) }, label = { Text("시간 (HH:mm)") },
                            isError = !timeValid, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(destination, { destination = it.take(160) }, label = { Text("목적지 · 지점 또는 주소") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(recipient, { recipient = it.take(40) }, label = { Text("연락 상대") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(request, { request = it.take(240) }, label = { Text("부탁") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                } else {
                    Detail("약속", "$date $time")
                    Detail("목적지", destination.trim())
                    Detail("연락 상대", recipient.trim())
                    Text(request.trim(), color = T.white, lineHeight = T.bodyLineHeight, fontSize = T.body)
                    Label("경로 연결 필요 · 실제 내비·연락처와 연결되지 않았어요.", amber = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(T.gap)) {
                    ConfirmButton(if (confirming) "돌아가 수정" else "취소", false,
                        { if (confirming) confirming = false else onDismiss() }, Modifier.weight(1f), !saving)
                    ConfirmButton(if (saving) "저장 중…" else if (confirming) "확인하고 저장" else "내용 확인", true, {
                        if (!confirming) confirming = true else {
                            saving = true
                            onSave(initial.copy(date = date, time = time, destination = destination.trim(),
                                recipient = recipient.trim(), request = request.trim())) { saving = false }
                        }
                    }, Modifier.weight(1f), valid && parked && !saving)
                }
                if (!valid) Label("날짜·시간 형식과 모든 입력란을 확인해 주세요.", muted = true, tiny = true)
            }
        }
    }
}
