package com.phonons.app.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ShapeLibraryTest {
    private val all = ShapeLibrary.all

    @Test
    fun libraryIsLargeAndEveryCategoryHasShapes() {
        assertTrue("only ${all.size} shapes", all.size >= 500)
        for (category in ShapeCategory.entries) {
            assertTrue(category.label, all.count { it.category == category } >= 25)
        }
        // Printed so the count quoted in the app and README can be checked against the real one.
        println("Library: ${all.size}, " + ShapeCategory.entries.joinToString { c -> "${c.label} ${all.count { it.category == c }}" })
    }

    @Test
    fun namesAndIdsAreUnique() {
        assertEquals(all.map { it.name }.filter { n -> all.count { it.name == n } > 1 }.toSet(), emptySet<String>())
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test
    fun noTwoShapesAreTheSameCurve() {
        val duplicates = all.groupBy { it.curve }.values.filter { it.size > 1 }.map { group -> group.map { it.name } }
        assertEquals(emptyList<List<String>>(), duplicates)
    }

    @Test
    fun everyShapeIsWellFormed() {
        for (shape in all) {
            val freqs = shape.points.map { it.first }
            assertEquals("${shape.name} frequencies out of order", freqs.sorted(), freqs)
            assertEquals("${shape.name} repeats a frequency", freqs.size, freqs.toSet().size)
            assertTrue("${shape.name} outside 20 Hz to 40 kHz", freqs.all { it in 10.0..40000.0 })
            assertTrue("${shape.name} too extreme", shape.points.all { abs(it.second) <= 12.0 })
            assertTrue("${shape.name} has no description", shape.description.isNotBlank())
        }
    }

    @Test
    fun everyShapeChangesTheSoundAtEveryBandCount() {
        for (bands in GraphicEq.BAND_OPTIONS) {
            for (shape in all) {
                assertFalse("${shape.name} is flat at $bands bands", GraphicEq.isFlat(GraphicEq.apply(shape.shape, bands)))
            }
        }
    }

    @Test
    fun searchFindsCommonSpellings() {
        for (query in listOf("hip hop", "hip-hop", "hiphop")) {
            assertEquals(query, "Hip-hop", ShapeLibrary.search(query).first().name)
        }
        assertEquals("R&B", ShapeLibrary.search("r&b").first().name)
        assertTrue(ShapeLibrary.search("rnb").any { it.name == "R&B" })
        assertTrue(ShapeLibrary.search("dnb").any { it.name == "Drum and bass" })
        assertTrue(ShapeLibrary.search("KPOP").any { it.name == "K-pop" })
    }

    @Test
    fun searchRequiresEveryWordAndIgnoresOrder() {
        val results = ShapeLibrary.search("treble 8 kHz")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { "treble" in it.searchText && "8" in it.searchText })
        assertEquals(results.toSet(), ShapeLibrary.search("8 kHz treble").toSet())
        assertTrue(ShapeLibrary.search("zzzznotashape").isEmpty())
    }

    @Test
    fun handWrittenShapesRankAheadOfBuildingBlocks() {
        val results = ShapeLibrary.search("bass")
        val firstBlock = results.indexOfFirst { it.category == ShapeCategory.BLOCKS }
        val lastWritten = results.indexOfLast { it.category != ShapeCategory.BLOCKS }
        assertTrue(firstBlock >= 0)
        assertTrue(lastWritten in 0 until firstBlock)
        assertTrue(results.first().name.contains("bass", ignoreCase = true))
    }

    @Test
    fun theUsersSoundcoreCurveIsFoundAndHitsItsPoints() {
        for (query in listOf("my soundcore", "soundcore", "acoustic soundcore")) {
            assertEquals(query, "My soundcore curve", ShapeLibrary.search(query).first().name)
        }
        val shape = all.first { it.name == "My soundcore curve" }
        val expected = listOf(100.0 to 4.0, 200.0 to 1.0, 400.0 to 2.0, 800.0 to 2.0, 1600.0 to 4.0, 3200.0 to 4.0, 6400.0 to 4.0, 12800.0 to 2.0)
        assertEquals(expected, shape.points)
        // The nearest faders at 31 bands stay within half a step of the curve it was read from.
        val gains = GraphicEq.apply(shape.shape, 31)
        val freqs = GraphicEq.frequencies(31)
        assertEquals(4.0, gains[freqs.indexOf(100.0)], 0.0)
        assertEquals(2.0, gains[freqs.indexOf(800.0)], 0.0)
        assertEquals(4.0, gains[freqs.indexOf(2000.0)], 0.0)
    }

    @Test
    fun numbersMatchWholeAndRespectTheirSign() {
        fun names(query: String) = ShapeLibrary.search(query).map { it.name }
        assertTrue("Bass +4 dB below 100 Hz" in names("bass 4 100"))
        assertTrue("Bass -4 dB below 100 Hz" in names("bass 4 100"))
        assertTrue("Bass +4 dB below 100 Hz" in names("bass +4 100"))
        assertFalse("Bass -4 dB below 100 Hz" in names("bass +4 100"))
        assertTrue("Bass -4 dB below 100 Hz" in names("bass -4 100"))
        assertFalse("Bass +4 dB below 100 Hz" in names("bass -4 100"))
        assertFalse(names("treble 1 khz").any { it.startsWith("Treble") && "12 kHz" in it })
    }

    @Test
    fun filtersCombine() {
        val results = ShapeLibrary.search("", ShapeCategory.MUSIC, ShapeTrait.MORE_BASS)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.category == ShapeCategory.MUSIC && ShapeTrait.MORE_BASS in it.traits })

        val favourite = all[3]
        assertEquals(listOf(favourite), ShapeLibrary.search("", only = setOf(favourite.id)))
        assertTrue(ShapeLibrary.search("", only = emptySet()).isEmpty())
    }

    @Test
    fun everyTraitFilterFindsShapes() {
        for (trait in ShapeTrait.entries) {
            assertTrue(trait.label, ShapeLibrary.search("", trait = trait).size >= 10)
        }
    }

    @Test
    fun traitsDescribeTheCurve() {
        fun traits(name: String) = ShapeLibrary.search(name).first { it.name == name }.traits
        assertTrue(ShapeTrait.MORE_BASS in traits("Basshead"))
        assertTrue(ShapeTrait.LESS_BASS in traits("Podcast"))
        assertTrue(ShapeTrait.LESS_TREBLE in traits("Falling asleep"))
        assertTrue(ShapeTrait.MORE_TREBLE in traits("Airy"))
        assertTrue(ShapeTrait.SCOOPED in traits("Big V"))
        assertTrue(ShapeTrait.MORE_MIDS in traits("Too hollow"))
        assertTrue(ShapeTrait.GENTLE in traits("Natural"))
        assertTrue(ShapeTrait.STRONG in traits("Bass cannon"))
    }

    @Test
    fun amountScalesAShapeAndStaysInRange() {
        val shape = all.first { it.name == "Bass cannon" }.shape
        val full = GraphicEq.apply(shape, 31)
        val half = GraphicEq.apply(shape, 31, amount = 0.5)
        full.indices.forEach { assertEquals(full[it] * 0.5, half[it], 0.26) }
        assertTrue(GraphicEq.apply(shape, 31, amount = 1.5).all { abs(it) <= GraphicEq.MAX_DB })
        assertTrue(GraphicEq.isFlat(GraphicEq.apply(shape, 31, amount = 0.0)))
    }

    @Test
    fun appliedShapesStayFlatBeyondTheirLastPointWhenTheyEndAtZero() {
        val bass = all.first { it.name == "Sub bass" }
        val freqs = GraphicEq.frequencies(31)
        val gains = GraphicEq.apply(bass.shape, 31)
        assertEquals(0.0, gains[freqs.indexOf(1000.0)], 0.0)
        assertTrue(gains[freqs.indexOf(31.5)] > 4.0)
    }
}
