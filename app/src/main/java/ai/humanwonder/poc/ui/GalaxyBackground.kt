package ai.humanwonder.poc.ui

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

/** False only in the screen checks (Robolectric can't run OpenGL); the sky is then drawn still. */
val LocalLiveSky = staticCompositionLocalOf { true }

/**
 * The live aurora sky behind the chat (after the Stitch home screen), drawn with OpenGL ES 2.0
 * behind the Compose UI.
 */
@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    /** 0 = resting, 1 = Orbit is answering (the aurora lifts and brightens a little). */
    energy: Float = 0f,
    /** 0 = the empty sky (tall aurora), 1 = a conversation (low, dimmer aurora behind the composer). */
    calm: Float = 0f,
    /** Change this value to send an "uplink" pulse rising from the bottom of the screen. */
    pulseKey: Int = 0,
) {
    if (!LocalLiveSky.current) {
        // A still stand-in for the screen checks, which have no OpenGL: dark sky, low teal glow.
        Box(
            modifier.background(
                Brush.verticalGradient(
                    0f to Color.Black, 0.55f to Color.Black,
                    1f to Color(0xFF0E3D37).copy(alpha = 1f - 0.5f * calm),
                ),
            ),
        )
        return
    }
    AndroidView(
        factory = { GalaxyView(it) },
        modifier = modifier,
        update = { view ->
            view.setEnergy(energy)
            view.setCalm(calm)
            view.setPulseKey(pulseKey)
        },
    )
}

/**
 * Lets the Activity pause/resume the GL threads with its own lifecycle. Home and Chat each have
 * a sky (both exist for a moment while one screen fades into the other), so every view is tracked.
 */
object GalaxyLifecycle {
    private val views = mutableListOf<WeakReference<GalaxyView>>()
    internal fun attach(v: GalaxyView) {
        views.removeAll { it.get() == null }
        views.add(WeakReference(v))
    }
    fun onPause() { views.mapNotNull { it.get() }.forEach { it.pause() } }
    fun onResume() { views.mapNotNull { it.get() }.forEach { it.resume() } }
}

@SuppressLint("ViewConstructor")
class GalaxyView(context: Context) : GLSurfaceView(context), Choreographer.FrameCallback {

    private val renderer = GalaxyRenderer()
    private var running = false
    private var lastFrameNanos = 0L
    private var pulseKey = Int.MIN_VALUE

    fun setEnergy(target: Float) { renderer.targetEnergy = target.coerceIn(0f, 1f) }

    fun setCalm(value: Float) { renderer.calm = value.coerceIn(0f, 1f) }

    fun setPulseKey(key: Int) {
        if (pulseKey != Int.MIN_VALUE && key != pulseKey) renderer.pulse()
        pulseKey = key
    }

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        GalaxyLifecycle.attach(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Render at density-independent resolution, exactly like the Stitch web canvas
        // (1 CSS px = 1 dp). The system scales it up; the nebula is soft so this looks the same
        // and costs a fraction of full resolution.
        val density = resources.displayMetrics.density
        holder.setFixedSize(max(1, (w / density).toInt()), max(1, (h / density).toInt()))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    fun pause() {
        stop()
        onPause()
    }

    fun resume() {
        // A sky whose screen has gone keeps no frame loop running.
        if (!isAttachedToWindow) return
        onResume()
        start()
    }

    private fun start() {
        if (running) return
        running = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        // ~30 fps is plenty for a slow nebula and leaves battery/heat for the language model.
        if (frameTimeNanos - lastFrameNanos >= 30_000_000L) {
            lastFrameNanos = frameTimeNanos
            requestRender()
        }
        Choreographer.getInstance().postFrameCallback(this)
    }
}

private class GalaxyRenderer : GLSurfaceView.Renderer {

    @Volatile var targetEnergy = 0f
    @Volatile var calm = 0f
    @Volatile private var pulseStart = -1L
    private var energy = 0f

    fun pulse() { pulseStart = SystemClock.elapsedRealtime() }

    private var program = 0
    private var aPosition = 0
    private var uTime = -1
    private var uResolution = -1
    private var uEnergy = -1
    private var uPulse = -1
    private var uCalm = -1
    private var width = 1
    private var height = 1
    private val start = SystemClock.elapsedRealtime()

    private val quad: FloatBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder())
        .asFloatBuffer().apply {
            put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
            position(0)
        }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f) // true black, shown if the shader can't run
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program != 0) {
            aPosition = GLES20.glGetAttribLocation(program, "a_position")
            uTime = GLES20.glGetUniformLocation(program, "u_time")
            uResolution = GLES20.glGetUniformLocation(program, "u_resolution")
            uEnergy = GLES20.glGetUniformLocation(program, "u_energy")
            uPulse = GLES20.glGetUniformLocation(program, "u_pulse")
            uCalm = GLES20.glGetUniformLocation(program, "u_calm")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = max(1, w)
        height = max(1, h)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return
        GLES20.glUseProgram(program)
        // Seconds since start, wrapped every hour to keep float precision on mediump GPUs.
        val t = ((SystemClock.elapsedRealtime() - start) % 3_600_000L) / 1000f
        GLES20.glUniform1f(uTime, t)
        GLES20.glUniform2f(uResolution, width.toFloat(), height.toFloat())
        // Ease toward the target so the sky "breathes" up and down instead of switching.
        energy += (targetEnergy - energy) * 0.06f
        GLES20.glUniform1f(uEnergy, energy)
        GLES20.glUniform1f(uCalm, calm)
        val ps = pulseStart
        GLES20.glUniform1f(uPulse, if (ps < 0) 99f else (SystemClock.elapsedRealtime() - ps) / 1000f)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPosition)
    }

    private fun buildProgram(vs: String, fs: String): Int {
        val v = compile(GLES20.GL_VERTEX_SHADER, vs)
        val f = compile(GLES20.GL_FRAGMENT_SHADER, fs)
        if (v == 0 || f == 0) return 0
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) {
            Log.e(TAG, "Galaxy shader link failed: ${GLES20.glGetProgramInfoLog(p)}")
            GLES20.glDeleteProgram(p)
            return 0
        }
        return p
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) {
            Log.e(TAG, "Galaxy shader compile failed: ${GLES20.glGetShaderInfoLog(s)}")
            GLES20.glDeleteShader(s)
            return 0
        }
        return s
    }

    private companion object {
        const val TAG = "HumanWonder"

        const val VERTEX_SHADER = """
attribute vec2 a_position;
void main() {
  gl_Position = vec4(a_position, 0.0, 1.0);
}
"""

        // The "aurora valley" from the Stitch home screen: a ribbon of violet-blue-teal light,
        // low in the middle and climbing both sides, over true black. u_calm (0 = empty sky,
        // 1 = conversation) lowers and dims it so it never sits behind text; u_energy lifts it
        // slightly while Orbit answers; u_pulse sends a soft ring up when a question is sent.
        // Precision falls back to mediump on GPUs without highp.
        const val FRAGMENT_SHADER = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform float u_time;
uniform vec2 u_resolution;
uniform float u_energy;
uniform float u_pulse;
uniform float u_calm;

vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec3 permute(vec3 x) { return mod289(((x * 34.0) + 1.0) * x); }

float snoise(vec2 v) {
    const vec4 C = vec4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
    vec2 i  = floor(v + dot(v, C.yy));
    vec2 x0 = v - i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod289(i);
    vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
    vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
    m = m * m;
    m = m * m;
    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
    vec3 g;
    g.x  = a0.x  * x0.x  + h.x  * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
}

float fbm(vec2 p) {
    float f = 0.0;
    float w = 0.5;
    for (int i = 0; i < 4; i++) {
        f += w * snoise(p);
        p *= 2.03;
        w *= 0.5;
    }
    return f;
}

float hash21(vec2 p) {
    p = fract(p * vec2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution.xy;
    float t = u_time;
    float calm = u_calm;
    float e = u_energy;

    // The aurora valley: a ribbon of light, low in the middle, climbing both sides of the screen.
    float x = uv.x - 0.5;
    float edge = pow(abs(x) * 2.0, 2.3);
    float base = mix(0.15, 0.07, calm) + 0.015 * e;
    float lift = mix(0.56, 0.15, calm);
    float wobble = fbm(vec2(uv.x * 1.8 + t * 0.025, t * 0.04)) * 0.06;
    float ridgeY = base + lift * edge + wobble;
    float d = uv.y - ridgeY;

    // Falls off quickly above the ribbon and slowly below it (the haze underneath).
    float above = exp(-max(d, 0.0) / 0.095);
    float below = mix(1.0, 0.32, smoothstep(0.0, 0.5, -d));
    float glow = d > 0.0 ? above : below;

    // Rays drifting along the ribbon.
    float rays = fbm(vec2(uv.x * 5.0 - t * 0.05, d * 9.0 + t * 0.02));
    glow *= 0.72 + 0.55 * rays;

    float ridge = exp(-abs(d) / 0.045);

    vec3 deep = vec3(0.07, 0.08, 0.26);
    vec3 violet = vec3(0.55, 0.40, 1.00);
    vec3 blue = vec3(0.28, 0.50, 1.00);
    vec3 cyan = vec3(0.22, 0.92, 0.82);
    float hx = uv.x + 0.12 * sin(t * 0.06);
    vec3 band = mix(violet, blue, smoothstep(0.15, 0.55, hx));
    band = mix(band, cyan, smoothstep(0.62, 1.05, hx) * 0.85);
    vec3 aurora = mix(deep, band, clamp(ridge * 1.1 + 0.25, 0.0, 1.0)) * glow;
    aurora += vec3(0.85, 0.85, 1.0) * ridge * ridge * 0.25 * glow;
    aurora *= mix(1.0, 0.58, calm) * (1.0 + 0.3 * e);

    // A few far stars above the ribbon.
    vec2 starGrid = gl_FragCoord.xy * 0.11;
    vec2 starId = floor(starGrid);
    float starRand = hash21(starId);
    float star = 0.0;
    if (starRand > 0.986) {
        vec2 sp = fract(starGrid) - 0.5;
        float tw = sin(t * 1.6 + starRand * 40.0) * 0.5 + 0.5;
        star = (1.0 - smoothstep(0.0, 0.22, length(sp))) * (0.35 + 0.35 * tw);
    }
    star *= smoothstep(0.0, 0.12, d) * mix(1.0, 0.45, calm);

    vec3 col = aurora + vec3(star);

    // Uplink pulse: a soft ring rising from the bottom centre when a question is sent.
    if (u_pulse < 2.4) {
        vec2 q = vec2((uv.x - 0.5) * u_resolution.x / u_resolution.y, uv.y + 0.05);
        float ring = 1.0 - smoothstep(0.0, 0.05, abs(length(q) - u_pulse * 0.5));
        float fade = 1.0 - u_pulse / 2.4;
        col += band * ring * fade * fade * 0.35;
    }

    gl_FragColor = vec4(col, 1.0);
}
"""
    }
}
