/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;

import org.hibernate.criterion.Criterion;
import org.hibernate.search.jsr352.massindexing.impl.util.MassIndexerUtil;

/**
 * An alternative to the current mass indexer, using the Java Batch architecture as defined by JSR 352.
 *
 * @author Mincong Huang
 */
public class BatchIndexingJob {

	public static final String JOB_NAME = "BatchIndexingJob";

	public static Builder forEntity(Class<?> rootEntity) {
		return new Builder( rootEntity );
	}

	public static Builder forEntities(Class<?> rootEntity, Class<?>... rootEntities) {
		return new Builder( rootEntity, rootEntities );
	}

	/**
	 * This method should only be used in Java EE.
	 *
	 * @param executionId
	 * @return
	 */
	public static long restart(long executionId) {
		return BatchRuntime.getJobOperator().restart( executionId, null );
	}

	/**
	 * This method should only be used in Java SE.
	 *
	 * @param executionId
	 * @param entityManagerFactorySE
	 * @param jobOperatorSE
	 * @return
	 */
	public static long restart(long executionId, JobOperator jobOperatorSE) {
		if ( jobOperatorSE == null ) {
			throw new NullPointerException( "You're under a Java SE environment. "
					+ "Please assign the jobOperator before the job start." );
		}
		return jobOperatorSE.restart( executionId, null );
	}

	public static class Builder {

		private final Set<Class<?>> rootEntities;
		private String entityManagerFactoryScope;
		private String entityManagerFactoryReference;
		private boolean cacheable = false;
		private boolean optimizeAfterPurge = false;
		private boolean optimizeAtEnd = false;
		private boolean purgeAtStart = false;
		private int fetchSize = 200 * 1000;
		private int itemCount = 200;
		private int maxResults = 1000 * 1000;
		private int rowsPerPartition = 250;
		private int maxThreads = 1;
		private JobOperator jobOperator;
		private Set<Criterion> criteria;
		private String hql;

		private Builder(Class<?> rootEntity, Class<?>... rootEntities) {
			if ( rootEntity == null ) {
				throw new IllegalArgumentException( "rootEntities must have at least 1 element." );
			}
			this.rootEntities = new HashSet<>();
			this.rootEntities.add( rootEntity );
			for ( Class<?> clz : rootEntities ) {
				this.rootEntities.add( clz );
			}
			criteria = new HashSet<>();
			hql = "";
		}

		public Builder entityManagerFactoryScope(String scope) {
			this.entityManagerFactoryScope = scope;
			return this;
		}

		public Builder entityManagerFactoryReference(String reference) {
			this.entityManagerFactoryReference = reference;
			return this;
		}

		/**
		 * Whether the Hibernate queries are cacheable. This setting will be applied to
		 * {@link org.hibernate.search.jsr352.massindexing.impl.steps.lucene.EntityReader} . The default value is false. Set it
		 * to true when reading a complex graph with relations.
		 *
		 * @param cacheable
		 * @return
		 */
		public Builder cacheable(boolean cacheable) {
			this.cacheable = cacheable;
			return this;
		}

		/**
		 * Checkpoint frequency during the mass index process. The checkpoint will be done every N items read, where N
		 * is the given item count.
		 *
		 * @param itemCount the number of item count before starting the next checkpoint.
		 * @return
		 */
		public Builder checkpointFreq(int itemCount) {
			this.itemCount = itemCount;
			return this;
		}

		/**
		 * Configure additional parameters for Java SE: assign the job operator.
		 * You should NOT use this method if you're under Java EE.
		 */
		public Builder underJavaSE(JobOperator jobOperator) {
			if ( jobOperator == null ) {
				throw new NullPointerException( "The jobOperator cannot be null." );
			}
			this.jobOperator = jobOperator;
			return this;
		}

		/**
		 * The fetch size for the result fetching.
		 *
		 * @param fetchSize
		 * @return
		 */
		public Builder fetchSize(int fetchSize) {
			if ( fetchSize < 1 ) {
				throw new IllegalArgumentException( "fetchSize must be at least 1" );
			}
			this.fetchSize = fetchSize;
			return this;
		}

		/**
		 * The maximum number of results will be return from the HQL / criteria. It is equivalent to keyword `LIMIT` in
		 * SQL.
		 *
		 * @param maxResults
		 * @return
		 */
		public Builder maxResults(int maxResults) {
			if ( maxResults < 1 ) {
				throw new IllegalArgumentException( "maxResults must be at least 1" );
			}
			this.maxResults = maxResults;
			return this;
		}

		/**
		 * Specify the maximum number of threads on which to execute the partitions of this step. Note the batch runtime
		 * cannot guarantee the request number of threads are available; it will use as many as it can up to the request
		 * maximum. This an an optional attribute. The default is the number of partitions.
		 *
		 * @param maxThreads
		 * @return
		 */
		public Builder maxThreads(int maxThreads) {
			if ( maxThreads < 1 ) {
				throw new IllegalArgumentException( "threads must be at least 1." );
			}
			this.maxThreads = maxThreads;
			return this;
		}

		/**
		 * Specify whether the mass indexer should be optimized at the beginning of the job. This operation takes place
		 * after the purge operation and before the step of lucene document production. The default value is false.
		 * TODO: specify what is the optimization exactly
		 *
		 * @param optimizeAfterPurge
		 * @return
		 */
		public Builder optimizeAfterPurge(boolean optimizeAfterPurge) {
			this.optimizeAfterPurge = optimizeAfterPurge;
			return this;
		}

		/**
		 * Specify whether the mass indexer should be optimized at the end of the job. This operation takes place after
		 * the step of lucene document production. The default value is false. TODO: specify what is the optimization
		 * exactly
		 *
		 * @param optimizeAtEnd
		 * @return
		 */
		public Builder optimizeAtEnd(boolean optimizeAtEnd) {
			this.optimizeAtEnd = optimizeAtEnd;
			return this;
		}

		/**
		 * Specify whether the existing lucene index should be purged at the beginning of the job. This operation takes
		 * place before the step of lucene document production. The default value is false.
		 *
		 * @param purgeAtStart
		 * @return
		 */
		public Builder purgeAtStart(boolean purgeAtStart) {
			this.purgeAtStart = purgeAtStart;
			return this;
		}

		/**
		 * Add criterion to choose the set of entities to index.
		 *
		 * @param criterion
		 * @return
		 */
		public Builder restrictedBy(Criterion criterion) {
			if ( !hql.isEmpty() ) {
				throw new IllegalArgumentException( "Cannot use HQL approach "
						+ "and Criteria approach in the same time." );
			}
			if ( criterion == null ) {
				throw new NullPointerException( "The criterion is null." );
			}
			criteria.add( criterion );
			return this;
		}

		/**
		 * Use HQL / JPQL to select to entities to index
		 *
		 * @param hql
		 * @return
		 */
		public Builder restrictedBy(String hql) {
			if ( hql == null ) {
				throw new NullPointerException( "The HQL is null." );
			}
			if ( criteria.size() > 0 ) {
				throw new IllegalArgumentException( "Cannot use HQL approach "
						+ "and Criteria approach in the same time." );
			}
			this.hql = hql;
			return this;
		}

		/**
		 * Define the max number of rows to process per partition.
		 *
		 * @param partitionCapacity
		 * @return
		 */
		public Builder rowsPerPartition(int rowsPerPartition) {
			if ( rowsPerPartition < 1 ) {
				throw new IllegalArgumentException(
						"rowsPerPartition must be at least 1" );
			}
			this.rowsPerPartition = rowsPerPartition;
			return this;
		}

		/**
		 * Start the job.
		 *
		 * @return
		 * @throws IOException if the serialization of JobContextData fails.
		 */
		public long start() throws IOException {

			Properties jobParams = new Properties();

			// check different variables
			if ( jobOperator == null ) {
				jobOperator = BatchRuntime.getJobOperator();
			}

			if ( entityManagerFactoryScope != null ) {
				jobParams.put( "entityManagerFactoryScope", entityManagerFactoryScope );
			}
			if ( entityManagerFactoryReference != null ) {
				jobParams.put( "entityManagerFactoryReference", entityManagerFactoryReference );
			}
			jobParams.put( "cacheable", String.valueOf( cacheable ) );
			jobParams.put( "fetchSize", String.valueOf( fetchSize ) );
			jobParams.put( "hql", hql );
			jobParams.put( "itemCount", String.valueOf( itemCount ) );
			jobParams.put( "maxResults", String.valueOf( maxResults ) );
			jobParams.put( "maxThreads", String.valueOf( maxThreads ) );
			jobParams.put( "optimizeAfterPurge", String.valueOf( optimizeAfterPurge ) );
			jobParams.put( "optimizeAtEnd", String.valueOf( optimizeAtEnd ) );
			jobParams.put( "purgeAtStart", String.valueOf( purgeAtStart ) );
			jobParams.put( "rootEntities", getRootEntitiesAsString() );
			jobParams.put( "rowsPerPartition", String.valueOf( rowsPerPartition ) );
			if ( !criteria.isEmpty() ) {
				jobParams.put( "criteria", MassIndexerUtil.serializeCriteria( criteria ) );
			}
			long executionId = jobOperator.start( JOB_NAME, jobParams );
			return executionId;
		}

		private String getRootEntitiesAsString() {
			return rootEntities.stream()
					.map( (e) -> e.getName() )
					.collect( Collectors.joining( "," ) );
		}
	}
}
