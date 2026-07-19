package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TreeFellerSearchTest {
    @Test
    void traversalExcludesStartingBlockAndFindsConnectedTreeParts() {
        TreeFellerSearch.Node start = new TreeFellerSearch.Node(0, 0, 0);
        Map<TreeFellerSearch.Node, TreeFellerSearch.Kind> blocks = Map.of(
                new TreeFellerSearch.Node(0, 1, 0), TreeFellerSearch.Kind.WOOD_XP,
                new TreeFellerSearch.Node(0, 2, 0), TreeFellerSearch.Kind.WOOD_XP,
                new TreeFellerSearch.Node(1, 2, 0), TreeFellerSearch.Kind.NON_WOOD_TREE_PART);
        TreeFellerSearch.Result result = new TreeFellerSearch(access(blocks, Set.of()), 100)
                .search(start);
        assertFalse(result.blocks().contains(start));
        assertEquals(3, result.blocks().size());
        assertFalse(result.thresholdReached());
    }

    @Test
    void placedOrOtherwiseIneligibleBlocksStopTraversal() {
        TreeFellerSearch.Node blocked = new TreeFellerSearch.Node(0, 1, 0);
        Map<TreeFellerSearch.Node, TreeFellerSearch.Kind> blocks = Map.of(
                blocked, TreeFellerSearch.Kind.WOOD_XP,
                new TreeFellerSearch.Node(0, 2, 0), TreeFellerSearch.Kind.WOOD_XP);
        TreeFellerSearch.Result result = new TreeFellerSearch(access(blocks, Set.of(blocked)), 100)
                .search(new TreeFellerSearch.Node(0, 0, 0));
        assertTrue(result.blocks().isEmpty());
    }

    @Test
    void thresholdStopsBeforeThirdVerticalLogLikeUpstreamGreaterThanCheck() {
        TreeFellerSearch.Node start = new TreeFellerSearch.Node(0, 0, 0);
        Map<TreeFellerSearch.Node, TreeFellerSearch.Kind> blocks = Map.of(
                new TreeFellerSearch.Node(0, 1, 0), TreeFellerSearch.Kind.WOOD_XP,
                new TreeFellerSearch.Node(0, 2, 0), TreeFellerSearch.Kind.WOOD_XP,
                new TreeFellerSearch.Node(0, 3, 0), TreeFellerSearch.Kind.WOOD_XP);
        // mcMMO checks size > threshold before classifying each target. With threshold 1,
        // the first two vertical logs are collected; the next target check marks the threshold
        // and processTree returns before the third vertical log is reached recursively.
        TreeFellerSearch.Result result = new TreeFellerSearch(access(blocks, Set.of()), 1)
                .search(start);
        assertTrue(result.thresholdReached());
        assertEquals(2, result.blocks().size());
        assertTrue(result.blocks().contains(new TreeFellerSearch.Node(0, 1, 0)));
        assertTrue(result.blocks().contains(new TreeFellerSearch.Node(0, 2, 0)));
        assertFalse(result.blocks().contains(new TreeFellerSearch.Node(0, 3, 0)));
    }

    private static TreeFellerSearch.BlockAccess access(
            Map<TreeFellerSearch.Node, TreeFellerSearch.Kind> blocks,
            Set<TreeFellerSearch.Node> ineligible) {
        return new TreeFellerSearch.BlockAccess() {
            @Override
            public TreeFellerSearch.Kind kind(TreeFellerSearch.Node node) {
                return blocks.getOrDefault(node, TreeFellerSearch.Kind.OTHER);
            }

            @Override
            public boolean eligible(TreeFellerSearch.Node node) {
                return !ineligible.contains(node);
            }
        };
    }
}
