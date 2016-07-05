package org.hibernate.search.jsr352.internal;

import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;

/**
 * Lucene partition mapper provides a partition plan to the Lucene production
 * step: "produceLuceneDoc". The partition plan is defined dynamically,
 * according to the indexing context.
 * <p>
 * Several batch properties are used in this mapper:
 * <ul>
 * <li><b>partitionCapacity</b> defines the capacity of one partition: the
 * number of id arrays that will be treated in this partition. So the number of
 * partition is computed by the equation: <br>
 * {@code nbPartition = nbArray / partitionCapacity;}
 *
 * <li><b>threads</b> defines the number of threads wished by the user. Default
 * value is defined in the job xml file. However, the valued used might be
 * smaller, depending on the number of partitions.
 * </ul>
 *
 * @author Mincong HUANG
 */
@Named
public class LucenePartitionMapper implements PartitionMapper {

	private static final Logger logger = Logger.getLogger(LucenePartitionMapper.class);

	private final JobContext jobContext;
    private final IndexingContext indexingContext;

    @Inject @BatchProperty private int partitionCapacity;
    @Inject @BatchProperty private int threads;

    @Inject
    public LucenePartitionMapper(JobContext jobContext, IndexingContext indexingContext) {
		this.jobContext = jobContext;
		this.indexingContext = indexingContext;
	}

	@Override
    public PartitionPlan mapPartitions() throws Exception {

        Set<Class<?>> rootEntities = ( ( BatchContextData )jobContext.getTransientUserData() ).getEntityTypesToIndex();
        Queue<String> classQueue = new LinkedList<>();

        int totalPartitions = 0;
        for (Class<?> rootEntity: rootEntities) {

            int _queueSize = indexingContext.sizeOf(rootEntity);
            int _partitions = (int) Math.ceil((double) _queueSize / partitionCapacity);

            logger.infof("rootEntity=%s", rootEntity.toString());
            logger.infof("_queueSize=%d", _queueSize);
            logger.infof("partitionCapacity=%d", partitionCapacity);
            logger.infof("_partitions=%d", _partitions);

            // enqueue entity type into classQueue, as much as the number of
            // the class partitions
            for (int i = 0; i < _partitions; i++) {
                classQueue.add(rootEntity.getName());
            }
            logger.infof("%d partitions added to root entity \"%s\".",
                    _partitions, rootEntity);

            totalPartitions += _partitions;
        }
        final int TOTAL_PARTITIONS = totalPartitions;

        return new PartitionPlanImpl() {

            @Override
            public int getPartitions() {
                logger.infof("#mapPartitions(): %d partitions.", TOTAL_PARTITIONS);
                return TOTAL_PARTITIONS;
            }

            @Override
            public int getThreads() {
                logger.infof("#getThreads(): %d threads.", TOTAL_PARTITIONS);//Math.min(TOTAL_PARTITIONS, threads));
                return Math.min(TOTAL_PARTITIONS, threads);
            }

            @Override
            public Properties[] getPartitionProperties() {
                Properties[] props = new Properties[TOTAL_PARTITIONS];
                for (int i = 0; i < props.length; i++) {
                    String entityType = classQueue.poll();
                    props[i] = new Properties();
                    props[i].setProperty("entityType", entityType);
                }
                return props;
            }
        };
    }
}
