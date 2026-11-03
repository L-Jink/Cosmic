package client.processor.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FredrickProcessorTest {
    private static final byte OK = 0x0;
    private static final byte MESO_OVERFLOW = 0x1F;
    private static final byte NO_SLOTS = 0x20;
    private static final byte CANNOT_PAY_FEE = 0x21;
    private static final byte UNIQUE_HELD = 0x22;

    @Test
    void retrievalSucceedsWithSpaceAndMesoRoom() {
        assertEquals(OK, FredrickProcessor.retrievalResponse(true, false, 5000, true, 0));
    }

    @Test
    void fullInventoryRefusesRetrieval() {
        assertEquals(NO_SLOTS, FredrickProcessor.retrievalResponse(false, false, 0, true, 0));
    }

    @Test
    void oneOfAKindItemAlreadyHeldRefusesRetrieval() {
        assertEquals(UNIQUE_HELD, FredrickProcessor.retrievalResponse(false, true, 0, true, 0));
    }

    @Test
    void mesoOverflowRefusesRetrieval() {
        assertEquals(MESO_OVERFLOW, FredrickProcessor.retrievalResponse(true, false, 1000, false, Integer.MAX_VALUE));
    }

    @Test
    void storageFeeRequiresEnoughMesos() {
        // negative net meso = fee owed to Fredrick
        assertEquals(CANNOT_PAY_FEE, FredrickProcessor.retrievalResponse(true, false, -5000, true, 4999));
        assertEquals(OK, FredrickProcessor.retrievalResponse(true, false, -5000, true, 5000));
        assertEquals(CANNOT_PAY_FEE, FredrickProcessor.retrievalResponse(true, false, Integer.MIN_VALUE, true, Integer.MAX_VALUE));
    }

    @Test
    void zeroNetMesoNeedsNothing() {
        assertEquals(OK, FredrickProcessor.retrievalResponse(true, false, 0, true, 0));
    }
}
