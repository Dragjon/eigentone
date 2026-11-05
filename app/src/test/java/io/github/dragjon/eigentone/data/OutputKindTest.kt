package io.github.dragjon.eigentone.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OutputKindTest {
    private fun bt(name: String) = OutputKind.guess("Bluetooth:$name", name)

    @Test
    fun thePhonesOwnSpeakerIsThePhone() {
        assertEquals(OutputKind.PHONE, OutputInfo("speaker", "Phone speaker").kind)
    }

    @Test
    fun namesSayWhatABluetoothDeviceIs() {
        assertEquals(OutputKind.EARBUDS, bt("Galaxy Buds2 Pro"))
        assertEquals(OutputKind.EARBUDS, bt("AirPods Pro"))
        assertEquals(OutputKind.EARBUDS, bt("WF-1000XM5"))
        assertEquals(OutputKind.SPEAKER, bt("JBL Flip 6"))
        assertEquals(OutputKind.SPEAKER, bt("Bose SoundLink Flex"))
        assertEquals(OutputKind.CAR, bt("My Car"))
        assertEquals(OutputKind.CAR, bt("SYNC"))
        assertEquals(OutputKind.HEADPHONES, bt("WH-1000XM5"))
        assertEquals(OutputKind.HEADPHONES, bt("JBL Tune 510BT"))
        assertEquals(OutputKind.HEADPHONES, OutputKind.guess("Wired:Wired", "Wired"))
    }

    @Test
    fun aDeviceThatSaysItIsASpeakerIsOne() {
        assertEquals(OutputKind.SPEAKER, OutputKind.guess("Bluetooth:Living room", "Living room", speakerType = true))
    }

    @Test
    fun wordsInsideOtherWordsDontCount() {
        // "Scarlett" holds "car", and "Recharge" holds "charge".
        assertEquals(OutputKind.HEADPHONES, bt("Scarlett Solo"))
        assertEquals(OutputKind.HEADPHONES, bt("Recharge HP"))
    }
}
