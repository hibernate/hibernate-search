package org.hibernate.search.jsr352.internal;

import javax.batch.api.BatchProperty;
import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Decider decides the next step-execution after the end of index chunk. If
 * user TODO: add description 
 * 
 * @author Mincong HUANG
 */
@Named
public class AfterIndexDecider implements Decider {

    @Inject @BatchProperty
    private Boolean optimizeAtEnd;
    
    /**
     * Decide the next step
     * 
     * @param executions not used for the moment.
     */
    @Override
    public String decide(StepExecution[] executions) throws Exception {
        return String.valueOf(optimizeAtEnd);
    }
}
