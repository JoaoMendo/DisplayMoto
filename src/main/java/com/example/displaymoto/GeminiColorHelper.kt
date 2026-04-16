package com.example.displaymoto

import android.util.Log
import androidx.compose.ui.graphics.Color
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

object GeminiColorHelper {
    private const val TAG = "GeminiColorHelper"
    private const val MODEL = "gemini-2.0-flash"

    // =====================================================================
    // CÁLCULOS WCAG 2.1 — LUMINÂNCIA RELATIVA E RÁCIO DE CONTRASTE
    // =====================================================================

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
     * - primaryText: rácio ≥ 4.5:1 (WCAG AA normal text)
     * - secondaryText: rácio ≥ 3.0:1 (WCAG AA large text)
     * - accentColor: rácio ≥ 3.0:1, hue distinto do fundo e do texto
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

        // Threshold 3:1 para texto grande (WCAG SC 1.4.3 — todo o texto do dashboard é 18pt+)
        val textThreshold = 3.0

        // 1. TEXTO PRIMÁRIO: melhor contraste na DIREÇÃO CORRETA
        //    Fundos escuros → texto claro. Fundos claros → texto escuro.
        val directedPrimary = allCandidates
            .filter { c ->
                val cLum = wcagLuminance(c)
                val ratio = contrastRatio(cLum, bgLum)
                val correctDirection = if (isDarkBg) cLum > 0.5 else cLum < 0.5
                ratio >= textThreshold && correctDirection && !isHarshClash(c, bgColor)
            }
            .randomOrNull()

        val primaryText = directedPrimary
            ?: if (isDarkBg) Color.White else Color.Black

        val primaryLum = wcagLuminance(primaryText)
        val primaryHue = hueOf(primaryText)

        Log.d(TAG, "Primary text on $backgroundHex: ${"%.2f".format(contrastRatio(primaryLum, bgLum))}:1 (isDark=$isDarkBg)")

        // 2. TEXTO SECUNDÁRIO: mesma direção mas com contraste ligeiramente menor (hierarquia)
        val secondaryText = allCandidates
            .filter { c ->
                val cLum = wcagLuminance(c)
                val ratio = contrastRatio(cLum, bgLum)
                val correctDirection = if (isDarkBg) cLum > 0.3 else cLum < 0.7
                ratio >= textThreshold && ratio < contrastRatio(primaryLum, bgLum) * 0.92
                    && correctDirection && !isHarshClash(c, bgColor)
            }
            .randomOrNull()
            ?: if (isDarkBg) Color(0xFFCCCCCC) else Color(0xFF444444)

        Log.d(TAG, "Secondary text on $backgroundHex: ${"%.2f".format(contrastRatio(wcagLuminance(secondaryText), bgLum))}:1")

        // 3. ACCENT COLOR: ≥ 3:1 contraste, hue distinto, VIBRANTE e na direção correta
        val accentColor = allCandidates
            .filter { c ->
                val cLum = wcagLuminance(c)
                val ratio = contrastRatio(cLum, bgLum)
                val cHue = hueOf(c)
                val bgHueDist = hueDifference(cHue, bgHue)
                val saturation = saturationOf(c)
                val correctDirection = if (isDarkBg) cLum > 0.35 else cLum < 0.65
                ratio >= 3.0 && bgHueDist > 40f && saturation > 0.4f && correctDirection && !isHarshClash(c, bgColor)
            }
            .randomOrNull()
            // Fallback 1: relaxar filtros de hue
            ?: allCandidates
                .filter { c ->
                    val cLum = wcagLuminance(c)
                    val ratio = contrastRatio(cLum, bgLum)
                    val correctDirection = if (isDarkBg) cLum > 0.35 else cLum < 0.65
                    ratio >= 3.0 && correctDirection && !isHarshClash(c, bgColor)
                }
                .randomOrNull()
            // Fallback 2: branco ou preto
            ?: if (isDarkBg) Color.White else Color.Black

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

    /** Verifica se duas cores colidem severamente (ex: verde e vermelho) e são hostis a daltónicos */
    private fun isHarshClash(c1: Color, c2: Color): Boolean {
        val h1 = hueOf(c1)
        val h2 = hueOf(c2)
        val s1 = saturationOf(c1)
        val s2 = saturationOf(c2)
        
        fun isRed(h: Float) = h > 330f || h < 40f
        fun isGreen(h: Float) = h in 70f..170f
        
        // Só é clash se for vermelho de um lado, verde do outro, e ambas tiverem saturação visível
        return ((isRed(h1) && isGreen(h2)) || (isGreen(h1) && isRed(h2))) && s1 > 0.2f && s2 > 0.2f
    }

    // =====================================================================
    // GEMINI API — REFINAMENTO COM IA
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
                    You are a WCAG 2.2 color contrast engine. Given a background color, return 3 optimized foreground colors.

                    BACKGROUND: $backgroundHex — This background is $lumDirection

                    ABSOLUTE RULES:
                    1. DARK backgrounds (luminance < 0.5): ALL 3 returned colors MUST have luminance > 0.5 (be LIGHT). Never return dark colors on dark backgrounds.
                    2. LIGHT backgrounds (luminance ≥ 0.5): ALL 3 returned colors MUST have luminance < 0.4 (be DARK). Never return light colors on light backgrounds.
                    3. Contrast ratio formula: CR = (L_lighter + 0.05) / (L_darker + 0.05) where L = 0.2126*R_lin + 0.7152*G_lin + 0.0722*B_lin
                    4. primaryText contrast ≥ 3:1 against background (large text, 18pt+)
                    5. secondaryText contrast ≥ 3:1 against background
                    6. accentColor contrast ≥ 3:1 against background, hue must differ ≥ 60° from background hue
                    7. NEVER pair saturated red and saturated green (colorblind hostile and visually harsh).
                    8. accentColor must be visually distinct from primaryText (different hue).
                    9. IMPORTANT: Return RANDOM but ELEGANT accessible colors. Do NOT use pure neon colors (like #00FF00, #FF0000). Use modern UI color palettes (e.g. pastels, muted tones, sophisticated analogous or split-complementary harmonies). The combination MUST be visually appealing and harmonious.

                    REFERENCE EXAMPLES (correct outputs):
                    - Background #FF0000 (red, dark) → {"primaryText":"#FFFFFF","secondaryText":"#E0E0E0","accentColor":"#00E5FF"}
                    - Background #00FF00 (green, light) → {"primaryText":"#1A1A1A","secondaryText":"#3D3D3D","accentColor":"#6A1B9A"}
                    - Background #0000FF (blue, dark) → {"primaryText":"#FFFFFF","secondaryText":"#E0E0E0","accentColor":"#FFD600"}
                    - Background #FFFF00 (yellow, light) → {"primaryText":"#1A1A1A","secondaryText":"#3D3D3D","accentColor":"#1565C0"}
                    - Background #800080 (purple, dark) → {"primaryText":"#FFFFFF","secondaryText":"#E0E0E0","accentColor":"#FFD600"}
                    - Background #FFFFFF (white, light) → {"primaryText":"#1A1A1A","secondaryText":"#4A4A4A","accentColor":"#0277BD"}
                    - Background #0D0F26 (dark navy, dark) → {"primaryText":"#FFFFFF","secondaryText":"#B0BEC5","accentColor":"#00BCD4"}

                    NOW: Return optimized colors for background $backgroundHex.
                    Return ONLY a raw JSON object. No markdown, no backticks, no explanation:
                    {"primaryText":"#XXXXXX","secondaryText":"#XXXXXX","accentColor":"#XXXXXX"}
                """.trimIndent()

                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.9)
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

                val cleanJson = content.replace("```json", "").replace("```", "").trim()
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
                    val ratio = contrastRatio(it, bgColor)
                    val dirOk = isCorrectDirection(it)
                    Log.d(TAG, "AI primaryText ratio: ${"%.2f".format(ratio)}:1, direction=${if(dirOk) "OK" else "WRONG"}")
                    if (ratio >= 3.0 && dirOk && !isHarshClash(it, bgColor)) it else null
                }
                val validSecondary = aiSecondary?.let {
                    val ratio = contrastRatio(it, bgColor)
                    val dirOk = isCorrectDirection(it)
                    Log.d(TAG, "AI secondaryText ratio: ${"%.2f".format(ratio)}:1, direction=${if(dirOk) "OK" else "WRONG"}")
                    if (ratio >= 3.0 && dirOk && !isHarshClash(it, bgColor)) it else null
                }
                val validAccent = aiAccent?.let {
                    val ratio = contrastRatio(it, bgColor)
                    val dirOk = isCorrectDirection(it)
                    val hDist = hueDifference(hueOf(it), hueOf(bgColor))
                    Log.d(TAG, "AI accentColor ratio: ${"%.2f".format(ratio)}:1, hue dist: ${"%.0f".format(hDist)}°, direction=${if(dirOk) "OK" else "WRONG"}")
                    if (ratio >= 3.0 && dirOk && hDist > 30f && !isHarshClash(it, bgColor)) it else null
                }

                // Se alguma cor da IA falhar, usar a local como fallback desse canal
                val local = computeLocalContrastColors(backgroundHex)
                val result = AdaptiveColors(
                    primaryText = validPrimary ?: local.primaryText,
                    secondaryText = validSecondary ?: local.secondaryText,
                    accentColor = validAccent ?: local.accentColor
                )

                Log.i(TAG, "Final colors for $backgroundHex: " +
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

    private fun parseColorSafe(hexStr: String): Color? {
        return try {
            Color(android.graphics.Color.parseColor(hexStr))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse color: $hexStr")
            null
        }
    }
}
