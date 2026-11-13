package ai.humanwonder.poc.ai

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Orbit's archive: the bundled catalog parses, every picture it names is in the app, the picker
 * agrees with the reference rules (tools/frames/pick-cases.json), and the answer clean-up removes
 * what the model sometimes adds. Plain JVM test, no phone needed.
 */
class FramesTest {
    private val catalog = FrameCatalog.parse(File("src/main/assets/frames/frames.json").readText())

    @Test
    fun everyFrameHasItsPicture() {
        assertTrue(catalog.frames.size >= 20)
        for (f in catalog.frames) {
            assertTrue("missing ${f.assetPath}", File("src/main/assets/${f.assetPath}").isFile)
        }
        for (r in catalog.regions) for (id in r.night + r.day) assertTrue("region ${r.id} names $id", catalog.frame(id) != null)
    }

    @Test
    fun pickerAgreesWithReferenceCases() {
        val cases = Json.parseToJsonElement(File("../tools/frames/pick-cases.json").readText()).jsonObject["cases"]!!.jsonArray
        val wrong = mutableListOf<String>()
        for (c in cases) {
            val o = c.jsonObject
            val got = FramePicker.pick(
                catalog,
                question = o["q"]!!.jsonPrimitive.content,
                context = o["context"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                shown = o["shown"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet().orEmpty(),
                hour = o["hour"]?.jsonPrimitive?.intOrNull,
                city = o["city"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )?.id
            val want = o["want"]
            val ok = when (want) {
                null, is JsonNull -> got == null
                is JsonArray -> got in want.map { it.jsonPrimitive.content }
                is JsonPrimitive -> got == want.content
                else -> false
            }
            if (!ok) wrong += "${o["q"]}: got $got, want $want"
        }
        assertEquals(wrong.joinToString("\n"), 0, wrong.size)
    }

    @Test
    fun answerCleanUp() {
        val raw = "**Radio:**\n*Reading you clearly.*\n\nI'm sending a frame from my archive: the Gulf at night. ✨🌙\n\n" +
            "*You are sending a frame from your archive.*\n(Describe it as you see it from orbit.)"
        val shown = Persona.visibleAnswer(raw)
        assertEquals("Reading you clearly. I'm sending a frame from my archive: the Gulf at night.", shown)
        assertFalse(Persona.visibleAnswer("Radio voice: Reading you clearly. Hello").startsWith("Radio"))
        assertEquals("Orbit 412 here.", Persona.visibleAnswer("<think>\n\n</think>\n\nOrbit 412 here."))
    }

    @Test
    fun frameLineGoesWithTheQuestion() {
        val f = catalog.frame("gulf-night")!!
        val p = Persona.userPrompt("Show me the Gulf", ai.humanwonder.poc.Perspective.Orbit, ai.humanwonder.poc.Tone.Balanced, f)
        assertTrue(p.startsWith("Show me the Gulf\n\n("))
        assertTrue(p.contains(f.says))
        assertTrue(p.endsWith(") /no_think"))
        assertFalse(Persona.systemPrompt("Sam").contains("Example."))
    }
}
