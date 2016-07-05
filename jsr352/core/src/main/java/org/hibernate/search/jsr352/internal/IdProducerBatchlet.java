package org.hibernate.search.jsr352.internal;

import java.io.Serializable;
import java.util.Arrays;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.jboss.logging.Logger;

/**
 * Read identifiers of entities via entity manager. The result is going to be
 * stored in {@code IndexingContext}, then be used for Lucene document
 * production in the next step.
 *
 * @author Mincong HUANG
 */
@Named
public class IdProducerBatchlet implements Batchlet {

	private static final Logger logger = Logger.getLogger(IdProducerBatchlet.class);

	private final JobContext jobContext;
    private final IndexingContext indexingContext;

    @Inject @BatchProperty private int arrayCapacity;
    @Inject @BatchProperty private int fetchSize;
    @Inject @BatchProperty private int maxResults;
    @Inject @BatchProperty private String entityType;

    private EntityManager em;
    private Session session;

    @Inject
    public IdProducerBatchlet(JobContext jobContext, IndexingContext indexingContext) {
		this.jobContext = jobContext;
		this.indexingContext = indexingContext;
	}

	/**
     * Load id of all target entities using Hibernate Session. In order to
     * follow the id loading progress, the total number will be additionally
     * computed as well.
     */
    @Override
    public String process() throws Exception {

        // get entity class type
        Class<?> entityClazz = ( (BatchContextData) jobContext.getTransientUserData() ).getIndexedType( entityType );

        if (em == null) {
            em = indexingContext.getEntityManager();
        }
        // unwrap session from entity manager
        session = em.unwrap(Session.class);

        // get total number of id
        final long rowCount = (long) session
            .createCriteria(entityClazz)
            .setProjection(Projections.rowCount())
            .setCacheable(false)
            .uniqueResult();
        logger.infof("entityType = %s (%d rows).", entityType, rowCount);
        indexingContext.addEntityCount(rowCount);

        // load ids and store in scrollable results
        ScrollableResults scrollableIds = session
            .createCriteria(entityClazz)
            .setCacheable(false)
            .setFetchSize(fetchSize)
            .setProjection(Projections.id())
            .setMaxResults(maxResults)
            .scroll(ScrollMode.FORWARD_ONLY);

        Serializable[] entityIDs = new Serializable[arrayCapacity];
        long rowLoaded = 0;
        int i = 0;
        try {
            // Create a key-value pair for entity in the hash-map embedded in
            // indexingContext. The key is the entity class type and the value
            // is an empty queue of IDs.
            indexingContext.createQueue(entityClazz);

            while (scrollableIds.next() && rowLoaded < rowCount) {
                Serializable id = (Serializable) scrollableIds.get(0);
                entityIDs[i++] = id;
                rowLoaded++;
                if (i == arrayCapacity) {
                    // add array entityIDs into indexing context's hash-map,
                    // mapped to key K = entityClazz
                    indexingContext.add(entityIDs, entityClazz);
                    // reset id array and index
                    entityIDs = new Serializable[arrayCapacity];
                    i = 0;
                } else if (scrollableIds.isLast()) {
                    indexingContext.add(Arrays.copyOf(entityIDs, i), entityClazz);
                }
            }
        } finally {
            scrollableIds.close();
        }
        return BatchStatus.COMPLETED.toString();
    }

    @Override
    public void stop() throws Exception {
        if (session.isOpen()) {
            session.close();
        }
    }
}
