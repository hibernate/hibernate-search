package org.hibernate.search.jsr352.internal;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

/**
 * Specific indexing context for mass indexer. Several attributes are used :
 * <p>
 * <ul>
 * <li>entityCount: the total number of entities to be indexed in the job. The
 *      number is summarized by partitioned step "loadId". Each
 *      IdProducerBatchlet (partiton) produces the number of entities linked to
 *      its own target entity, then call the method #addEntityCount(long) to
 *      summarize it with other partition(s).</li>
 * </ul>
 * @author Mincong HUANG
 */
@Named
@Singleton
public class IndexingContext {

    private ConcurrentHashMap<Class<?>, ConcurrentLinkedQueue<Serializable[]>> idQueues;
    private long entityCount = 0;
    private EntityManager entityManager;

    private static final Logger logger = Logger.getLogger(IndexingContext.class);

    public void add(Serializable[] clazzIDs, Class<?> clazz) {
        idQueues.get(clazz).add(clazzIDs);
    }

    public Serializable[] poll(Class<?> clazz) {
        // TODO: this method is really slow
        Serializable[] IDs = idQueues.get(clazz).poll();
        String len = (IDs == null) ? "null" : String.valueOf(IDs.length);
        logger.infof("Polling %s IDs for %s", len, clazz.getName());
        return IDs;
    }

    public int sizeOf(Class<?> clazz) {
        return idQueues.get(clazz).size();
    }

    public void createQueue(Class<?> clazz) {
        idQueues.put(clazz, new ConcurrentLinkedQueue<>());
    }

    public IndexingContext() {
        this.idQueues = new ConcurrentHashMap<>();
    }

    public ConcurrentHashMap<Class<?>, ConcurrentLinkedQueue<Serializable[]>> getIdQueues() {
        return idQueues;
    }

    // I don't think we need this method.
    public void setIdQueues(ConcurrentHashMap<Class<?>, ConcurrentLinkedQueue<Serializable[]>> idQueues) {
        this.idQueues = idQueues;
    }

    public synchronized void addEntityCount(long entityCount) {
        this.entityCount += entityCount;
    }

    public long getEntityCount() {
        return entityCount;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
}
