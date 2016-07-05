package org.hibernate.search.jsr352.internal;

import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.inject.Named;

import org.jboss.logging.Logger;

@Named
public class OptimizerBatchlet implements Batchlet {

    private static final Logger logger = Logger.getLogger(OptimizerBatchlet.class);
    
    @Override
    public String process() throws Exception {
        logger.info("Optimizing ...");
        return BatchStatus.COMPLETED.toString();
    }

    @Override
    public void stop() throws Exception {
        
    }
}
