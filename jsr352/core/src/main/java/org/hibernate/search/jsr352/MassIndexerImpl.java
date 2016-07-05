package org.hibernate.search.jsr352;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManager;

import org.hibernate.search.jsr352.internal.IndexingContext;

public class MassIndexerImpl implements MassIndexer {

    private boolean optimizeAfterPurge = false;
    private boolean optimizeAtEnd = false;
    private boolean purgeAtStart = false;
    private int arrayCapacity = 1000;
    private int fetchSize = 200 * 1000;
    private int maxResults = 1000 * 1000;
    private int partitionCapacity = 250;
    private int partitions = 1;
    private int threads = 1;
    private final Set<Class<?>> rootEntities = new HashSet<>();
    private EntityManager entityManager;
    private JobOperator jobOperator;

    private final String JOB_NAME = "mass-index";

    public MassIndexerImpl() {

    }

    /**
     * Mass index the Address entity's.
     * <p>Here're an example with parameters and expected results:
     * <ul>
     * <li><b>array capacity</b> = 500
     *
     * <li><b>partition capacity</b> = 250
     *
     * <li><b>max results</b> = 200 * 1000
     *
     * <li><b>queue size</b>
     *      = Math.ceil(max results / array capacity)
     *      = Math.ceil(200 * 1000 / 500)
     *      = Math.ceil(400)
     *      = 400
     *
     * <li><b>number of partitions</b>
     *      = Math.ceil(queue size / partition capacity)
     *      = Math.ceil(400 / 250)
     *      = Math.ceil(1.6)
     *      = 2
     *
     * </ul>
     */
    @Override
    public long start() {
        registrerEntityManager(entityManager);

        Properties jobParams = new Properties();
        jobParams.setProperty("fetchSize", String.valueOf(fetchSize));
        jobParams.setProperty("arrayCapacity", String.valueOf(arrayCapacity));
        jobParams.setProperty("maxResults", String.valueOf(maxResults));
        jobParams.setProperty("partitionCapacity", String.valueOf(partitionCapacity));
        jobParams.setProperty("partitions", String.valueOf(partitions));
        jobParams.setProperty("threads", String.valueOf(threads));
        jobParams.setProperty("purgeAtStart", String.valueOf(purgeAtStart));
        jobParams.setProperty("optimizeAfterPurge", String.valueOf(optimizeAfterPurge));
        jobParams.setProperty("optimizeAtEnd", String.valueOf(optimizeAtEnd));
        jobParams.put( "rootEntities", getEntitiesToIndexAsString() );
//      JobOperator jobOperator = BatchRuntime.getJobOperator();
        Long executionId = jobOperator.start(JOB_NAME, jobParams);
        return executionId;
    }

    @Override
    public void stop(long executionId) {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        jobOperator.stop(executionId);
    }

    @Override
    public MassIndexer arrayCapacity(int arrayCapacity) {
        if (arrayCapacity < 1) {
            throw new IllegalArgumentException("arrayCapacity must be at least 1");
        }
        this.arrayCapacity = arrayCapacity;
        return this;
    }

    @Override
    public MassIndexer fetchSize(int fetchSize) {
        if (fetchSize < 1) {
            throw new IllegalArgumentException("fetchSize must be at least 1");
        }
        this.fetchSize = fetchSize;
        return this;
    }

    @Override
    public MassIndexer maxResults(int maxResults) {
        if (maxResults < 1) {
            throw new IllegalArgumentException("maxResults must be at least 1");
        }
        this.maxResults = maxResults;
        return this;
    }

    @Override
    public MassIndexer optimizeAfterPurge(boolean optimizeAfterPurge) {
        this.optimizeAfterPurge = optimizeAfterPurge;
        return this;
    }

    @Override
    public MassIndexer optimizeAtEnd(boolean optimizeAtEnd) {
        this.optimizeAtEnd = optimizeAtEnd;
        return this;
    }

    @Override
    public MassIndexer partitionCapacity(int partitionCapacity) {
        if (partitionCapacity < 1) {
            throw new IllegalArgumentException("partitionCapacity must be at least 1");
        }
        this.partitionCapacity = partitionCapacity;
        return this;
    }

    @Override
    public MassIndexer partitions(int partitions) {
        if (partitions < 1) {
            throw new IllegalArgumentException("partitions must be at least 1");
        }
        this.partitions = partitions;
        return this;
    }

    @Override
    public MassIndexer purgeAtStart(boolean purgeAtStart) {
        this.purgeAtStart = purgeAtStart;
        return this;
    }

    @Override
    public MassIndexer threads(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1.");
        }
        this.threads = threads;
        return this;
    }

    @Override
    public MassIndexer addRootEntities(Set<Class<?>> rootEntities) {
        if (rootEntities == null) {
            throw new NullPointerException("rootEntities cannot be NULL.");
        } else if (rootEntities.isEmpty()) {
            throw new NullPointerException("rootEntities must have at least 1 element.");
        }
        this.rootEntities.addAll(rootEntities);
        return this;
    }

    @Override
    public MassIndexer addRootEntities(Class<?>... rootEntities) {
        this.rootEntities.addAll(Arrays.asList(rootEntities));
        return this;
    }

    @Override
    public MassIndexer entityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        return this;
    }

    @Override
    public MassIndexer jobOperator(JobOperator jobOperator) {
        this.jobOperator = jobOperator;
        return this;
    }

    @Override
    public boolean isOptimizeAfterPurge() {
        return optimizeAfterPurge;
    }

    @Override
	public boolean isOptimizeAtEnd() {
        return optimizeAtEnd;
    }

    @Override
	public boolean isPurgeAtStart() {
        return purgeAtStart;
    }

    @Override
	public int getArrayCapacity() {
        return arrayCapacity;
    }

    @Override
	public int getFetchSize() {
        return fetchSize;
    }

    @Override
	public int getMaxResults() {
        return maxResults;
    }

    @Override
	public int getPartitionCapacity() {
        return partitionCapacity;
    }

    @Override
	public int getPartitions() {
        return partitions;
    }

    @Override
	public int getThreads() {
        return threads;
    }

    public String getJOB_NAME() {
        return JOB_NAME;
    }

    @Override
	public Set<Class<?>> getRootEntities() {
        return rootEntities;
    }

	private String getEntitiesToIndexAsString() {
		return rootEntities.stream()
			.map( (e) -> e.getName() )
			.collect( Collectors.joining( ", " ) );
	}

    @SuppressWarnings("unchecked")
    private void registrerEntityManager(EntityManager entityManager) {
        BeanManager bm = CDI.current().getBeanManager();
        Bean<IndexingContext> bean = (Bean<IndexingContext>) bm
            .resolve(bm.getBeans(IndexingContext.class));
        IndexingContext indexingContext = bm
                .getContext(bean.getScope())
                .get(bean, bm.createCreationalContext(bean));
        indexingContext.setEntityManager(entityManager);
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public JobOperator getJobOperator() {
        return jobOperator;
    }
}
