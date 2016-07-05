package org.hibernate.search.jsr352.internal;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

import javax.batch.api.chunk.ItemReader;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;

/**
 * Read entity IDs from {@code IndexingContext}. Each time, there's one array
 * being read. The number of IDs inside the array depends on the array capacity.
 * This value is defined before the job start. Either the default value defined
 * in the job xml will be applied, or the value overwritten by the user in job
 * parameters. These IDs will be processed in {@code BatchItemProcessor}, then
 * be used for Lucene document production.
 * <p>
 * The motivation of using an array of IDs over a single ID is to accelerate
 * the entity processing. Use a SELECT statement to obtain only one ID is
 * rather a waste. For more detail about the entity process, please check {@code
 * BatchItemProcessor}.
 *
 * @author Mincong HUANG
 */
@Named
public class BatchItemReader implements ItemReader {

    @Inject
    private IndexingContext indexingContext;

    // TODO: I think this can be done with StepContext
    private boolean isRestarted;
    private boolean hasReadTempIDs;

    // TODO: this array should be defined dynamically by the item-count value
    // defined by the batch job. But for instance, just use a static value
    private Queue<Serializable[]> tempIDs;

    private static final Logger logger = Logger.getLogger(BatchItemReader.class);

    private final StepContext stepContext;

	@Inject
	public BatchItemReader(StepContext stepContext) {
		this.stepContext = stepContext;
	}

	/**
     * The checkpointInfo method returns the current checkpoint data for this
     * reader. It is called before a chunk checkpoint is committed.
     *
     * @return the checkpoint info
     * @throws Exception thrown for any errors.
     */
    @Override
    public Serializable checkpointInfo() throws Exception {
        logger.info("checkpointInfo() called. Saving temporary IDs to batch runtime...");
        Queue<Serializable[]> checkpoint = new LinkedList<>(tempIDs);
        tempIDs.clear();
        return (Serializable) checkpoint;
    }

    /**
     * Close operation(s) before the class destruction.
     *
     * @throws Exception thrown for any errors.
     */
    @Override
    public void close() throws Exception {
        logger.info("close");
    }

    /**
     * Initialize the environment. If checkpoint does not exist, then it should
     * be the first open. If checkpoint exist, then it isn't the first open,
     * save the input object "checkpoint" into "tempIDs".
     *
     * @param checkpoint The last checkpoint info saved in the batch runtime,
     *          previously given by checkpointInfo().
     * @throws Exception thrown for any errors.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void open(Serializable checkpoint) throws Exception {
        logger.infof( "#open(...)" );
        if (checkpoint == null) {
            tempIDs = new LinkedList<>();
            isRestarted = false;
        } else {
            tempIDs = (Queue<Serializable[]>) checkpoint;
            isRestarted = true;
        }
    }

    /**
     * Read item from the {@code IndexingContext}. Here, item means an array of
     * IDs previously produced by the {@code IdProducerBatchlet}.
     *
     * If this is a restart job, then the temporary IDs restored from checkpoint
     * will be read at first.
     *
     * @throws Exception thrown for any errors.
     */
    @Override
    public Object readItem() throws Exception {
        Serializable[] IDs = null;
        if (isRestarted && !hasReadTempIDs && !tempIDs.isEmpty()) {
            IDs = tempIDs.poll();
            hasReadTempIDs = tempIDs.isEmpty();
        } else {
            IDs = indexingContext.poll( ( (EntityIndexingStepData) stepContext.getTransientUserData() ).getEntityClass() );
            tempIDs.add(IDs);
        }
        return IDs;
    }
}
