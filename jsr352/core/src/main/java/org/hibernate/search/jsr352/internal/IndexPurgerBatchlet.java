package org.hibernate.search.jsr352.internal;

import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.inject.Named;

import org.jboss.logging.Logger;

@Named
public class IndexPurgerBatchlet implements Batchlet {

    private static final Logger logger = Logger.getLogger(IndexPurgerBatchlet.class);
    
    @Override
    public String process() throws Exception {
        
        logger.info("purging entities ...");
        
        return BatchStatus.COMPLETED.toString();
    }

    @Override
    public void stop() throws Exception {
        // TODO Auto-generated method stub
    }
}
