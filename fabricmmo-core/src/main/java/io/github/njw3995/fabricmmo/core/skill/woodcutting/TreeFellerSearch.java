package io.github.njw3995.fabricmmo.core.skill.woodcutting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Pure port of mcMMO 2.3.000's Tree Feller cylinder/cube traversal. */
public final class TreeFellerSearch {
    private static final int[][] DIRECTIONS = {
            {-2, -1}, {-2, 0}, {-2, 1},
            {-1, -2}, {-1, -1}, {-1, 0}, {-1, 1}, {-1, 2},
            {0, -2}, {0, -1}, {0, 1}, {0, 2},
            {1, -2}, {1, -1}, {1, 0}, {1, 1}, {1, 2},
            {2, -1}, {2, 0}, {2, 1}
    };

    private final BlockAccess access;
    private final int threshold;
    private boolean thresholdReached;

    public TreeFellerSearch(BlockAccess access, int threshold) {
        this.access = Objects.requireNonNull(access, "access");
        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold must be positive");
        }
        this.threshold = threshold;
    }

    public Result search(Node startingBlock) {
        Set<Node> blocks = new HashSet<>();
        thresholdReached = false;
        processTree(Objects.requireNonNull(startingBlock, "startingBlock"), blocks);
        return new Result(Set.copyOf(blocks), thresholdReached);
    }

    private void processTree(Node block, Set<Node> blocks) {
        if (thresholdReached) {
            return;
        }
        List<Node> futureCenters = new ArrayList<>();
        if (processTarget(block.offset(0, 1, 0), futureCenters, blocks)) {
            for (int[] direction : DIRECTIONS) {
                processTarget(block.offset(direction[0], 0, direction[1]), futureCenters, blocks);
                if (thresholdReached) {
                    return;
                }
            }
        } else {
            processTarget(block.offset(0, -1, 0), futureCenters, blocks);
            for (int y = -1; y <= 1; y++) {
                for (int[] direction : DIRECTIONS) {
                    processTarget(block.offset(direction[0], y, direction[1]), futureCenters, blocks);
                    if (thresholdReached) {
                        return;
                    }
                }
            }
        }
        for (Node futureCenter : futureCenters) {
            if (thresholdReached) {
                return;
            }
            processTree(futureCenter, blocks);
        }
    }

    private boolean processTarget(Node block, List<Node> futureCenters, Set<Node> blocks) {
        if (blocks.contains(block) || !access.eligible(block)) {
            return false;
        }
        if (blocks.size() > threshold) {
            thresholdReached = true;
        }
        Kind kind = access.kind(block);
        if (kind == Kind.WOOD_XP) {
            blocks.add(block);
            futureCenters.add(block);
            return true;
        }
        if (kind == Kind.NON_WOOD_TREE_PART) {
            blocks.add(block);
        }
        return false;
    }

    public enum Kind {
        OTHER,
        WOOD_XP,
        NON_WOOD_TREE_PART
    }

    public interface BlockAccess {
        Kind kind(Node node);

        boolean eligible(Node node);
    }

    public record Node(int x, int y, int z) {
        public Node offset(int dx, int dy, int dz) {
            return new Node(x + dx, y + dy, z + dz);
        }
    }

    public record Result(Set<Node> blocks, boolean thresholdReached) {
        public Result {
            blocks = Set.copyOf(blocks);
        }
    }
}
