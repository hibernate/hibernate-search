/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import java.io.IOException;
import java.util.Collections;
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
 * 		MassIndexingJob.NAME,
 * 		MassIndexingJob.parameters()
 * 			.forEntities( String.class, Integer.class )
 * 			.fetchSize( 1000 )
 * 			.rowsPerPartition( 10_000 )
 * 			.maxResults( 1000 )
 * 			.maxThreads( 30 )
 * 			.purgeAtStart( true )
 * 			.build()
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

		/**
		 * The entity type to index in this job execution.
		 *
		 * @param rootEntity the type of entity to index
		 *
		 * @return itself
		 */
		public ParametersBuilder forEntity(Class<?> rootEntity) {
			return new ParametersBuilder( rootEntity );
		}

		/**
		 * The entity types to index in this job execution.
		 *
		 * @param rootEntity the first type of entity to index, must not be null
		 * @param rootEntities the remaining types of entity to index
		 *
		 * @return itself
		 */
		public ParametersBuilder forEntities(Class<?> rootEntity, Class<?>... rootEntities) {
			return new ParametersBuilder( rootEntity, rootEntities );
		}
	}

	/**
	 * Parameter builder for mass-indexing job. The default value of each parameter is defined in the job XML file
	 * {@code hibernate-search-mass-indexing.xml}.
	 */
	public static class ParametersBuilder {

		private final Set<Class<?>> rootEntities;
		private String entityManagerFactoryScope;
		private String entityManagerFactoryReference;
		private Boolean cacheable;
		private Boolean optimizeAfterPurge;
		private Boolean optimizeOnFinish;
		private Boolean purgeAllOnStart;
		private Integer fetchSize;
		private Integer checkpointInterval;
		private Integer rowsPerPartition;
		private Integer maxThreads;
		private Set<Criterion> customQueryCriteria;
		private String customQueryHql;
		private Integer customQueryLimit;

		private ParametersBuilder(Class<?> rootEntity, Class<?>... rootEntities) {
			if ( rootEntity == null ) {
				throw new IllegalArgumentException( "rootEntities must have at least 1 element." );
			}
			this.rootEntities = new HashSet<>();
			this.rootEntities.add( rootEntity );
			Collections.addAll( this.rootEntities, rootEntities );
			customQueryCriteria = new HashSet<>();
		}

		public ParametersBuilder entityManagerFactoryScope(String scope) {
			this.entityManagerFactoryScope = scope;
			return this;
		}

		/**
		 * The string that will identify the {@link javax.persistence.EntityManagerFactory EntityManagerFactory}. This
		 * method is required if there's more than one persistence unit.
		 *
		 * @param reference the name of reference
		 *
		 * @return itself
		 */
		public ParametersBuilder entityManagerFactoryReference(String reference) {
			this.entityManagerFactoryReference = reference;
			return this;
		}

		/**
		 * Whether the Hibernate queries in this job should be cached. This method is optional, its default value is
		 * false. Set it to true when reading a complex graph with relations.
		 *
		 * @param cacheable cacheable
		 *
		 * @return itself
		 */
		public ParametersBuilder cacheable(boolean cacheable) {
			this.cacheable = cacheable;
			return this;
		}

		/**
		 * The number of entities to process before triggering the next checkpoint. The value defined must be greater
		 * than 0, and less than the value of {@link #rowsPerPartition}. This is an optional method, its default value
		 * is {@literal 200}.
		 *
		 * @param checkpointInterval the number of entities to process before triggering the next checkpoint.
		 *
		 * @return itself
		 */
		public ParametersBuilder checkpointInterval(int checkpointInterval) {
			this.checkpointInterval = checkpointInterval;
			return this;
		}

		/**
		 * The number of rows to retrieve for each database query. This is an optional method, its default value is
		 * {@literal 200,000}.
		 *
		 * @param fetchSize the number of rows to retrieve for each database query.
		 *
		 * @return itself
		 */
		public ParametersBuilder fetchSize(int fetchSize) {
			if ( fetchSize < 1 ) {
				throw new IllegalArgumentException( "fetchSize must be at least 1" );
			}
			this.fetchSize = fetchSize;
			return this;
		}

		/**
		 * The maximum number of results will be returned from the HQL / criteria. It is equivalent to keyword
		 * {@literal LIMIT} in SQL.
		 *
		 * @param customQueryLimit the custom query limit.
		 *
		 * @return itself
		 */
		public ParametersBuilder customQueryLimit(int customQueryLimit) {
			if ( customQueryLimit < 1 ) {
				throw new IllegalArgumentException( "customQueryLimit must be at least 1" );
			}
			this.customQueryLimit = customQueryLimit;
			return this;
		}

		/**
		 * The maximum number of threads to use for processing the job. Note the batch runtime cannot guarantee the
		 * request number of threads are available; it will use as many as it can up to the request maximum. This
		 * method is optional, its default value is {@literal 8}.
		 *
		 * @param maxThreads the maximum number of threads.
		 *
		 * @return itself
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
		 * after the purge operation and before indexing. This method is optional, its default value is false.
		 *
		 * @param optimizeAfterPurge optimize after purge.
		 *
		 * @return itself
		 */
		public ParametersBuilder optimizeAfterPurge(boolean optimizeAfterPurge) {
			this.optimizeAfterPurge = optimizeAfterPurge;
			return this;
		}

		/**
		 * Specify whether the mass indexer should be optimized at the end of the job. This operation takes place after
		 * indexing. This method is optional, its default value is false.
		 *
		 * @param optimizeOnFinish optimize on finish.
		 *
		 * @return itself
		 */
		public ParametersBuilder optimizeOnFinish(boolean optimizeOnFinish) {
			this.optimizeOnFinish = optimizeOnFinish;
			return this;
		}

		/**
		 * Specify whether the existing lucene index should be purged at the beginning of the job. This operation takes
		 * place before indexing. This method is optional, its default value is false.
		 *
		 * @param purgeAllOnStart purge all on start.
		 *
		 * @return itself
		 */
		public ParametersBuilder purgeAllOnStart(boolean purgeAllOnStart) {
			this.purgeAllOnStart = purgeAllOnStart;
			return this;
		}

		/**
		 * Add criterion to construct a customized selection of mass-indexing under the criteria approach. You
		 * can call this method multiple times to add multiple criteria: only entities matching every criterion
		 * will be indexed. However, mixing this approach with the HQL restriction is not allowed.
		 *
		 * @param criterion criterion.
		 *
		 * @return itself
		 */
		public ParametersBuilder restrictedBy(Criterion criterion) {
			if ( customQueryHql != null ) {
				throw new IllegalArgumentException( "Cannot use HQL approach and Criteria approach in the same time." );
			}
			if ( criterion == null ) {
				throw new NullPointerException( "The criterion is null." );
			}
			customQueryCriteria.add( criterion );
			return this;
		}

		/**
		 * Use HQL / JPQL to index entities of a target entity type. Your query should contain only one entity type.
		 * Mixing this approach with the criteria restriction is not allowed. Please notice that there's no query
		 * validation for your input.
		 *
		 * @param hql HQL / JPQL.
		 *
		 * @return itself
		 */
		public ParametersBuilder restrictedBy(String hql) {
			if ( hql == null ) {
				throw new NullPointerException( "The HQL is null." );
			}
			if ( customQueryCriteria.size() > 0 ) {
				throw new IllegalArgumentException( "Cannot use HQL approach and Criteria approach in the same time." );
			}
			this.customQueryHql = hql;
			return this;
		}

		/**
		 * The maximum number of rows to process per partition. The value defined must be greater than 0, and greater
		 * than the value of {@link #checkpointInterval}. This method is optional, its default value is {@literal 250}.
		 *
		 * @param rowsPerPartition Rows per partition.
		 *
		 * @return itself
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
		 * @return the parameters.
		 *
		 * @throws SearchException if the serialization of some parameters fail.
		 */
		public Properties build() {
			Properties jobParams = new Properties();

			addIfNotNull( jobParams, MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_SCOPE, entityManagerFactoryScope );
			addIfNotNull( jobParams, MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE, entityManagerFactoryReference );
			addIfNotNull( jobParams, MassIndexingJobParameters.CACHEABLE, cacheable );
			addIfNotNull( jobParams, MassIndexingJobParameters.FETCH_SIZE, fetchSize );
			addIfNotNull( jobParams, MassIndexingJobParameters.CUSTOM_QUERY_HQL, customQueryHql );
			addIfNotNull( jobParams, MassIndexingJobParameters.CHECKPOINT_INTERVAL, checkpointInterval );
			addIfNotNull( jobParams, MassIndexingJobParameters.CUSTOM_QUERY_LIMIT, customQueryLimit );
			addIfNotNull( jobParams, MassIndexingJobParameters.MAX_THREADS, maxThreads );
			addIfNotNull( jobParams, MassIndexingJobParameters.OPTIMIZE_AFTER_PURGE, optimizeAfterPurge );
			addIfNotNull( jobParams, MassIndexingJobParameters.OPTIMIZE_ON_FINISH, optimizeOnFinish );
			addIfNotNull( jobParams, MassIndexingJobParameters.PURGE_ALL_ON_START, purgeAllOnStart );
			addIfNotNull( jobParams, MassIndexingJobParameters.ROOT_ENTITIES, getRootEntitiesAsString() );
			addIfNotNull( jobParams, MassIndexingJobParameters.ROWS_PER_PARTITION, rowsPerPartition );

			if ( !customQueryCriteria.isEmpty() ) {
				try {
					jobParams.put(
							MassIndexingJobParameters.CUSTOM_QUERY_CRITERIA,
							MassIndexerUtil.serializeCriteria( customQueryCriteria )
					);
				}
				catch (IOException e) {
					throw new SearchException( "Failed to serialize Criteria", e );
				}
			}

			return jobParams;
		}

		private String getRootEntitiesAsString() {
			return rootEntities.stream()
					.map( Class::getName )
					.collect( Collectors.joining( "," ) );
		}

		private void addIfNotNull(Properties properties, String key, Object value) {
			if ( value != null ) {
				properties.put( key, String.valueOf( value ) );
			}
		}
	}

}
