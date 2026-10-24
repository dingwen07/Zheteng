package net.extrawdw.apps.zheteng.ui.main

import net.extrawdw.apps.zheteng.R
import net.extrawdw.apps.zheteng.data.Challenge
import net.extrawdw.apps.zheteng.data.Finish
import net.extrawdw.apps.zheteng.data.RedirectReason
import net.extrawdw.apps.zheteng.data.Screen
import net.extrawdw.apps.zheteng.data.Shop
import net.extrawdw.apps.zheteng.theme.BrandCoral
import net.extrawdw.apps.zheteng.theme.BrandPaper
import net.extrawdw.apps.zheteng.theme.LocalAppColors
import net.extrawdw.apps.zheteng.theme.ZhetengTheme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Shape = RoundedCornerShape(24.dp)

@Composable
fun ZhetengApp(model: AdViewModel, sensorAvailable: Boolean) {
    var about by remember { mutableStateOf(false) }
    val state = model.state
    ZhetengTheme {
        val colors = LocalAppColors.current
        Surface(Modifier.fillMaxSize(), color = colors.paper) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )) {
                Header(state.screen, model.seconds, onAbout = { about = true })
                HorizontalDivider(color = colors.line, modifier = Modifier.padding(horizontal = 24.dp))
                // Layout depends on window width only; hinge posture is used solely by the challenge.
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (state.screen) {
                        Screen.AD -> AdaptivePanels(
                            first = { compact -> AdPoster(compact) },
                            second = { compact -> ChallengePanel(state, model.demo, compact, model::sample) })
                        Screen.REDIRECT -> AdaptivePanels(
                            first = { _ -> ShopSplash(state.shop) },
                            second = { _ -> RedirectDetails(state, model::returnToAd) })
                        Screen.HOME -> AdaptivePanels(
                            first = { _ -> HomePoster(state, model.openingSeconds) },
                            second = { _ -> HomeDetails(model::restart, onAbout = { about = true }) })
                    }
                }
            }
            if (about) {
                AlertDialog(onDismissRequest = { about = false },
                    title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.Bold) },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(stringResource(R.string.about_intro))
                            Text(stringResource(R.string.about_rules))
                            Text(stringResource(R.string.about_shops))
                            Text(stringResource(R.string.about_timer))
                            Text(if (model.demo) stringResource(R.string.about_demo) else stringResource(R.string.about_sensor))
                            if (sensorAvailable && !model.demo) TextButton(onClick = { model.enableDemo(); about = false }) { Text(stringResource(R.string.switch_demo)) }
                        }
                    },
                    confirmButton = { TextButton(onClick = { about = false }) { Text(stringResource(R.string.got_it)) } })
            }
        }
    }
}

@Composable
private fun Header(screen: Screen, seconds: Int, onAbout: () -> Unit) {
    val description = stringResource(R.string.about_description)
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        FoldLogo(Modifier.size(40.dp))
        Column(Modifier.padding(start = 10.dp)) {
            Text(stringResource(R.string.app_name), fontWeight = FontWeight.Black, fontSize = 21.sp, letterSpacing = 2.sp)
            Text(stringResource(R.string.app_subtitle), fontSize = 10.sp, color = colors.muted, letterSpacing = 1.sp)
        }
        if (screen != Screen.HOME) Spacer(Modifier.weight(1f))
        if (screen == Screen.AD) {
            Row(Modifier.clip(CircleShape).background(colors.ink).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(colors.coral, CircleShape))
                Text(stringResource(R.string.ad_label), color = colors.paper, fontSize = 12.sp)
                Text(stringResource(R.string.seconds_short, seconds), color = colors.paper, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.testTag("countdown"))
            }
        } else if (screen == Screen.HOME) {
            Box(Modifier.weight(1f).padding(start = 12.dp), contentAlignment = Alignment.CenterEnd) {
                VideoLink()
            }
        }
        IconButton(onClick = onAbout, modifier = Modifier.padding(start = 3.dp).size(44.dp).testTag("about")) {
            Box(Modifier.size(22.dp).border(1.dp, colors.muted, CircleShape), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.help_symbol), color = colors.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { contentDescription = description })
            }
        }
    }
}

@Composable
private fun AdaptivePanels(
    first: @Composable (Boolean) -> Unit,
    second: @Composable (Boolean) -> Unit
) {
    val colors = LocalAppColors.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 700.dp) {
            Row(Modifier.fillMaxSize()) {
                Panel(Modifier.weight(1.08f).fillMaxHeight()) { first(false) }
                Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 24.dp).background(colors.line))
                Panel(Modifier.weight(1f).fillMaxHeight()) { second(false) }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
                verticalArrangement = Arrangement.spacedBy(28.dp)) {
                first(true)
                second(true)
            }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(28.dp)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
        verticalArrangement = Arrangement.spacedBy(20.dp)) { content() }
}

@Composable
private fun Eyebrow(text: String, color: Color? = null) {
    val accent = color ?: LocalAppColors.current.coral
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(6.dp).background(accent, CircleShape))
        Text(text, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun AdPoster(compact: Boolean) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 18.dp else 24.dp)) {
        Eyebrow(stringResource(R.string.ad_eyebrow))
        Column {
            Text(stringResource(R.string.ad_headline_first), color = colors.ink, fontSize = if (compact) 39.sp else 49.sp,
                fontWeight = FontWeight.Black, lineHeight = if (compact) 48.sp else 60.sp, letterSpacing = (-2).sp)
            Text(stringResource(R.string.ad_headline_second), color = colors.coral, fontSize = if (compact) 39.sp else 49.sp,
                fontWeight = FontWeight.Black, lineHeight = if (compact) 48.sp else 60.sp, letterSpacing = (-2).sp)
            Text(stringResource(R.string.ad_tagline), color = colors.muted, fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp))
        }
        Box(Modifier.fillMaxWidth().height(if (compact) 185.dp else 272.dp).clip(Shape).background(colors.paleCoral)) {
            FoldIllustration(Modifier.fillMaxSize().padding(5.dp))
            Text(stringResource(R.string.illustration_caption), color = colors.ink.copy(alpha = .5f), fontSize = 9.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(18.dp))
            Row(Modifier.align(Alignment.BottomStart).padding(18.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.illustration_slogan), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.ink)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.external_arrow), color = colors.coral, fontSize = 24.sp)
            }
        }
        Row(Modifier.fillMaxWidth().border(1.dp, colors.line, RoundedCornerShape(14.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.benefit_label), color = colors.muted, fontSize = 10.sp)
                Text(stringResource(R.string.benefit_title), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 5.dp))
            }
            Text(stringResource(R.string.benefit_price), color = colors.coral, fontSize = 28.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.sponsor_note), color = colors.muted, fontSize = 10.sp)
    }
}

@Composable
private fun ChallengePanel(state: Challenge, demo: Boolean, compact: Boolean, onSample: (Float) -> Unit) {
    val description = stringResource(R.string.demo_slider_description)
    val colors = LocalAppColors.current
    val angle = state.angle
    val low = state.minimum
    val over = low != null && low < Challenge.TARGET_MIN
    val inRange = angle != null && angle in Challenge.TARGET_MIN..Challenge.TARGET_MAX && !over
    val status = when {
        angle == null -> stringResource(R.string.status_waiting)
        !state.ready -> stringResource(R.string.status_unfold_first)
        over -> stringResource(R.string.status_too_deep)
        inRange -> stringResource(R.string.status_in_range)
        low != null && low in Challenge.TARGET_MIN..Challenge.TARGET_MAX -> stringResource(R.string.status_unfold)
        low != null -> stringResource(R.string.status_too_shallow)
        else -> stringResource(R.string.status_ready)
    }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.challenge_title), fontSize = if (compact) 21.sp else 25.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.challenge_index), color = colors.muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
        Column(Modifier.fillMaxWidth().clip(Shape).background(colors.panel).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.hinge_angle_label), color = colors.muted, fontSize = 11.sp)
                Text(if (demo) stringResource(R.string.sensor_mode_demo) else stringResource(R.string.sensor_mode_live), color = if (demo) colors.muted else colors.green, fontSize = 10.sp)
            }
            Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.BottomCenter) {
                AngleDial(angle ?: 180f, Modifier.fillMaxSize().padding(top = 16.dp))
                Column(Modifier.padding(bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (angle != null) stringResource(R.string.angle_value, angle.roundToInt()) else stringResource(R.string.angle_unknown), fontSize = 49.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        color = if (inRange) colors.green else colors.ink, modifier = Modifier.testTag("angle"))
                    Text(stringResource(R.string.angle_target), color = colors.green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.angle_closed), color = colors.muted, fontSize = 9.sp)
                Text(stringResource(R.string.angle_flat), color = colors.muted, fontSize = 9.sp)
            }
            HorizontalDivider(Modifier.padding(vertical = 15.dp), color = colors.line)
            Text(status, color = if (over) colors.coral else if (inRange) colors.green else colors.ink,
                fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("challenge-status"))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            StepNumber(stringResource(R.string.step_one))
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(stringResource(R.string.step_fold_title), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.step_fold_body), color = colors.muted, fontSize = 11.sp, lineHeight = 18.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            StepNumber(stringResource(R.string.step_two))
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(stringResource(R.string.step_unfold_title), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.step_unfold_body), color = colors.muted, fontSize = 11.sp, lineHeight = 18.sp)
            }
        }
        if (demo) {
            Column(Modifier.fillMaxWidth().border(1.dp, colors.line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.demo_title), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.demo_hint), color = colors.muted, fontSize = 10.sp)
                }
                Slider(value = angle ?: 180f, onValueChange = onSample, valueRange = 0f..180f,
                    colors = SliderDefaults.colors(thumbColor = colors.ink, activeTrackColor = colors.ink, inactiveTrackColor = colors.line),
                    modifier = Modifier.fillMaxWidth().height(40.dp).testTag("hinge-slider")
                        .semantics { contentDescription = description })
                Text(stringResource(R.string.demo_note), color = colors.muted, fontSize = 9.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.challenge_footer), color = colors.muted, fontSize = 10.sp)
            Text(pluralStringResource(R.plurals.redirect_count, state.redirects, state.redirects), color = colors.muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StepNumber(number: String) {
    val colors = LocalAppColors.current
    Box(Modifier.size(23.dp).border(1.dp, colors.line, CircleShape), contentAlignment = Alignment.Center) {
        Text(number, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AngleDial(angle: Float, modifier: Modifier) {
    val description = stringResource(R.string.dial_description)
    val colors = LocalAppColors.current
    val animated by animateFloatAsState(angle, tween(120), label = "hinge pointer")
    Canvas(modifier.semantics { contentDescription = description }) {
        val radius = minOf(size.width / 2 - 16.dp.toPx(), size.height - 14.dp.toPx())
        val center = Offset(size.width / 2, size.height - 4.dp.toPx())
        val origin = Offset(center.x - radius, center.y - radius)
        val bounds = Size(radius * 2, radius * 2)
        drawArc(colors.track, 180f, 180f, false, origin, bounds, style = Stroke(11.dp.toPx(), cap = StrokeCap.Round))
        drawArc(colors.green, 180f + Challenge.TARGET_MIN, Challenge.TARGET_MAX - Challenge.TARGET_MIN, false, origin, bounds, style = Stroke(11.dp.toPx(), cap = StrokeCap.Butt))
        for (i in 0..36) {
            val rad = Math.toRadians((180 + i * 5).toDouble())
            val length = if (i % 6 == 0) 10.dp.toPx() else 4.dp.toPx()
            val r = radius - 14.dp.toPx()
            drawLine(colors.muted.copy(alpha = .4f), center + Offset(cos(rad).toFloat() * r, sin(rad).toFloat() * r),
                center + Offset(cos(rad).toFloat() * (r - length), sin(rad).toFloat() * (r - length)), 1.dp.toPx())
        }
        val theta = Math.toRadians((180 + animated).toDouble())
        val dot = center + Offset(cos(theta).toFloat() * radius, sin(theta).toFloat() * radius)
        drawCircle(colors.paper, 10.dp.toPx(), dot)
        drawCircle(colors.ink, 6.dp.toPx(), dot)
    }
}

@Composable
fun FoldLogo(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.logo_description)
    Canvas(modifier.clip(RoundedCornerShape(28)).background(BrandCoral).semantics { contentDescription = description }) {
        scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
            val left = Path().apply { moveTo(22f, 25f); lineTo(48f, 35f); lineTo(48f, 76f); lineTo(22f, 65f); close() }
            val right = Path().apply { moveTo(52f, 35f); lineTo(77f, 23f); lineTo(77f, 65f); lineTo(52f, 77f); close() }
            drawPath(left, BrandPaper.copy(alpha = .65f)); drawPath(right, BrandPaper)
            val arrow = Path().apply { moveTo(58f, 43f); lineTo(68f, 50f); lineTo(58f, 60f); close() }
            drawPath(arrow, BrandCoral)
            drawLine(BrandCoral, Offset(71f, 40f), Offset(71f, 59f), 2.5f, StrokeCap.Round)
        }
    }
}

@Composable
private fun FoldIllustration(modifier: Modifier) {
    val description = stringResource(R.string.illustration_description)
    val colors = LocalAppColors.current
    Canvas(modifier.semantics { contentDescription = description }) {
        val s = minOf(size.width / 340f, size.height / 235f)
        val offset = Offset((size.width - 340f * s) / 2, (size.height - 235f * s) / 2)
        drawCircle(colors.paleCoral, size.minDimension * .37f, Offset(size.width * .52f, size.height * .48f))
        scale(s, s, Offset.Zero) {
            // A hand-drawn perspective device. Coordinates are normalized for any panel size.
            val x = offset.x / s
            val y = offset.y / s
            fun polygon(vararg points: Float, color: Color) {
                val path = Path().apply { moveTo(x + points[0], y + points[1]);
                    for (i in 2 until points.size step 2) lineTo(x + points[i], y + points[i + 1]); close() }
                drawPath(path, color)
            }
            polygon(64f, 67f, 162f, 39f, 172f, 171f, 73f, 188f, color = Color(0xFF252A26))
            polygon(168f, 39f, 273f, 70f, 264f, 187f, 178f, 171f, color = Color(0xFF252A26))
            polygon(73f, 74f, 155f, 51f, 164f, 162f, 81f, 177f, color = Color(0xFFEBEEDF))
            polygon(177f, 52f, 263f, 78f, 255f, 176f, 185f, 163f, color = colors.coral)
            drawLine(Color(0xFF72756A), Offset(x + 165, y + 43), Offset(x + 175, y + 172), 2.5f)
            polygon(207f, 94f, 234f, 116f, 207f, 137f, color = BrandPaper)
            drawLine(BrandPaper, Offset(x + 240, y + 98), Offset(x + 240, y + 135), 4f, StrokeCap.Round)
            // Two deliberately excessive speed marks.
            drawLine(colors.coral, Offset(x + 278, y + 43), Offset(x + 289, y + 33), 3f, StrokeCap.Round)
            drawLine(colors.coral, Offset(x + 285, y + 56), Offset(x + 301, y + 53), 3f, StrokeCap.Round)
            val arcOrigin = Offset(x + 110, y + 128)
            drawArc(colors.green, 25f, 110f, false, arcOrigin, Size(120f, 65f), style = Stroke(2f, cap = StrokeCap.Round))
            polygon(119f, 172f, 130f, 175f, 125f, 164f, color = colors.green)
            for (i in 0..2) drawLine(Color(0xFFAAB09D), Offset(x + 94, y + 100 + i * 12), Offset(x + 139, y + 88 + i * 12), 4f, StrokeCap.Round)
        }
    }
}

@Composable
private fun ShopSplash(shop: Shop) {
    val colors = LocalAppColors.current
    Column(Modifier.fillMaxWidth().padding(vertical = 26.dp).testTag("redirect-screen"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(stringResource(R.string.shop_opening, stringResource(shop.titleRes)), color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(22.dp))
        Box(Modifier.size(132.dp).clip(RoundedCornerShape(30.dp)).background(Color(shop.color)), contentAlignment = Alignment.Center) {
            Text(stringResource(shop.titleRes), color = Color.White, fontSize = if (shop == Shop.PDD) 32.sp else 45.sp, fontWeight = FontWeight.Black)
        }
        Text(stringResource(shop.titleRes), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(shop.taglineRes), color = colors.muted, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.simulated_splash_label), color = colors.muted, fontSize = 10.sp,
            modifier = Modifier.border(1.dp, colors.line, CircleShape).padding(horizontal = 12.dp, vertical = 7.dp))
    }
}

@Composable
private fun RedirectDetails(state: Challenge, onReturn: () -> Unit) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Eyebrow(stringResource(R.string.redirect_eyebrow))
        Text(stringResource(R.string.redirect_title, stringResource(state.shop.titleRes)), fontSize = 31.sp, lineHeight = 43.sp, fontWeight = FontWeight.Black)
        Column(Modifier.fillMaxWidth().clip(Shape).background(colors.paleCoral).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(when (state.redirectReason) {
                RedirectReason.TOO_SLOW -> stringResource(R.string.redirect_slow)
                RedirectReason.TOO_DEEP -> stringResource(R.string.redirect_deep)
                RedirectReason.TOO_SHALLOW -> stringResource(R.string.redirect_shallow)
            }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(if (state.redirectReason == RedirectReason.TOO_SLOW)
                stringResource(R.string.redirect_slow_details)
                else stringResource(R.string.redirect_angle_details, state.lastMinimum?.roundToInt() ?: 180),
                fontSize = 13.sp, color = colors.muted, lineHeight = 23.sp)
            Text(stringResource(R.string.redirect_voluntary), fontSize = 12.sp, color = colors.coral)
        }
        Button(onClick = onReturn, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("return-ad"), shape = RoundedCornerShape(14.dp)) {
            Text(stringResource(R.string.return_ad), fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.redirect_footer), color = colors.muted, fontSize = 11.sp)
    }
}

@Composable
private fun HomePoster(state: Challenge, elapsedSeconds: Int) {
    val colors = LocalAppColors.current
    Column(Modifier.testTag("home-screen"), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Eyebrow(if (state.finish == Finish.FOLDED) stringResource(R.string.home_folded_eyebrow) else stringResource(R.string.home_waited_eyebrow), colors.green)
        FoldLogo(Modifier.size(80.dp))
        Text(stringResource(R.string.home_headline), fontSize = 40.sp, lineHeight = 53.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.home_tagline), color = colors.muted, fontSize = 15.sp)
        Row(Modifier.fillMaxWidth().clip(Shape).background(colors.paleGreen).padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat(stringResource(R.string.stat_elapsed), stringResource(R.string.seconds_short, elapsedSeconds))
            Stat(stringResource(R.string.stat_attempts), stringResource(R.string.count_value, state.attempts))
            Stat(stringResource(R.string.stat_redirects), stringResource(R.string.count_value, state.redirects))
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(value, fontSize = 25.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(label, fontSize = 10.sp, color = colors.muted)
    }
}

@Composable
private fun HomeDetails(onRestart: () -> Unit, onAbout: () -> Unit) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(stringResource(R.string.home_welcome), fontSize = 23.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.home_body), color = colors.muted, fontSize = 16.sp, lineHeight = 29.sp)
        HorizontalDivider(color = colors.line)
        Text(stringResource(R.string.home_message), fontSize = 14.sp, lineHeight = 24.sp)
        Button(onClick = onRestart, shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("restart")) {
            Text(stringResource(R.string.restart_ad), fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onAbout, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.home_about_link), color = colors.muted, fontSize = 12.sp) }
        Text(stringResource(R.string.home_signature), color = colors.muted, fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun VideoLink(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val label = stringResource(R.string.video_link_label)
    val url = stringResource(R.string.video_link_url)
    val link = buildAnnotatedString {
        withLink(LinkAnnotation.Url(url, TextLinkStyles(style = SpanStyle(
            color = colors.coral, textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.Medium
        )))) { append(label) }
    }
    Text(link, modifier.padding(vertical = 14.dp), fontSize = 14.sp)
}
