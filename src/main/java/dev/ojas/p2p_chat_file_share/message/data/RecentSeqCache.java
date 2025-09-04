package dev.ojas.p2p_chat_file_share.message.data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Small synchronized bounded set. Keeps insertion order; evicts oldest when capacity reached.
 * Used to store recently seen seq numbers to detect duplicates.
 */
public class RecentSeqCache {
    private final int capacity;
    private final LinkedHashSet<Long> set;

    public RecentSeqCache(int capacity) {
        this.capacity = capacity;
        this.set = new LinkedHashSet<>(capacity);
    }

    /**
     * Returns true if this seq was previously unseen (and now recorded).
     * Returns false if it was already present (duplicate).
     */
    public synchronized boolean recordIfNew(long seq) {
        if (set.contains(seq)) return false;
        set.add(seq);
        if (set.size() > capacity) {
            // remove oldest
            Long first = set.iterator().next();
            set.remove(first);
        }
        return true;
    }
}
