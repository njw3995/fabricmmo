package io.github.njw3995.fabricmmo.core.skill.unarmed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnarmedBlockHandlerTest {
    @Test
    void mapsExactlyThePinnedBlockCrackerWhitelist() {
        assertEquals("cracked_stone_bricks", UnarmedBlockHandler.crackedPath("stone_bricks"));
        assertEquals("infested_cracked_stone_bricks",
                UnarmedBlockHandler.crackedPath("infested_stone_bricks"));
        assertEquals("cracked_deepslate_bricks",
                UnarmedBlockHandler.crackedPath("deepslate_bricks"));
        assertEquals("cracked_deepslate_tiles",
                UnarmedBlockHandler.crackedPath("deepslate_tiles"));
        assertEquals("cracked_polished_blackstone_bricks",
                UnarmedBlockHandler.crackedPath("polished_blackstone_bricks"));
        assertEquals("cracked_nether_bricks",
                UnarmedBlockHandler.crackedPath("nether_bricks"));
        assertEquals("", UnarmedBlockHandler.crackedPath("stone"));
    }
}
