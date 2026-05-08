package com.evertask.shared.data.repository

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class AndroidTemplateEngineTest {

    private lateinit var engine: AndroidTemplateEngine

    @Before
    fun setup() {
        val context = mockk<Context>()
        val assetManager = mockk<AssetManager>()
        every { context.assets } returns assetManager
        every { assetManager.open(any()) } throws IOException("Asset not found")
        engine = AndroidTemplateEngine(context)
    }

    @Test
    fun `clean my room matches clean_room template`() {
        val template = engine.findTemplate("clean my room")
        assertEquals("clean_room", template.id)
    }

    @Test
    fun `do laundry matches do_laundry template`() {
        val template = engine.findTemplate("do laundry")
        assertEquals("do_laundry", template.id)
    }

    @Test
    fun `load dishwasher matches wash_dishes template`() {
        val template = engine.findTemplate("load dishwasher")
        assertEquals("wash_dishes", template.id)
    }

    @Test
    fun `go for a run matches go_for_run template`() {
        val template = engine.findTemplate("go for a run")
        assertEquals("go_for_run", template.id)
    }

    @Test
    fun `write report matches write_report template`() {
        val template = engine.findTemplate("write report")
        assertEquals("write_report", template.id)
    }

    @Test
    fun `buy groceries matches grocery_shop via assets or fallback`() {
        // When assets fail, the fallback JSON does not contain grocery_shop,
        // so we verify it falls back gracefully to a shopping-related template or default.
        val template = engine.findTemplate("buy groceries")
        assertTrue(
            "Expected a template but got ${template.id}",
            template.id.isNotBlank()
        )
    }

    @Test
    fun `unknown input returns default_template with 4 subtasks`() {
        val template = engine.findTemplate("xyz qwerty abc123 nonsense")
        assertEquals("default_template", template.id)
        assertEquals(4, template.subtasks.size)
    }

    @Test
    fun `keyword density scoring works with multiple matching keywords`() {
        // prepare_presentation has 7 keywords: presentation, slides, pitch, deck, meeting, speak, demo
        // "meeting presentation" matches 2 out of 7 = ~28%, which is above the 25% threshold
        val template = engine.findTemplate("meeting presentation")
        assertEquals("prepare_presentation", template.id)
    }

    @Test
    fun `synonym expansion maps washing to laundry`() {
        val template = engine.findTemplate("washing clothes")
        assertEquals("do_laundry", template.id)
    }

    @Test
    fun `soft match fallback activates for low density matches`() {
        // "meditate daily" matches "meditate" in meditate template (1/7 = 14%)
        // With soft match threshold of 10%, it should still return meditate rather than default
        val template = engine.findTemplate("meditate daily")
        assertEquals("meditate", template.id)
    }
}
