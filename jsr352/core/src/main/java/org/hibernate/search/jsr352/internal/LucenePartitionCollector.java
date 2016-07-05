package org.hibernate.search.jsr352.internal;

import java.io.Serializable;

import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LucenePartitionCollector implements PartitionCollector {

    @Inject
    private StepContext stepContext;
    
    /**
     * The collectPartitionData method receives control periodically during 
     * partition processing. This method receives control on each thread 
     * processing a partition as IdProducerBatchlet, once at the end of the 
     * process.
     */
    @Override
    public Serializable collectPartitionData() throws Exception {
        
        // get transient user data
        Object userData = stepContext.getTransientUserData();
        int workCount = userData != null ? (int) userData : 0;
        
        // once data collected, reset the counter 
        // to zero in transient user data
        stepContext.setTransientUserData(0);
        
        return workCount;
    }
}
