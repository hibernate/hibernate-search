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

import org.hibernate.criterion.Criterion;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jsr352.massindexing.impl.util.MassIndexerUtil;

/**
 * A utility class to start the Hibernate Search JSR-352 mass indexing job.
 * <p>
 * Use it like this:
 * <code><pre>
 * jobOperator.start(
 *		MassIndexingJob.NAME,
 *		MassIndexingJob.parameters()
 *			.forEntities( String.class, Integer.class )
 *			.fetchSize( 1000 )
 *			.rowsPerPartition( 10_000 )
 *			.maxResults( 1000 )
 *			.maxThreads( 30 )
 *			.purgeAtStart( true )
 *			.build()
 * );
 * </pre></code>
 *
 * @author Mincong Huang
 */
public final class MassIndexingJob {

	public static final String NAME = "hibernate-search-mass-indexing";

	private MassIndexingJob() {
		// Private constructor, do not use it.
	}

	public static ParametersBuilderInitialStep parameters() {
		return ParametersBuilderInitialStep.INSTANCE;
	}

	public static class ParametersBuilderInitialStep {
		private static final ParametersBuilderInitialStep INSTANCE = new ParametersBuilderInitialStep();

		private ParametersBuilderInitialStep() {
			// Private constructor, do not use it.
		}

		public ParametersBuilder forEntity(Class<?> rootEntity) {
			return new ParametersBuilder( rootEntity );
		}

		public ParametersBuilder forEntities(Class<?> rootEntity, Class<?>... rootEntities) {
			return new ParametersBuilder( rootEntity, rootEntities );
		}
	}

	public static class ParametersBuilder {

		private final Set<Class<?>> rootEntities;
		private String entityManagerFactoryScope;
		private String entityManagerFactoryReference;
		private boolean cacheable = false;
		private boolean optimizeAfterPurge = false;
		private boolean optimizeOnFinish = false;
		private boolean purgeAllOnStart = false;
		private int fetchSize = 200 * 1000;
		private int itemCount = 200;
		private int maxResults = 1000 * 1000;
		private int rowsPerPartition = 250;
		private int maxThreads = 1;
		private Set<Criterion> criteria;
		private String hql;

		private ParametersBuilder(Class<?> rootEntity, Class<?>... rootEntities) {
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

		public ParametersBuilder entityManagerFactoryScope(String scope) {
			this.entityManagerFactoryScope = scope;
			return this;
		}

		public ParametersBuilder entityManagerFactoryReference(String reference) {
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
		public ParametersBuilder cacheable(boolean cacheable) {
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
		public ParametersBuilder checkpointFreq(int itemCount) {
			this.itemCount = itemCount;
			return this;
		}

		/**
		 * The fetch size for the result fetching.
		 *
		 * @param fetchSize
		 * @return
		 */
		public ParametersBuilder fetchSize(int fetchSize) {
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
		public ParametersBuilder maxResults(int maxResults) {
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
		public ParametersBuilder maxThreads(int maxThreads) {
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
		public ParametersBuilder optimizeAfterPurge(boolean optimizeAfterPurge) {
			this.optimizeAfterPurge = optimizeAfterPurge;
			return this;
		}

		/**
		 * Specify whether the mass indexer should be optimized at the end of the job. This operation takes place after
		 * the step of lucene document production. The default value is false. TODO: specify what is the optimization
		 * exactly
		 *
		 * @param optimizeOnFinish
		 * @return
		 */
		public ParametersBuilder optimizeOnFinish(boolean optimizeOnFinish) {
			this.optimizeOnFinish = optimizeOnFinish;
			return this;
		}

		/**
		 * Specify whether the existing lucene index should be purged at the beginning of the job. This operation takes
		 * place before the step of lucene document production. The default value is false.
		 *
		 * @param purgeAllOnStart
		 * @return
		 */
		public ParametersBuilder purgeAllOnStart(boolean purgeAllOnStart) {
			this.purgeAllOnStart = purgeAllOnStart;
			return this;
		}

		/**
		 * Add criterion to choose the set of entities to index.
		 *
		 * @param criterion
		 * @return
		 */
		public ParametersBuilder restrictedBy(Criterion criterion) {
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
		public ParametersBuilder restrictedBy(String hql) {
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
		public ParametersBuilder rowsPerPartition(int rowsPerPartition) {
			if ( rowsPerPartition < 1 ) {
				throw new IllegalArgumentException(
						"rowsPerPartition must be at least 1" );
			}
			this.rowsPerPartition = rowsPerPartition;
			return this;
		}

		/**
		 * Build the parameters.
		 *
		 * @return The parameters.
		 * @throws SearchException if the serialization of some parameters fail.
		 */
		public Properties build() {
			Properties jobParams = new Properties();

			if ( entityManagerFactoryScope != null ) {
				jobParams.put( MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_SCOPE, entityManagerFactoryScope );
			}
			if ( entityManagerFactoryReference != null ) {
				jobParams.put( MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE, entityManagerFactoryReference );
			}
			jobParams.put( MassIndexingJobParameters.CACHEABLE, String.valueOf( cacheable ) );
			jobParams.put( MassIndexingJobParameters.FETCH_SIZE, String.valueOf( fetchSize ) );
			jobParams.put( MassIndexingJobParameters.HQL, hql );
			jobParams.put( MassIndexingJobParameters.ITEM_COUNT, String.valueOf( itemCount ) );
			jobParams.put( MassIndexingJobParameters.MAX_RESULTS, String.valueOf( maxResults ) );
			jobParams.put( MassIndexingJobParameters.MAX_THREADS, String.valueOf( maxThreads ) );
			jobParams.put( MassIndexingJobParameters.OPTIMIZE_AFTER_PURGE, String.valueOf( optimizeAfterPurge ) );
			jobParams.put( MassIndexingJobParameters.OPTIMIZE_ON_FINISH, String.valueOf( optimizeOnFinish ) );
			jobParams.put( MassIndexingJobParameters.PURGE_ALL_ON_START, String.valueOf( purgeAllOnStart ) );
			jobParams.put( MassIndexingJobParameters.ROOT_ENTITIES, getRootEntitiesAsString() );
			jobParams.put( MassIndexingJobParameters.ROWS_PER_PARTITION, String.valueOf( rowsPerPartition ) );

			if ( !criteria.isEmpty() ) {
				try {
					jobParams.put( MassIndexingJobParameters.CRITERIA, MassIndexerUtil.serializeCriteria( criteria ) );
				}
				catch (IOException e) {
					throw new SearchException( "Failed to serialize Criteria", e );
				}
			}

			return jobParams;
		}

		private String getRootEntitiesAsString() {
			return rootEntities.stream()
					.map( (e) -> e.getName() )
					.collect( Collectors.joining( "," ) );
		}
	}
}
