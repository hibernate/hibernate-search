package org.hibernate.search.jsr352.internal;

import javax.batch.api.partition.PartitionReducer;
import javax.inject.Named;

import org.jboss.logging.Logger;

@Named
public class LucenePartitionReducer implements PartitionReducer {

    private static final Logger logger = Logger.getLogger(LucenePartitionReducer.class);
    
    @Override
    public void beginPartitionedStep() throws Exception {
        logger.info("#beginPartitionedStep() called.");
    }

    @Override
    public void beforePartitionedStepCompletion() throws Exception {
        logger.info("#beforePartitionedStepCompletion() called.");
    }

    @Override
    public void rollbackPartitionedStep() throws Exception {
        logger.info("#rollbackPartitionedStep() called.");
    }

    @Override
    public void afterPartitionedStepCompletion(PartitionStatus status)
            throws Exception {
        logger.info("#afterPartitionedStepCompletion(...) called.");
    }

}
