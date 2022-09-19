/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.CacheMode;
import org.hibernate.search.batch.jsr352.core.massindexing.MassIndexingJobParameters.Defaults;
import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.ValidationUtil;
import org.hibernate.search.util.common.SearchException;

/**
 * A utility class to start the Hibernate Search JSR-352 mass indexing job.
 * <p>
 * Use it like this:
 * <code>
 * jobOperator.start(
 * 		MassIndexingJob.NAME,
 * 		MassIndexingJob.parameters()
 * 			.forEntities( EntityA.class, EntityB.class )
 * 			.build()
 * );
 * </code>
 *
 * You can also add optional parameters to tune your job execution. See our
 * documentation for more detail.
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
		 * @param entityType the type of entity to index
		 *
		 * @return itself
		 */
		public ParametersBuilder forEntity(Class<?> entityType) {
			return new ParametersBuilder( entityType );
		}

		/**
		 * The entity types to index in this job execution.
		 *
		 * @param entityType the first type of entity to index, must not be null
		 * @param entityTypes the remaining types of entity to index
		 *
		 * @return itself
		 */
		public ParametersBuilder forEntities(Class<?> entityType, Class<?>... entityTypes) {
			return new ParametersBuilder( entityType, entityTypes );
		}
	}

	/**
	 * Parameter builder for mass-indexing job. The default value of each parameter is defined in the job XML file
	 * {@code hibernate-search-mass-indexing.xml}.
	 */
	public static class ParametersBuilder {

		private final Set<Class<?>> entityTypes;
		private String entityManagerFactoryNamespace;
		private String entityManagerFactoryReference;
		private CacheMode cacheMode;
		private Boolean mergeSegmentsAfterPurge;
		private Boolean mergeSegmentsOnFinish;
		private Boolean purgeAllOnStart;
		private Integer idFetchSize;
		private Integer entityFetchSize;
		private Integer sessionClearInterval;
		private Integer checkpointInterval;
		private Integer rowsPerPartition;
		private Integer maxThreads;
		private String customQueryHql;
		private Integer maxResultsPerEntity;
		private String tenantId;

		private ParametersBuilder(Class<?> entityType, Class<?>... entityTypes) {
			if ( entityType == null ) {
				throw new IllegalArgumentException( "entityTypes must have at least 1 element." );
			}
			this.entityTypes = new HashSet<>();
			this.entityTypes.add( entityType );
			Collections.addAll( this.entityTypes, entityTypes );
		}

		/**
		 * The string that allows to select how you want to reference the {@link EntityManagerFactory}.
		 * Possible values are:
		 * <ul>
		 * <li>{@code persistence-unit-name} (the default): use the persistence unit name defined
		 * in {@code persistence.xml}.
		 * <li>{@code session-factory-name}: use the session factory name defined in the Hibernate
		 * configuration by the {@code hibernate.session_factory_name} configuration property.
		 * </ul>
		 *
		 * @param namespace the name of namespace to use
		 *
		 * @return itself
		 */
		public ParametersBuilder entityManagerFactoryNamespace(String namespace) {
			this.entityManagerFactoryNamespace = namespace;
			return this;
		}

		/**
		 * The string that will identify the {@link EntityManagerFactory EntityManagerFactory}. This
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
		 * The Hibernate {@link CacheMode} when loading entities.
		 * <p>
		 * This is an optional parameter, its default value is
		 * {@link MassIndexingJobParameters.Defaults#CACHE_MODE}.
		 *
		 * @param cacheMode the cache mode
		 *
		 * @return itself
		 */
		public ParametersBuilder cacheMode(CacheMode cacheMode) {
			this.cacheMode = cacheMode;
			return this;
		}

		/**
		 * The number of entities to process before clearing the session. The value defined must be greater
		 * than 0, and equal to or less than the value of {@link #checkpointInterval}.
		 * <p>
		 * This is an optional parameter, its default value is
		 * {@link MassIndexingJobParameters.Defaults#SESSION_CLEAR_INTERVAL_DEFAULT_RAW},
		 * or the value of {@link #checkpointInterval} if it is smaller.
		 *
		 * @param sessionClearInterval the number of entities to process before clearing the session.
		 *
		 * @return itself
		 */
		public ParametersBuilder sessionClearInterval(int sessionClearInterval) {
			this.sessionClearInterval = sessionClearInterval;
			return this;
		}

		/**
		 * The number of entities to process before triggering the next checkpoint. The value defined must be greater
		 * than 0, and equal to or less than the value of {@link #rowsPerPartition}.
		 * <p>
		 * This is an optional parameter, its default value is
		 * {@link MassIndexingJobParameters.Defaults#CHECKPOINT_INTERVAL_DEFAULT_RAW}, or the value of
		 * {@link #rowsPerPartition} if it is smaller.
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
		 * Specifies the fetch size to be used when loading primary keys at the
		 * step-level. Some databases accept special values, for example MySQL
		 * might benefit from using {@link Integer#MIN_VALUE}, otherwise it
		 * will attempt to preload everything in memory.
		 * <p>
		 * This is an optional parameter, its default value is
		 * {@link MassIndexingJobParameters.Defaults#ID_FETCH_SIZE}.
		 *
		 * @param idFetchSize the fetch size to be used when loading primary keys
		 *
		 * @return itself
		 */
		public ParametersBuilder idFetchSize(int idFetchSize) {
			this.idFetchSize = idFetchSize;
			return this;
		}

		/**
		 * Specifies the fetch size to be used when loading entities from
		 * database. Some databases accept special values, for example MySQL
		 * might benefit from using {@link Integer#MIN_VALUE}, otherwise it
		 * will attempt to preload everything in memory.
		 * <p>
		 * This is an optional parameter, its default value is
		 * the value of the session clear interval.
		 *
		 * @param entityFetchSize the fetch size to be used when loading entities
		 *
		 * @return itself
		 */
		public ParametersBuilder entityFetchSize(int entityFetchSize) {
			this.entityFetchSize = entityFetchSize;
			return this;
		}

		/**
		 * The maximum number of results to load per entity type. This parameter let you define a
		 * threshold value to avoid loading too many entities accidentally. The value defined must
		 * be greater than 0. The parameter is not used by default. It is equivalent to keyword
		 * {@literal LIMIT} in SQL.
		 *
		 * @param maxResultsPerEntity the maximum number of results returned per entity type.
		 *
		 * @return itself
		 */
		public ParametersBuilder maxResultsPerEntity(int maxResultsPerEntity) {
			if ( maxResultsPerEntity < 1 ) {
				String msg = String.format(
						Locale.ROOT,
						"The value of parameter '%s' must be at least 1 (value=%d).",
						MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY,
						maxResultsPerEntity
				);
				throw new IllegalArgumentException( msg );
			}
			this.maxResultsPerEntity = maxResultsPerEntity;
			return this;
		}

		/**
		 * The maximum number of threads to use for processing the job. Note the batch runtime cannot guarantee the
		 * request number of threads are available; it will use as many as it can up to the request maximum.
		 * <p>
		 * This is an optional parameter, its default value is
		 * the number of partitions.
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
		 * Specify whether the mass indexer should merge segments at the beginning of the job. This operation takes place
		 * after the purge operation and before indexing.
		 * <p>
		 * This is an optional parameter, its default value is
		 * {@link MassIndexingJobParameters.Defaults#MERGE_SEGMENTS_AFTER_PURGE}.
		 *
		 * @param mergeSegmentsAfterPurge merge segments after purge.
		 *
		 * @return itself
		 */
		public ParametersBuilder mergeSegmentsAfterPurge(boolean mergeSegmentsAfterPurge) {
			this.mergeSegmentsAfterPurge = mergeSegmentsAfterPurge;
			return this;
		}

		/**
		 * Specify whether the mass indexer should merge segments at the end of the job. This operation takes place after
		 * indexing.
		 * <p>
		 * This is an optional parameter, its default value is
		 * {@link MassIndexingJobParameters.Defaults#MERGE_SEGMENTS_ON_FINISH}.
		 *
		 * @param mergeSegmentsOnFinish merge segments on finish.
		 *
		 * @return itself
		 */
		public ParametersBuilder mergeSegmentsOnFinish(boolean mergeSegmentsOnFinish) {
			this.mergeSegmentsOnFinish = mergeSegmentsOnFinish;
			return this;
		}

		/**
		 * Specify whether the existing lucene index should be purged at the beginning of the job. This operation takes
		 * place before indexing.
		 * <p>
		 * This is an optional parameter, its default value is
		 * {@link MassIndexingJobParameters.Defaults#PURGE_ALL_ON_START}.
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
			this.customQueryHql = hql;
			return this;
		}

		/**
		 * The maximum number of rows to process per partition. The value defined must be greater than 0, and greater
		 * than the value of {@link #checkpointInterval}.
		 * <p>
		 * This is an optional parameter, its default value is
		 * {@link MassIndexingJobParameters.Defaults#ROWS_PER_PARTITION}.
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
		 * Define the tenant ID for the job execution.
		 *
		 * @param tenantId Tenant ID. Null or empty value is not allowed.
		 *
		 * @return itself
		 */
		public ParametersBuilder tenantId(String tenantId) {
			if ( tenantId == null ) {
				throw new NullPointerException( "Your tenantId is null, please provide a valid tenant ID." );
			}
			if ( tenantId.isEmpty() ) {
				throw new IllegalArgumentException( "Your tenantId is empty, please provide a valid tenant ID." );
			}
			this.tenantId = tenantId;
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
			int defaultedCheckpointInterval = Defaults.checkpointInterval( checkpointInterval, rowsPerPartition );
			ValidationUtil.validateCheckpointInterval(
					defaultedCheckpointInterval,
					rowsPerPartition != null ? rowsPerPartition : Defaults.ROWS_PER_PARTITION
			);
			int defaultedSessionClearInterval =
					Defaults.sessionClearInterval( sessionClearInterval, defaultedCheckpointInterval );
			ValidationUtil.validateSessionClearInterval( defaultedSessionClearInterval, defaultedCheckpointInterval );

			Properties jobParams = new Properties();

			addIfNotNull( jobParams, MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE,
					entityManagerFactoryNamespace );
			addIfNotNull( jobParams, MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE,
					entityManagerFactoryReference );
			addIfNotNull( jobParams, MassIndexingJobParameters.ID_FETCH_SIZE, idFetchSize );
			addIfNotNull( jobParams, MassIndexingJobParameters.ENTITY_FETCH_SIZE, entityFetchSize );
			addIfNotNull( jobParams, MassIndexingJobParameters.CUSTOM_QUERY_HQL, customQueryHql );
			addIfNotNull( jobParams, MassIndexingJobParameters.CHECKPOINT_INTERVAL, checkpointInterval );
			addIfNotNull( jobParams, MassIndexingJobParameters.SESSION_CLEAR_INTERVAL, sessionClearInterval );
			addIfNotNull( jobParams, MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY, maxResultsPerEntity );
			addIfNotNull( jobParams, MassIndexingJobParameters.MAX_THREADS, maxThreads );
			addIfNotNull( jobParams, MassIndexingJobParameters.MERGE_SEGMENTS_AFTER_PURGE, mergeSegmentsAfterPurge );
			addIfNotNull( jobParams, MassIndexingJobParameters.MERGE_SEGMENTS_ON_FINISH, mergeSegmentsOnFinish );
			addIfNotNull( jobParams, MassIndexingJobParameters.PURGE_ALL_ON_START, purgeAllOnStart );
			addIfNotNull( jobParams, MassIndexingJobParameters.ENTITY_TYPES, getEntityTypesAsString() );
			addIfNotNull( jobParams, MassIndexingJobParameters.ROWS_PER_PARTITION, rowsPerPartition );
			addIfNotNull( jobParams, MassIndexingJobParameters.TENANT_ID, tenantId );
			if ( cacheMode != null ) {
				jobParams.put( MassIndexingJobParameters.CACHE_MODE, cacheMode.name() );
			}

			return jobParams;
		}

		private String getEntityTypesAsString() {
			return entityTypes.stream()
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
