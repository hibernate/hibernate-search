package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.jsr352.MassIndexer;
import org.hibernate.search.jsr352.MassIndexerImpl;
import org.junit.Test;

public class MassIndexerTest {

    private final boolean OPTIMIZE_AFTER_PURGE = true;
    private final boolean OPTIMIZE_AT_END = true;
    private final boolean PURGE_AT_START = true;
    private final int ARRAY_CAPACITY = 500;
    private final int FETCH_SIZE = 100000;
    private final int MAX_RESULTS = 1000000;
    private final int PARTITION_CAPACITY = 500;
    private final int PARTITIONS = 4;
    private final int THREADS = 2;

    /*
     * Test if all params are correctly set
     */
    @Test
    public void testJobParams() {
        
        MassIndexer massIndexer = new MassIndexerImpl()
                .arrayCapacity(ARRAY_CAPACITY)
                .fetchSize(FETCH_SIZE)
                .maxResults(MAX_RESULTS)
                .optimizeAfterPurge(OPTIMIZE_AFTER_PURGE)
                .optimizeAtEnd(OPTIMIZE_AT_END)
                .partitionCapacity(PARTITION_CAPACITY)
                .partitions(PARTITIONS)
                .purgeAtStart(PURGE_AT_START)
                .threads(THREADS);
        
        assertEquals(ARRAY_CAPACITY, massIndexer.getArrayCapacity());
        assertEquals(FETCH_SIZE, massIndexer.getFetchSize());
        assertEquals(MAX_RESULTS, massIndexer.getMaxResults());
        assertEquals(OPTIMIZE_AFTER_PURGE, massIndexer.isOptimizeAfterPurge());
        assertEquals(OPTIMIZE_AT_END, massIndexer.isOptimizeAtEnd());
        assertEquals(PARTITION_CAPACITY, massIndexer.getPartitionCapacity());
        assertEquals(PARTITIONS, massIndexer.getPartitions());
        assertEquals(PURGE_AT_START, massIndexer.isPurgeAtStart());
        assertEquals(THREADS, massIndexer.getThreads());
    }
    
    /**
     * Test if the set of root entities is set correctly via toString() method
     */
    @Test
    public void testRootEntities_notNull() {
        
        Set<Class<?>> rootEntities = new HashSet<>();
        rootEntities.add(String.class);
        rootEntities.add(Integer.class);
        
        MassIndexer massIndexer = new MassIndexerImpl().addRootEntities(rootEntities);
        Set<Class<?>> _rootEntities = massIndexer.getRootEntities();
        
        assertTrue(_rootEntities.contains(String.class));
        assertTrue(_rootEntities.contains(Integer.class));
    }
    
    @Test(expected=NullPointerException.class)
    public void testRootEntities_empty() {
        new MassIndexerImpl().addRootEntities(new HashSet<Class<?>>());
    }
}
