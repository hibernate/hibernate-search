/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.CacheMode;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.ValidationUtil;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * A utility class to start the Hibernate Search Jakarta Batch mass indexing job.
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
		private Integer checkpointInterval;
		private Integer rowsPerPartition;
		private Integer maxThreads;
		private String reindexOnlyHql;
		private String serializedReindexOnlyParameters;
		private Integer maxResultsPerEntity;
		private Object tenantId;

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
		 * Specifies the fetch size to be used when loading entities from the database.
		 * <p>
		 * The value defined must be greater
		 * than 0, and equal to or less than the value of {@link #checkpointInterval}.
		 * <p>
		 * This is an optional parameter, its default value is
		 * {@link MassIndexingJobParameters.Defaults#ENTITY_FETCH_SIZE_RAW},
		 * or the value of {@link #checkpointInterval} if it is smaller.
		 *
		 * @param entityFetchSize the fetch size to be used when loading entities
		 *
		 * @return itself
		 */
		public ParametersBuilder entityFetchSize(int entityFetchSize) {
			Contracts.assertStrictlyPositive( entityFetchSize, "entityFetchSize" );
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
		 * Use a JPQL/HQL conditional expression to limit the entities to be re-indexed.
		 * <p>
		 * The letter {@code e} is supposed to be used here as query alias.
		 * For instance a valid expression could be the following:
		 * <pre>
		 *     manager.level &lt; 2
		 * </pre>
		 * ... to filter instances that have a manager whose level is strictly less than 2.
		 * <p>
		 * Parameters can be used, so assuming the parameter "max" is defined
		 * in the {@code parameters} {@link Map},
		 * this is valid as well:
		 * <pre>
		 *     manager.level &lt; :max
		 * </pre>
		 * ... to filter instances that have a manager whose level is strictly less than {@code :max}.
		 *
		 * @param hql A JPQL/HQL conditional expression, e.g. {@code manager.level < 2}
		 * @param parameters A map of named parameters parameters that may be used in the conditional expression
		 * with the usual JPQL/HQL colon-prefixed syntax (e.g. ":myparam").
		 *
		 * @return itself
		 */
		public ParametersBuilder reindexOnly(String hql, Map<String, ?> parameters) {
			Contracts.assertNotNull( hql, "hql" );
			Contracts.assertNotNull( parameters, "parameters" );
			this.reindexOnlyHql = hql;
			try {
				this.serializedReindexOnlyParameters = parameters == null
						? null
						: SerializationUtil.serialize( parameters );
			}
			catch (IOException e) {
				throw new IllegalArgumentException(
						"Failed to serialize parameters; the parameters must be a serializable Map with serializable keys and values.",
						e );
			}
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
		 * Define the tenant ID for the job execution.
		 *
		 * @param tenantId Tenant ID. Null or empty value is not allowed.
		 *
		 * @return itself
		 */
		public ParametersBuilder tenantId(Object tenantId) {
			if ( tenantId == null ) {
				throw new NullPointerException( "Your tenantId is null, please provide a valid tenant ID." );
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
			int defaultedCheckpointInterval =
					MassIndexingJobParameters.Defaults.checkpointInterval( checkpointInterval, rowsPerPartition );
			ValidationUtil.validateCheckpointInterval(
					defaultedCheckpointInterval,
					rowsPerPartition != null ? rowsPerPartition : MassIndexingJobParameters.Defaults.ROWS_PER_PARTITION
			);
			int defaultedEntityFetchSize =
					MassIndexingJobParameters.Defaults.entityFetchSize( entityFetchSize,
							defaultedCheckpointInterval );
			ValidationUtil.validateEntityFetchSize( defaultedEntityFetchSize, defaultedCheckpointInterval );

			Properties jobParams = new Properties();

			addIfNotNull( jobParams, MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE,
					entityManagerFactoryNamespace );
			addIfNotNull( jobParams, MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE,
					entityManagerFactoryReference );
			addIfNotNull( jobParams, MassIndexingJobParameters.ID_FETCH_SIZE, idFetchSize );
			addIfNotNull( jobParams, MassIndexingJobParameters.ENTITY_FETCH_SIZE, entityFetchSize );
			addIfNotNull( jobParams, MassIndexingJobParameters.REINDEX_ONLY_HQL, reindexOnlyHql );
			addIfNotNull( jobParams, MassIndexingJobParameters.REINDEX_ONLY_PARAMETERS,
					serializedReindexOnlyParameters );
			addIfNotNull( jobParams, MassIndexingJobParameters.CHECKPOINT_INTERVAL, checkpointInterval );
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
