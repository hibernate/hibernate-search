package org.hibernate.search.jsr352.internal;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;

@Named
public class LucenePartitionAnalyzer implements PartitionAnalyzer {

    @Inject
    private IndexingContext indexingContext;
    
    @Inject @BatchProperty
    private int maxResults;
    
    private int workCount = 0;
    private float percentage = 0;
    
    private static final Logger logger = Logger.getLogger(LucenePartitionAnalyzer.class);
    
    /**
     * Analyze data obtained from different partition plans via partition data
     * collectors. The current analyze is to summarize to their progresses : 
     * 
     *     workCount = workCount1 + workCount2 + ... + workCountN
     * 
     * Then it shows the total mass index progress in percentage. This method is
     * very similar to the current simple progress monitor. Note: concerning
     * the number of total entities loaded, it depends on 2 values : the number
     * of row in the database table and the max results to process, defined by
     * user before the job start. So the minimum between them will be used.
     * 
     * @param fromCollector the checkpoint obtained from partition collector's 
     *          collectPartitionData
     */
    @Override
    public void analyzeCollectorData(Serializable fromCollector) throws Exception {
        
        long entityCount = indexingContext.getEntityCount();
        int entitiesLoaded = Math.min((int) entityCount, maxResults);

        workCount += (int) fromCollector;
        if (entitiesLoaded != 0) {
            percentage = workCount * 100f / entitiesLoaded;
        }
        logger.infof("%d works processed (%.1f%%).",
                workCount, percentage);
    }

    @Override
    public void analyzeStatus(BatchStatus batchStatus, String exitStatus)
            throws Exception {
        logger.info("analyzeStatus called.");
    }
}
