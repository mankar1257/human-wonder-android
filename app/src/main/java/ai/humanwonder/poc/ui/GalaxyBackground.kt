package ai.humanwonder.poc.ui

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

/**
 * The live procedural galaxy from the Stitch "Talk to Space (Live Galaxy)" screen: the same
 * GLSL shader, drawn with OpenGL ES 2.0 behind the Compose UI.
 */
@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    /** 0 = calm night, 1 = the sky is transmitting (nebula, core and aurora brighten). */
    energy: Float = 0f,
    /** Change this value to send an "uplink" pulse rising from the bottom of the screen. */
    pulseKey: Int = 0,
) {
    AndroidView(
        factory = { GalaxyView(it) },
        modifier = modifier,
        update = { view ->
            view.setEnergy(energy)
            view.setPulseKey(pulseKey)
        },
    )
}

/** Lets the Activity pause/resume the GL thread with its own lifecycle. */
object GalaxyLifecycle {
    private var view: WeakReference<GalaxyView>? = null
    internal fun attach(v: GalaxyView) { view = WeakReference(v) }
    fun onPause() { view?.get()?.pause() }
    fun onResume() { view?.get()?.resume() }
}

@SuppressLint("ViewConstructor")
class GalaxyView(context: Context) : GLSurfaceView(context), Choreographer.FrameCallback {

    private val renderer = GalaxyRenderer()
    private var running = false
    private var lastFrameNanos = 0L
    private var pulseKey = Int.MIN_VALUE

    fun setEnergy(target: Float) { renderer.targetEnergy = target.coerceIn(0f, 1f) }

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
    @Volatile private var pulseStart = -1L
    private var energy = 0f

    fun pulse() { pulseStart = SystemClock.elapsedRealtime() }

    private var program = 0
    private var aPosition = 0
    private var uTime = -1
    private var uResolution = -1
    private var uEnergy = -1
    private var uPulse = -1
    private var width = 1
    private var height = 1
    private val start = SystemClock.elapsedRealtime()

    private val quad: FloatBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder())
        .asFloatBuffer().apply {
            put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
            position(0)
        }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.024f, 0.086f, 0.090f, 1f) // #061617, shown if the shader can't run
        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program != 0) {
            aPosition = GLES20.glGetAttribLocation(program, "a_position")
            uTime = GLES20.glGetUniformLocation(program, "u_time")
            uResolution = GLES20.glGetUniformLocation(program, "u_resolution")
            uEnergy = GLES20.glGetUniformLocation(program, "u_energy")
            uPulse = GLES20.glGetUniformLocation(program, "u_pulse")
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

        // Fragment shader from the Stitch design (STITCH_SHADER ANIMATION_31), plus two Human
        // Wonder additions: u_energy (the sky brightens while it is answering) and u_pulse
        // (an uplink ring that rises from the bottom when a question is sent). Precision falls
        // back to mediump on GPUs without highp.
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

vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec3 permute(vec3 x) { return mod289(((x*34.0)+1.0)*x); }

float snoise(vec2 v) {
    const vec4 C = vec4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
    vec2 i  = floor(v + dot(v, C.yy));
    vec2 x0 = v - i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod289(i);
    vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
    vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);
    m = m*m;
    m = m*m;
    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0*a0 + h*h);
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
        p *= 2.05;
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
    vec2 p = (gl_FragCoord.xy - 0.5 * u_resolution.xy) / min(u_resolution.x, u_resolution.y);
    float t = u_time * 0.15;

    vec3 baseColor = vec3(0.024, 0.086, 0.090);

    float dist = length(p);
    vec2 spiralCoord = p * 1.6 + vec2(cos(dist * 2.2 - t * 0.6), sin(dist * 2.2 - t * 0.6)) * 0.4;

    float n1 = fbm(spiralCoord * 1.8 + vec2(t * 0.15, -t * 0.09));
    float n2 = fbm(p * 2.6 - vec2(t * 0.06, t * 0.12) + vec2(n1 * 0.45));
    float nebula = clamp(n1 * 0.6 + n2 * 0.4 + 0.2, 0.0, 1.0);

    vec3 cyanGlow = vec3(0.184, 0.902, 0.784);
    vec3 violetGlow = vec3(0.486, 0.227, 0.929);
    vec3 skyBlueGlow = vec3(0.12, 0.52, 0.95);

    float bottomHaze = smoothstep(0.85, 0.05, uv.y) * 0.9;
    vec3 aurora = mix(cyanGlow, violetGlow, sin(uv.x * 3.1415 + t * 1.2) * 0.5 + 0.5) * bottomHaze;

    vec3 cosmicDust = mix(violetGlow, skyBlueGlow, n2) * (nebula * 0.55);
    cosmicDust += cyanGlow * pow(max(0.0, n1), 2.2) * 0.6;

    float coreGlow = exp(-dist * 2.0) * 0.35;

    vec2 starGrid = gl_FragCoord.xy * 0.16;
    vec2 starId = floor(starGrid);
    float starRand = hash21(starId);
    float star = 0.0;
    if (starRand > 0.955) {
        vec2 starPos = fract(starGrid) - 0.5;
        float twinkle = sin(u_time * 2.8 + starRand * 6.28) * 0.5 + 0.5;
        float starDist = length(starPos);
        star = smoothstep(0.24, 0.02, starDist) * (0.65 + 0.35 * twinkle);
    }

    vec2 gridUV = fract(gl_FragCoord.xy / 22.0) - 0.5;
    float dotDist = length(gridUV);
    float dotPattern = smoothstep(0.08, 0.03, dotDist) * 0.14;

    float e = u_energy;
    vec3 finalColor = baseColor;
    finalColor += cosmicDust * 0.85 * (1.0 + 0.35 * e);
    finalColor += aurora * 0.85 * (1.0 + 0.55 * e);
    finalColor += cyanGlow * coreGlow * (0.45 + 0.9 * e);
    finalColor += vec3(star * 0.92, star * 0.96, star * 1.0) * (1.0 + 0.4 * e);
    finalColor += vec3(dotPattern);

    // Uplink pulse: a soft ring rising from the bottom centre, fading as it climbs.
    if (u_pulse < 2.6) {
        vec2 q = vec2((uv.x - 0.5) * u_resolution.x / u_resolution.y, uv.y + 0.08);
        float radius = u_pulse * 0.62;
        float ring = smoothstep(0.045, 0.0, abs(length(q) - radius));
        float fade = 1.0 - u_pulse / 2.6;
        finalColor += cyanGlow * ring * fade * fade * 0.55;
    }

    gl_FragColor = vec4(finalColor, 1.0);
}
"""
    }
}
