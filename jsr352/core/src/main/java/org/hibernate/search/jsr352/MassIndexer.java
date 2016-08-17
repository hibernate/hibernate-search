/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.jsr352.internal.se.JobSEEnvironment;

/**
 * An alternative interface to the current mass indexer, using the Java Batch
 * architecture as defined by JSR 352.
 *
 * @author Mincong Huang
 */
public class MassIndexer {

	private static final long NO_PREV_JOB_EXEC = 0L;
	private static final String JOB_NAME = "mass-index";
	private final Set<Class<?>> rootEntities = new HashSet<>();

	private boolean cacheable = false;
	private boolean optimizeAfterPurge = false;
	private boolean optimizeAtEnd = false;
	private boolean purgeAtStart = false;
	private boolean isJavaSE = false;
	private int fetchSize = 200 * 1000;
	private int itemCount = 3;
	private int maxResults = 1000 * 1000;
	private int rowsPerPartition = 250;
	private int maxThreads = 1;
	private EntityManagerFactory emf;
	private JobOperator jobOperator;
	private long executionId = NO_PREV_JOB_EXEC;

	/**
	 * Start the job.
	 *
	 * @return
	 */
	public long start() {

		// check different variables
		if ( rootEntities == null ) {
			throw new NullPointerException( "rootEntities cannot be null" );
		}
		if ( isJavaSE ) {
			if ( emf == null ) {
				throw new NullPointerException( "You're under a Java SE environment. "
						+ "Please assign the EntityManagerFactory before the job start." );
			}
			if ( jobOperator == null ) {
				throw new NullPointerException( "You're under a Java SE environment. "
						+ "Please assign the jobOperator before the job start." );
			}
			JobSEEnvironment.setEntityManagerFactory( emf );
		}
		else {
			if ( emf != null ) {
				throw new IllegalStateException( "You're under a Java EE environmant. "
						+ "Please do not assign the EntityManagerFactory. "
						+ "If you're under Java SE, set isJavaSE( true );");
			}
			jobOperator = BatchRuntime.getJobOperator();
		}

		Properties jobParams = new Properties();
		jobParams.put( "cacheable", String.valueOf( cacheable ) );
		jobParams.put( "fetchSize", String.valueOf( fetchSize ) );
		jobParams.put( "isJavaSE", String.valueOf( isJavaSE ) );
		jobParams.put( "itemCount", String.valueOf( itemCount ) );
		jobParams.put( "maxResults", String.valueOf( maxResults ) );
		jobParams.put( "maxThreads", String.valueOf( maxThreads ) );
		jobParams.put( "optimizeAfterPurge", String.valueOf( optimizeAfterPurge ) );
		jobParams.put( "optimizeAtEnd", String.valueOf( optimizeAtEnd ) );
		jobParams.put( "purgeAtStart", String.valueOf( purgeAtStart ) );
		jobParams.put( "rootEntities", getRootEntitiesAsString() );
		jobParams.put( "rowsPerPartition", String.valueOf( rowsPerPartition ) );
		executionId = jobOperator.start( JOB_NAME, jobParams );
		return executionId;
	}

	public long restart() {
		if ( executionId == NO_PREV_JOB_EXEC ) {
			throw new IllegalStateException( "No previous job execution." );
		}
		if ( isJavaSE ) {
			if ( emf == null ) {
				throw new NullPointerException( "You're under a Java SE environment. "
						+ "Please assign the EntityManagerFactory before the job start." );
			}
			if ( jobOperator == null ) {
				throw new NullPointerException( "You're under a Java SE environment. "
						+ "Please assign the jobOperator before the job start." );
			}
			JobSEEnvironment.setEntityManagerFactory( emf );
		}
		else {
			if ( emf != null ) {
				throw new IllegalStateException( "You're under a Java EE environmant. "
						+ "Please do not assign the EntityManagerFactory. "
						+ "If you're under Java SE, set isJavaSE( true );");
			}
			jobOperator = BatchRuntime.getJobOperator();
		}
		executionId = jobOperator.restart( executionId, null );
		return executionId;
	}

	/**
	 * Stop the job.
	 *
	 * @param executionId
	 */
	public void stop(long executionId) {
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		jobOperator.stop( executionId );
	}

	/**
	 * Add entity type to index.
	 *
	 * @param rootEntitiy
	 * @return
	 */
	public MassIndexer addRootEntity(Class<?> rootEntity) {
		if ( rootEntity == null ) {
			throw new NullPointerException( "rootEntity cannot be NULL." );
		}
		this.rootEntities.add( rootEntity );
		return this;
	}

	/**
	 * Add entity types to index. Currently, only root entities are accepted
	 * because the lack of entity types retrieve inside the job.
	 *
	 * @param rootEntities
	 * @return
	 */
	public MassIndexer addRootEntities(Class<?>... rootEntities) {
		if ( rootEntities == null ) {
			throw new NullPointerException( "rootEntities cannot be NULL." );
		}
		else if ( rootEntities.length == 0 ) {
			throw new IllegalStateException(
					"rootEntities must have at least 1 element." );
		}
		this.rootEntities.addAll( Arrays.asList( rootEntities ) );
		return this;
	}

	/**
	 * Checkpoint frequency during the mass index process. The checkpoint will
	 * be done every N items read, where N is the given item count.
	 *
	 * @param itemCount the number of item count before starting the next
	 * checkpoint.
	 * @return
	 */
	public MassIndexer checkpointFreq(int itemCount) {
		this.itemCount = itemCount;
		return this;
	}

	/**
	 * Whether the Hibernate queries are cacheable. This setting will be applied
	 * to all the queries. The default value is false.
	 *
	 * @param cacheable
	 * @return
	 */
	public MassIndexer cacheable(boolean cacheable) {
		this.cacheable = cacheable;
		return this;
	}

	/**
	 * Assign the entity manager factory. You must use this method if you're
	 * under Java SE. You should NOT use it if you're under Java EE.
	 *
	 * @param entityManagerFactory
	 * @return
	 */
	public MassIndexer entityManagerFactory(EntityManagerFactory entityManagerFactory) {
		if ( entityManagerFactory == null ) {
			throw new NullPointerException( "The entityManagerFactory cannot be null." );
		}
		else if ( !entityManagerFactory.isOpen() ) {
			throw new IllegalStateException( "Please provide an open entityManagerFactory." );
		}
		this.emf = entityManagerFactory;
		return this;
	}

	/**
	 * The fetch size for the result fetching.
	 *
	 * @param fetchSize
	 * @return
	 */
	public MassIndexer fetchSize(int fetchSize) {
		if ( fetchSize < 1 ) {
			throw new IllegalArgumentException( "fetchSize must be at least 1" );
		}
		this.fetchSize = fetchSize;
		return this;
	}

	/**
	 * Whether the a Java SE environment. Default is false.
	 *
	 * @param isJavaSE
	 * @return
	 */
	public MassIndexer isJavaSE(boolean isJavaSE) {
		this.isJavaSE = isJavaSE;
		return this;
	}

	/**
	 * Job operator to start the batch job. You must call this method if you're
	 * under Java SE. Else, this method is not necessary.
	 *
	 * @param jobOperator
	 * @return
	 */
	public MassIndexer jobOperator(JobOperator jobOperatorInJavaSE) {
		this.jobOperator = jobOperatorInJavaSE;
		return this;
	}

	/**
	 * The maximum number of results will be return from the HQL / criteria. It
	 * is equivalent to keyword `LIMIT` in SQL.
	 *
	 * @param maxResults
	 * @return
	 */
	public MassIndexer maxResults(int maxResults) {
		if ( maxResults < 1 ) {
			throw new IllegalArgumentException( "maxResults must be at least 1" );
		}
		this.maxResults = maxResults;
		return this;
	}

	/**
	 * Specify the maximum number of threads on which to execute the partitions
	 * of this step. Note the batch runtime cannot guarantee the request number
	 * of threads are available; it will use as many as it can up to the request
	 * maximum. This an an optional attribute. The default is the number of
	 * partitions.
	 *
	 * @param maxThreads
	 * @return
	 */
	public MassIndexer maxThreads(int maxThreads) {
		if ( maxThreads < 1 ) {
			throw new IllegalArgumentException( "threads must be at least 1." );
		}
		this.maxThreads = maxThreads;
		return this;
	}

	/**
	 * Specify whether the mass indexer should be optimized at the beginning of
	 * the job. This operation takes place after the purge operation and before
	 * the step of lucene document production. The default value is false. TODO:
	 * specify what is the optimization exactly
	 *
	 * @param optimizeAfterPurge
	 * @return
	 */
	public MassIndexer optimizeAfterPurge(boolean optimizeAfterPurge) {
		this.optimizeAfterPurge = optimizeAfterPurge;
		return this;
	}

	/**
	 * Specify whether the mass indexer should be optimized at the end of the
	 * job. This operation takes place after the step of lucene document
	 * production. The default value is false. TODO: specify what is the
	 * optimization exactly
	 *
	 * @param optimizeAtEnd
	 * @return
	 */
	public MassIndexer optimizeAtEnd(boolean optimizeAtEnd) {
		this.optimizeAtEnd = optimizeAtEnd;
		return this;
	}

	/**
	 * Specify whether the existing lucene index should be purged at the
	 * beginning of the job. This operation takes place before the step of
	 * lucene document production. The default value is false.
	 *
	 * @param purgeAtStart
	 * @return
	 */
	public MassIndexer purgeAtStart(boolean purgeAtStart) {
		this.purgeAtStart = purgeAtStart;
		return this;
	}

	/**
	 * Define the max number of rows to process per partition.
	 *
	 * @param partitionCapacity
	 * @return
	 */
	public MassIndexer rowsPerPartition(int rowsPerPartition) {
		if ( rowsPerPartition < 1 ) {
			throw new IllegalArgumentException(
					"rowsPerPartition must be at least 1" );
		}
		this.rowsPerPartition = rowsPerPartition;
		return this;
	}

	private String getRootEntitiesAsString() {
		return rootEntities.stream()
				.map( (e) -> e.getName() )
				.collect( Collectors.joining( "," ) );
	}
}
