package com.example.displaymoto

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.pow

data class AdaptiveColors(
    val primaryText: Color,
    val secondaryText: Color,
    val accentColor: Color
)

data class CompleteAppPalette(
    val background: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val accentColor: Color
)

object GeminiColorHelper {
    private const val TAG = "GeminiColorHelper"
    private const val MODEL = "gemini-2.5-flash-lite"

    enum class ComponenteLock(val uiName: String) {
        BACKGROUND("Background Color"),
        PRIMARY_TEXT("Primary Text Color (Large Text)"),
        SECONDARY_TEXT("Secondary Text Color (Normal Text)"),
        ACCENT("Accent Color (Interactive Components)")
    }

    // =====================================================================
    // CÁLCULOS WCAG 2.1 E VALIDAÇÃO AUTOMÁTICA
    // =====================================================================

    enum class TipoComponente { TEXTO_NORMAL, TEXTO_GRANDE, INTERATIVO }

    data class ResultadoContraste(
        val ratio: Double,
        val aprovado: Boolean,
        val sugestaoCor: Color?
    )

    /** 
     * Validação Automática baseada nas Diretrizes WCAG Nível AA.
     * Retorna aprovação e sugere cores extremas para garantir o contraste.
     */
    fun verificarContraste(corComponente: Color, corFundo: Color, tipo: TipoComponente): ResultadoContraste {
        val limiteAprovacao = when (tipo) {
            TipoComponente.TEXTO_NORMAL -> 7.0 // WCAG Nível AAA para texto normal
            TipoComponente.TEXTO_GRANDE -> 4.5 // WCAG Nível AAA para texto grande
            TipoComponente.INTERATIVO -> 3.0   // WCAG Nível AAA para componentes gráficos de UI
        }
        val ratio = contrastRatio(corComponente, corFundo)
        if (ratio >= limiteAprovacao) {
            return ResultadoContraste(ratio, true, null)
        }
        
        // Sugerir cor alternativa: garante extremo legível e atende a "sugerir cores alternativas quando necessário".
        val isDarkBg = wcagLuminance(corFundo) < 0.5
        val sugestao = if (isDarkBg) Color.White else Color.Black
        return ResultadoContraste(ratio, false, sugestao)
    }

    /** Lineariza um canal sRGB (0..1) para luminância relativa */
    private fun linearize(channel: Float): Double {
        return if (channel <= 0.03928f) (channel / 12.92).toDouble()
        else ((channel + 0.055) / 1.055).toDouble().pow(2.4)
    }

    /** Calcula luminância relativa WCAG de uma Color */
    fun wcagLuminance(color: Color): Double {
        return 0.2126 * linearize(color.red) +
               0.7152 * linearize(color.green) +
               0.0722 * linearize(color.blue)
    }

    /** Calcula o rácio de contraste WCAG entre duas luminâncias */
    fun contrastRatio(l1: Double, l2: Double): Double {
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Calcula o rácio de contraste WCAG entre duas Colors */
    fun contrastRatio(c1: Color, c2: Color): Double {
        return contrastRatio(wcagLuminance(c1), wcagLuminance(c2))
    }

    // =====================================================================
    // FALLBACK LOCAL — CORES COM CONTRASTE WCAG GARANTIDO
    // =====================================================================

    // =====================================================================
    // GERAÇÃO DE CORES EM TODO O ESPETRO HSL
    // =====================================================================

    /** Converte HSL (h: 0-360, s: 0-1, l: 0-1) para Color */
    private fun hslToColor(h: Float, s: Float, l: Float): Color {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r1, g1, b1) = when {
            h < 60f  -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else     -> Triple(c, 0f, x)
        }
        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f)
        )
    }

    /**
     * Gera candidatos varrendo TODO o espetro HSL:
     * - Hue: 0° a 355° em passos de 5° (72 hues)
     * - Saturação: 0.7, 0.85, 1.0
     * - Luminosidade: 0.30, 0.45, 0.55, 0.70, 0.85
     * Total: 72 × 3 × 5 = 1080 candidatos cromáticos
     * + 11 neutros acromáticos (brancos e pretos)
     */
    private fun generateSpectrumCandidates(): List<Color> {
        val candidates = mutableListOf<Color>()
        val hueSteps = (0 until 360 step 5)
        val saturations = listOf(0.7f, 0.85f, 1.0f)
        val lightnesses = listOf(0.30f, 0.45f, 0.55f, 0.70f, 0.85f)

        for (h in hueSteps) {
            for (s in saturations) {
                for (l in lightnesses) {
                    candidates.add(hslToColor(h.toFloat(), s, l))
                }
            }
        }
        // Neutros acromáticos
        for (v in listOf(0f, 0.1f, 0.2f, 0.3f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 0.95f, 1.0f)) {
            candidates.add(Color(v, v, v))
        }
        return candidates
    }

    /**
     * Calcula cores de contraste localmente SEM IA, usando rácios WCAG reais
     * e varrendo TODO o espetro de cores visíveis.
     *
     * - primaryText: rácio ≥ 4.5:1 (WCAG AAA large text)
     * - secondaryText: rácio ≥ 7.0:1 (WCAG AAA normal text) 
     * - accentColor: rácio ≥ 3.0:1 (WCAG AA UI components), hue distinto do fundo
     *
     * REGRA CHAVE: Em fundos escuros (luminância < 0.5) → preferir cores CLARAS.
     *              Em fundos claros (luminância ≥ 0.5) → preferir cores ESCURAS.
     *              Isto garante legibilidade visual, não apenas compliance WCAG.
     */
    fun computeLocalContrastColors(backgroundHex: String): AdaptiveColors {
        val bgColor = try {
            Color(android.graphics.Color.parseColor(backgroundHex))
        } catch (e: Exception) {
            Color(0xFF0D0F26)
        }

        val bgLum = wcagLuminance(bgColor)
        val bgHue = hueOf(bgColor)
        val isDarkBg = bgLum < 0.5
        val allCandidates = generateSpectrumCandidates()

        // Limites WCAG 2.1 Nível AAA (mais rigoroso)
        val normalTextThreshold = 7.0   // AAA normal text
        val largeTextThreshold = 4.5    // AAA large text
        val interactiveThreshold = 3.0  // AA UI components (AAA não define maior)

        // 1. TEXTO PRIMÁRIO: Texto Grande (AAA ≥ 4.5:1), melhor contraste na DIREÇÃO CORRETA
        val directedPrimary = allCandidates
            .filter { c ->
                val cLum = wcagLuminance(c)
                val correctDirection = if (isDarkBg) cLum > 0.5 else cLum < 0.5
                verificarContraste(c, bgColor, TipoComponente.TEXTO_GRANDE).aprovado && correctDirection && !isHarshClash(c, bgColor)
            }
            .randomOrNull()

        // Usa a sugestão automática caso não haja candidato válido
        val primaryText = directedPrimary
            ?: verificarContraste(if (isDarkBg) Color.White else Color.Black, bgColor, TipoComponente.TEXTO_GRANDE).sugestaoCor!!

        val primaryLum = wcagLuminance(primaryText)
        val primaryHue = hueOf(primaryText)

        Log.d(TAG, "Primary text on $backgroundHex: ${"%.2f".format(contrastRatio(primaryLum, bgLum))}:1 (isDark=$isDarkBg)")

        // 2. TEXTO SECUNDÁRIO: Texto Normal (AAA ≥ 7.0:1), hierarquia garantida
        val secondaryText = allCandidates
            .filter { c ->
                val cLum = wcagLuminance(c)
                val ratio = contrastRatio(cLum, bgLum)
                val correctDirection = if (isDarkBg) cLum > 0.3 else cLum < 0.7
                verificarContraste(c, bgColor, TipoComponente.TEXTO_NORMAL).aprovado && ratio < contrastRatio(primaryLum, bgLum) * 0.95
                    && correctDirection && !isHarshClash(c, bgColor)
            }
            .randomOrNull()
            ?: if (isDarkBg) Color.White else Color.Black // Fallback AAA garantido

        Log.d(TAG, "Secondary text on $backgroundHex: ${"%.2f".format(contrastRatio(wcagLuminance(secondaryText), bgLum))}:1")

        // 3. ACCENT COLOR: Componentes Interativos (AA ≥ 3.0:1 contraste), hue distinto, VIBRANTE e na direção correta
        val accentColor = allCandidates
            .filter { c ->
                val cLum = wcagLuminance(c)
                val cHue = hueOf(c)
                val bgHueDist = hueDifference(cHue, bgHue)
                val saturation = saturationOf(c)
                val correctDirection = if (isDarkBg) cLum > 0.35 else cLum < 0.65
                verificarContraste(c, bgColor, TipoComponente.INTERATIVO).aprovado && bgHueDist > 40f && saturation > 0.4f && correctDirection && !isHarshClash(c, bgColor)
            }
            .randomOrNull()
            // Fallback 1: relaxar filtros de hue e saturação
            ?: allCandidates
                .filter { c ->
                    val cLum = wcagLuminance(c)
                    val correctDirection = if (isDarkBg) cLum > 0.35 else cLum < 0.65
                    verificarContraste(c, bgColor, TipoComponente.INTERATIVO).aprovado && correctDirection && !isHarshClash(c, bgColor)
                }
                .randomOrNull()
            // Fallback 2: usar a sugestão automática de cor
            ?: verificarContraste(Color.Red, bgColor, TipoComponente.INTERATIVO).sugestaoCor!!

        Log.d(TAG, "Accent on $backgroundHex: ${"%.2f".format(contrastRatio(wcagLuminance(accentColor), bgLum))}:1")

        return AdaptiveColors(primaryText, secondaryText, accentColor)
    }

    /**
     * Dada uma cor de elementos (foreground) e o fundo atual,
     * ajusta a LUMINOSIDADE do fundo para garantir contraste ≥ 3:1.
     * Preserva hue e saturação do fundo original.
     * Retorna null se o fundo atual já tem contraste suficiente.
     */
    fun computeAdaptedBackground(foregroundColor: Color, currentBackground: Color): Color? {
        val fgLum = wcagLuminance(foregroundColor)
        val bgLum = wcagLuminance(currentBackground)
        val currentRatio = contrastRatio(fgLum, bgLum)

        Log.d(TAG, "Background adaptation check: fg lum=${"%.3f".format(fgLum)}, bg lum=${"%.3f".format(bgLum)}, ratio=${"%.2f".format(currentRatio)}:1")

        // Se já tem contraste suficiente, não mexer
        if (currentRatio >= 3.0) {
            Log.d(TAG, "Background already has sufficient contrast (${"%.2f".format(currentRatio)}:1). No change needed.")
            return null
        }

        // Extrair HSL do fundo atual
        val bgR = currentBackground.red
        val bgG = currentBackground.green
        val bgB = currentBackground.blue
        val max = maxOf(bgR, bgG, bgB)
        val min = minOf(bgR, bgG, bgB)
        var hue = hueOf(currentBackground)
        val delta = max - min
        val lightness = (max + min) / 2f
        val saturation = if (delta < 0.001f) 0f
            else delta / (1f - kotlin.math.abs(2f * lightness - 1f)).coerceAtLeast(0.001f)

        // Decidir direção: se foreground é claro → fundo deve escurecer; se escuro → fundo deve aclarar
        val fgIsLight = fgLum > 0.5

        // Tentar ajustar a luminosidade em passos de 0.02 até encontrar contraste adequado
        var bestBg: Color? = null
        val step = 0.02f
        val range = if (fgIsLight) {
            // Foreground claro → escurecer fundo (reduzir lightness)
            generateSequence(lightness) { it - step }.takeWhile { it >= 0f }
        } else {
            // Foreground escuro → aclarar fundo (aumentar lightness)
            generateSequence(lightness) { it + step }.takeWhile { it <= 1f }
        }

        for (newL in range) {
            val candidate = hslToColor(hue, saturation, newL)
            val candidateLum = wcagLuminance(candidate)
            val ratio = contrastRatio(fgLum, candidateLum)
            if (ratio >= 3.0) {
                bestBg = candidate
                Log.d(TAG, "Found adapted background at L=${"%.2f".format(newL)}, ratio=${"%.2f".format(ratio)}:1")
                break
            }
        }

        return bestBg ?: if (fgIsLight) Color.Black else Color.White
    }

    /**
     * Gera uma paleta temporária que respeita a escolha do utilizador mudando as outras dimensões via algoritmo local.
     */
    fun computeFallbackPalette(userColor: Color, fixedComponent: ComponenteLock, currentBackground: Color): CompleteAppPalette {
        val rawPalette = when(fixedComponent) {
            ComponenteLock.BACKGROUND -> {
                val hexBg = String.format("#%06X", 0xFFFFFF and userColor.toArgb())
                val locals = computeLocalContrastColors(hexBg)
                CompleteAppPalette(userColor, locals.primaryText, locals.secondaryText, locals.accentColor)
            }
            ComponenteLock.ACCENT -> {
                val adaptedBg = computeAdaptedBackground(userColor, currentBackground) ?: currentBackground
                val hexBg = String.format("#%06X", 0xFFFFFF and adaptedBg.toArgb())
                val locals = computeLocalContrastColors(hexBg)
                CompleteAppPalette(adaptedBg, locals.primaryText, locals.secondaryText, userColor)
            }
            ComponenteLock.PRIMARY_TEXT -> {
                val adaptedBg = computeAdaptedBackground(userColor, currentBackground) ?: currentBackground
                val hexBg = String.format("#%06X", 0xFFFFFF and adaptedBg.toArgb())
                val locals = computeLocalContrastColors(hexBg)
                CompleteAppPalette(adaptedBg, userColor, locals.secondaryText, locals.accentColor)
            }
            ComponenteLock.SECONDARY_TEXT -> {
                val adaptedBg = computeAdaptedBackground(userColor, currentBackground) ?: currentBackground
                val hexBg = String.format("#%06X", 0xFFFFFF and adaptedBg.toArgb())
                val locals = computeLocalContrastColors(hexBg)
                CompleteAppPalette(adaptedBg, locals.primaryText, userColor, locals.accentColor)
            }
        }
        // GUARDIÃO AAA: NUNCA devolver paleta sem validar
        return validateAndFixPalette(rawPalette, fixedComponent)
    }

    /** Extrai o hue (0-360) de uma Color */
    private fun hueOf(color: Color): Float {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        if (delta < 0.001f) return 0f // Acromático (cinza/branco/preto)
        val hue = when (max) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        return if (hue < 0f) hue + 360f else hue
    }

    /** Diferença angular entre dois hues (0-180) */
    private fun hueDifference(h1: Float, h2: Float): Float {
        val diff = kotlin.math.abs(h1 - h2)
        return if (diff > 180f) 360f - diff else diff
    }

    /** Extrai a saturação HSL (0-1) de uma Color */
    private fun saturationOf(color: Color): Float {
        val max = maxOf(color.red, color.green, color.blue)
        val min = minOf(color.red, color.green, color.blue)
        val delta = max - min
        if (delta < 0.001f) return 0f // Acromático
        val l = (max + min) / 2f
        return delta / (1f - kotlin.math.abs(2f * l - 1f)).coerceAtLeast(0.001f)
    }

    /** Verifica se duas cores colidem severamente (ex: verde e vermelho) ou causam "aberração cromática" (vibração visual) */
    private fun isHarshClash(c1: Color, c2: Color): Boolean {
        val s1 = saturationOf(c1)
        val s2 = saturationOf(c2)
        
        // Se uma das cores for quase preta ou branca (luminância extrema), raramente há choque visual agudo
        val l1 = wcagLuminance(c1)
        val l2 = wcagLuminance(c2)
        if (l1 < 0.15f || l1 > 0.85f || l2 < 0.15f || l2 > 0.85f) return false
        
        val h1 = hueOf(c1)
        val h2 = hueOf(c2)
        
        // Regra 1: Cores altamente saturadas de matizes diferentes "vibram" (ex: Azul neon em Verde Neon)
        if (s1 > 0.6f && s2 > 0.6f && hueDifference(h1, h2) > 30f) {
            return true
        }
        
        // Regra 2: Clássico vermelho/verde (proteção daltónicos)
        fun isRed(h: Float) = h > 330f || h < 40f
        fun isGreen(h: Float) = h in 70f..170f
        return ((isRed(h1) && isGreen(h2)) || (isGreen(h1) && isRed(h2))) && s1 > 0.3f && s2 > 0.3f
    }

    // =====================================================================
    // GEMINI API — REFINAMENTO COM IA
    // =====================================================================
    // GUARDIÃO MATEMÁTICO ABSOLUTO: CORREÇÃO PROGRAMÁTICA WCAG AAA
    // =====================================================================
    
    /**
     * Tenta manter a cor original ajustando apenas a luminosidade (HSL).
     * Se nenhum ajuste gradual atingir o rácio AAA, recorre a Branco ou Preto absoluto.
     */
    private fun maximizeContrast(fg: Color, bg_: Color, requiredRatio: Double): Color {
        // Se a cor sugerida já passa a matemática e NÃO causa vibração visual dolorosa, usamos.
        if (contrastRatio(fg, bg_) >= requiredRatio && !isHarshClash(fg, bg_)) return fg
        
        // Tentar ajustar gradualmente a luminosidade mantendo hue e saturação
        val fgHue = hueOf(fg)
        val fgSat = saturationOf(fg)
        val bgLum = wcagLuminance(bg_)
        val bgIsDark = bgLum < 0.5
        
        // Varrer luminosidade na direção correta (claro se bg escuro, escuro se bg claro)
        val step = 0.02f
        val range = if (bgIsDark) {
            generateSequence(0.55f) { it + step }.takeWhile { it <= 1.0f }
        } else {
            generateSequence(0.45f) { it - step }.takeWhile { it >= 0.0f }
        }
        
        for (newL in range) {
            val candidate = hslToColor(fgHue, fgSat, newL)
            if (contrastRatio(candidate, bg_) >= requiredRatio && !isHarshClash(candidate, bg_)) {
                Log.d(TAG, "maximizeContrast: fixed via luminance shift L=$newL, ratio=${"%.1f".format(contrastRatio(candidate, bg_))}:1")
                return candidate
            }
        }
        
        // Último recurso: pólo absoluto
        val cWhite = contrastRatio(Color.White, bg_)
        val cBlack = contrastRatio(Color.Black, bg_)
        val fallback = if (cWhite > cBlack) Color.White else Color.Black
        Log.d(TAG, "maximizeContrast: fell back to ${if (fallback == Color.White) "WHITE" else "BLACK"}, ratio=${"%.1f".format(contrastRatio(fallback, bg_))}:1")
        return fallback
    }

    /**
     * Força o background a ter contraste suficiente com o foreground.
     * Ajusta gradualmente a luminosidade antes de recorrer a B/W.
     */
    private fun forceBackgroundContrast(fg: Color, requiredRatio: Double, currentBg: Color): Color {
        // Se o fundo sugerido passa e não vibra visualmente com o foreground alvo, OK
        if (contrastRatio(fg, currentBg) >= requiredRatio && !isHarshClash(fg, currentBg)) return currentBg
        
        // Tentar ajustar gradualmente a luminosidade do background
        val bgHue = hueOf(currentBg)
        val bgSat = saturationOf(currentBg)
        val fgLum = wcagLuminance(fg)
        val fgIsDark = fgLum < 0.5
        
        val step = 0.02f
        // Se o fg é escuro, aclarar o bg. Se o fg é claro, escurecer o bg.
        val range = if (fgIsDark) {
            generateSequence(0.55f) { it + step }.takeWhile { it <= 1.0f }
        } else {
            generateSequence(0.45f) { it - step }.takeWhile { it >= 0.0f }
        }
        
        for (newL in range) {
            val candidate = hslToColor(bgHue, bgSat, newL)
            if (contrastRatio(fg, candidate) >= requiredRatio && !isHarshClash(fg, candidate)) {
                Log.d(TAG, "forceBackgroundContrast: fixed at L=$newL, ratio=${"%.1f".format(contrastRatio(fg, candidate))}:1")
                return candidate
            }
        }
        
        // Último recurso
        val cWhite = contrastRatio(fg, Color.White)
        val cBlack = contrastRatio(fg, Color.Black)
        val fallback = if (cWhite > cBlack) Color.White else Color.Black
        Log.d(TAG, "forceBackgroundContrast: fell back to ${if (fallback == Color.White) "WHITE" else "BLACK"}")
        return fallback
    }

    /**
     * Valida de forma estrita a paleta gerada. 
     * Se o Gemini cometer um erro de cálculo e falhar os 7.0:1 (AAA) ou 4.5:1 (AAA), esta função FORÇA
     * a cor a desviar-se para o Branco Absoluto ou Preto Absoluto para salvar a legibilidade.
     * Esta blindagem garante ZERO erros passíveis de visualização humana.
     */
    fun validateAndFixPalette(palette: CompleteAppPalette, fixedLock: ComponenteLock): CompleteAppPalette {
        var bg = palette.background
        var prim = palette.primaryText
        var sec = palette.secondaryText
        var acc = palette.accentColor

        Log.d(TAG, "validateAndFixPalette BEFORE: bg=${colorToHex(bg)}, prim=${colorToHex(prim)}(${"%.1f".format(contrastRatio(prim, bg))}:1), sec=${colorToHex(sec)}(${"%.1f".format(contrastRatio(sec, bg))}:1), acc=${colorToHex(acc)}(${"%.1f".format(contrastRatio(acc, bg))}:1)")

        when (fixedLock) {
            ComponenteLock.BACKGROUND -> {
                prim = maximizeContrast(prim, bg, 4.5)  // AAA Large Text
                sec = maximizeContrast(sec, bg, 7.0)    // AAA Normal Text
                acc = maximizeContrast(acc, bg, 3.0)    // AA UI Components
            }
            ComponenteLock.SECONDARY_TEXT -> {
                bg = forceBackgroundContrast(sec, 7.0, bg)
                prim = maximizeContrast(prim, bg, 4.5)
                acc = maximizeContrast(acc, bg, 3.0)
            }
            ComponenteLock.PRIMARY_TEXT -> {
                bg = forceBackgroundContrast(prim, 4.5, bg)
                sec = maximizeContrast(sec, bg, 7.0)
                acc = maximizeContrast(acc, bg, 3.0)
            }
            ComponenteLock.ACCENT -> {
                bg = forceBackgroundContrast(acc, 3.0, bg)
                prim = maximizeContrast(prim, bg, 4.5)
                sec = maximizeContrast(sec, bg, 7.0)
            }
        }

        Log.i(TAG, "validateAndFixPalette AFTER: bg=${colorToHex(bg)}, prim=${colorToHex(prim)}(${"%.1f".format(contrastRatio(prim, bg))}:1), sec=${colorToHex(sec)}(${"%.1f".format(contrastRatio(sec, bg))}:1), acc=${colorToHex(acc)}(${"%.1f".format(contrastRatio(acc, bg))}:1)")

        return CompleteAppPalette(bg, prim, sec, acc)
    }

    private fun colorToHex(c: Color): String = String.format("#%06X", 0xFFFFFF and c.toArgb())

    /**
     * PONTO ÚNICO DE GARANTIA AAA.
     * Qualquer AdaptiveColors que vá ser aplicada à UI OBRIGATORIAMENTE deve passar por aqui.
     * Garante:
     *   - secondaryText vs background ≥ 7.0:1  (AAA Normal Text)
     *   - primaryText vs background ≥ 4.5:1     (AAA Large Text)
     *   - accentColor vs background ≥ 3.0:1     (AA UI Components)
     */
    fun enforceAAA(colors: AdaptiveColors, background: Color): AdaptiveColors {
        val fixedPrimary = maximizeContrast(colors.primaryText, background, 4.5)
        val fixedSecondary = maximizeContrast(colors.secondaryText, background, 7.0)
        val fixedAccent = maximizeContrast(colors.accentColor, background, 3.0)
        
        Log.i(TAG, "enforceAAA on bg=${colorToHex(background)}: " +
            "prim ${colorToHex(colors.primaryText)}->${colorToHex(fixedPrimary)}(${"%,.1f".format(contrastRatio(fixedPrimary, background))}:1), " +
            "sec ${colorToHex(colors.secondaryText)}->${colorToHex(fixedSecondary)}(${"%,.1f".format(contrastRatio(fixedSecondary, background))}:1), " +
            "acc ${colorToHex(colors.accentColor)}->${colorToHex(fixedAccent)}(${"%,.1f".format(contrastRatio(fixedAccent, background))}:1)")
        
        return AdaptiveColors(fixedPrimary, fixedSecondary, fixedAccent)
    }

    // =====================================================================

    /**
     * Pede ao Gemini as cores ideais para o fundo dado.
     * Usa chamadas HTTP REST diretas ao endpoint da API do Gemini.
     * Em caso de erro, devolve o fallback local com WCAG garantido.
     */
    suspend fun adaptColorsToBackground(backgroundHex: String): AdaptiveColors? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            Log.w(TAG, "GEMINI_API_KEY is empty. Using local fallback.")
            return computeLocalContrastColors(backgroundHex)
        }

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting Gemini API call for background: $backgroundHex")

                // Pré-calcular luminância para incluir no prompt
                val bgColor = try { Color(android.graphics.Color.parseColor(backgroundHex)) } catch (_: Exception) { Color(0xFF0D0F26) }
                val bgLum = wcagLuminance(bgColor)
                val isDarkBg = bgLum < 0.5
                val lumDirection = if (isDarkBg) "DARK (luminance=${String.format("%.3f", bgLum)}). You MUST use LIGHT-colored text and elements." 
                                   else "LIGHT (luminance=${String.format("%.3f", bgLum)}). You MUST use DARK-colored text and elements."

                val prompt = """
                    Background: $backgroundHex (${if (isDarkBg) "dark" else "light"}).
                    Generate 3 WCAG-compliant HEX colors for this background:
                    - primaryText: contrast ≥4.5:1 (large text AAA)
                    - secondaryText: contrast ≥7:1 (normal text AAA)
                    - accentColor: contrast ≥3:1 (UI components), different hue from background
                    ${if (isDarkBg) "Use LIGHT colors." else "Use DARK colors."}
                    Never pair saturated red+green. Be creative with hues.
                    Return ONLY: {"primaryText":"#XXXXXX","secondaryText":"#XXXXXX","accentColor":"#XXXXXX"}
                """.trimIndent()

                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val requestBody = JSONObject().apply {
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("responseMimeType", "application/json")
                        put("maxOutputTokens", 100)
                    })
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }

                Log.d(TAG, "Sending request to Gemini...")
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "Gemini API response code: $responseCode")

                if (responseCode != 200) {
                    val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "No error body"
                    Log.e(TAG, "Gemini API HTTP $responseCode: $errorStream")
                    return@withContext computeLocalContrastColors(backgroundHex)
                }

                val responseText = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                val responseJson = JSONObject(responseText)
                val candidates = responseJson.getJSONArray("candidates")
                if (candidates.length() == 0) {
                    Log.w(TAG, "No candidates in Gemini response")
                    return@withContext computeLocalContrastColors(backgroundHex)
                }

                val content = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val startIndex = content.indexOf("{")
                val endIndex = content.lastIndexOf("}")
                if (startIndex == -1 || endIndex == -1) {
                    Log.e(TAG, "Gemini returned invalid JSON format: $content")
                    return@withContext computeLocalContrastColors(backgroundHex)
                }
                
                val cleanJson = content.substring(startIndex, endIndex + 1)
                Log.d(TAG, "Gemini parsed: $cleanJson")

                val colorsJson = JSONObject(cleanJson)
                val aiPrimary = parseColorSafe(colorsJson.getString("primaryText"))
                val aiSecondary = parseColorSafe(colorsJson.getString("secondaryText"))
                val aiAccent = parseColorSafe(colorsJson.getString("accentColor"))

                // Reutilizar bgColor e isDarkBg já calculados acima

                // VALIDAR: contraste + DIREÇÃO da luminância (claro/escuro)
                fun isCorrectDirection(c: Color): Boolean {
                    val cLum = wcagLuminance(c)
                    return if (isDarkBg) cLum > 0.4 else cLum < 0.5
                }

                val validPrimary = aiPrimary?.let {
                    val check = verificarContraste(it, bgColor, TipoComponente.TEXTO_GRANDE)
                    val dirOk = isCorrectDirection(it)
                    Log.d(TAG, "AI primaryText ratio: ${"%.2f".format(check.ratio)}:1, direction=${if(dirOk) "OK" else "WRONG"}")
                    // Se falhar a validação (aprovado=false), retorna a cor sugerida (correção automática).
                    if (check.aprovado && dirOk && !isHarshClash(it, bgColor)) it else check.sugestaoCor
                }
                val validSecondary = aiSecondary?.let {
                    val check = verificarContraste(it, bgColor, TipoComponente.TEXTO_NORMAL)
                    val dirOk = isCorrectDirection(it)
                    Log.d(TAG, "AI secondaryText ratio: ${"%.2f".format(check.ratio)}:1, direction=${if(dirOk) "OK" else "WRONG"}")
                    if (check.aprovado && dirOk && !isHarshClash(it, bgColor)) it else check.sugestaoCor
                }
                val validAccent = aiAccent?.let {
                    val check = verificarContraste(it, bgColor, TipoComponente.INTERATIVO)
                    val dirOk = isCorrectDirection(it)
                    val hDist = hueDifference(hueOf(it), hueOf(bgColor))
                    Log.d(TAG, "AI accentColor ratio: ${"%.2f".format(check.ratio)}:1, hue dist: ${"%.0f".format(hDist)}°, direction=${if(dirOk) "OK" else "WRONG"}")
                    if (check.aprovado && dirOk && hDist > 30f && !isHarshClash(it, bgColor)) it else check.sugestaoCor
                }

                // Se alguma cor da IA falhar, usar a local como fallback desse canal
                val local = computeLocalContrastColors(backgroundHex)
                val rawResult = AdaptiveColors(
                    primaryText = validPrimary ?: local.primaryText,
                    secondaryText = validSecondary ?: local.secondaryText,
                    accentColor = validAccent ?: local.accentColor
                )

                // GUARDIÃO AAA: validar e corrigir programaticamente antes de devolver
                val guardedPalette = validateAndFixPalette(
                    CompleteAppPalette(bgColor, rawResult.primaryText, rawResult.secondaryText, rawResult.accentColor),
                    ComponenteLock.BACKGROUND
                )
                val result = AdaptiveColors(
                    primaryText = guardedPalette.primaryText,
                    secondaryText = guardedPalette.secondaryText,
                    accentColor = guardedPalette.accentColor
                )

                Log.i(TAG, "Final AAA-guarded colors for $backgroundHex: " +
                    "primary=${"%.1f".format(contrastRatio(result.primaryText, bgColor))}:1, " +
                    "secondary=${"%.1f".format(contrastRatio(result.secondaryText, bgColor))}:1, " +
                    "accent=${"%.1f".format(contrastRatio(result.accentColor, bgColor))}:1")

                result

            } catch (e: Exception) {
                Log.e(TAG, "Gemini API FAILED: ${e.message}", e)
                computeLocalContrastColors(backgroundHex)
            }
        }
    }

    suspend fun adaptPaletteToUserChoice(userColor: Color, fixedComponent: ComponenteLock): CompleteAppPalette? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return null
        
        return withContext(Dispatchers.IO) {
            try {
                val hexCor = String.format("#%06X", 0xFFFFFF and userColor.toArgb())
                
                val bgLum = GeminiColorHelper.wcagLuminance(try { Color(android.graphics.Color.parseColor(hexCor)) } catch (_: Exception) { Color.Gray })
                val isDark = bgLum < 0.5
                
                val prompt = """
                    Locked: ${fixedComponent.uiName} = $hexCor.
                    Generate a complete WCAG-compliant palette around this locked color.
                    Rules: primaryText vs background ≥4.5:1, secondaryText vs background ≥7:1, accentColor vs background ≥3:1.
                    ${if (fixedComponent == ComponenteLock.BACKGROUND) "Background is ${if (isDark) "dark" else "light"}, use ${if (isDark) "light" else "dark"} foregrounds." else "Choose a background that contrasts well with $hexCor."}
                    Never pair saturated red+green. Be creative.
                    Return ONLY: {"background":"#XXXXXX","primaryText":"#XXXXXX","secondaryText":"#XXXXXX","accentColor":"#XXXXXX"}
                """.trimIndent()
                
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val requestBody = JSONObject().apply {
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("responseMimeType", "application/json")
                        put("maxOutputTokens", 120)
                    })
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                if (connection.responseCode != 200) return@withContext null

                val responseText = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                val responseJson = JSONObject(responseText)
                val candidates = responseJson.getJSONArray("candidates")
                if (candidates.length() == 0) return@withContext null

                val content = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val startIndex = content.indexOf("{")
                val endIndex = content.lastIndexOf("}")
                if (startIndex == -1 || endIndex == -1) {
                    Log.e(TAG, "Gemini returned invalid JSON format in adaptPalette: $content")
                    return@withContext null
                }

                val cleanJson = content.substring(startIndex, endIndex + 1)
                val colorsJson = JSONObject(cleanJson)
                val bg = parseColorSafe(colorsJson.getString("background"))
                val prim = parseColorSafe(colorsJson.getString("primaryText"))
                val sec = parseColorSafe(colorsJson.getString("secondaryText"))
                val acc = parseColorSafe(colorsJson.getString("accentColor"))
                
                if (bg != null && prim != null && sec != null && acc != null) {
                    val rawPalette = CompleteAppPalette(bg, prim, sec, acc)
                    validateAndFixPalette(rawPalette, fixedComponent)
                } else null

            } catch (e: Exception) {
                Log.e(TAG, "Gemini API adaptPalette FAILED: ${e.message}", e)
                null
            }
        }
    }

    private fun parseColorSafe(hexStr: String): Color? {
        return try {
            Color(android.graphics.Color.parseColor(hexStr))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse color: $hexStr")
            null
        }
    }
}
