/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.spi;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.listener.AbstractJobListener;
import jakarta.batch.runtime.context.JobContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.hibernate.search.jakarta.batch.core.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.jakarta.batch.core.inject.scope.spi.HibernateSearchJobScoped;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.JobContextUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.SerializationUtil;
import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.ValidationUtil;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.StringHelper;

/**
 * Listener before the start of the job. It aims to validate all the job
 * parameters and setup the job context data, shared by all the steps.
 *
 * @author Mincong Huang
 */
/*
 * Hack to make sure that, when using dependency injection,
 * this bean is resolved using DI and is properly injected.
 * Otherwise it would just be instantiated using its default
 * constructor and would not be injected.
 */
@Named(value = "org.hibernate.search.jsr352.massindexing.impl.JobContextSetupListener")
@HibernateSearchJobScoped
public class JobContextSetupListener extends AbstractJobListener {

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE)
	private String entityManagerFactoryNamespace;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE)
	private String entityManagerFactoryReference;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_TYPES)
	private String serializedEntityTypes;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.MAX_THREADS)
	private String serializedMaxThreads;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY)
	private String serializedMaxResultsPerEntity;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ID_FETCH_SIZE)
	private String serializedIdFetchSize;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_FETCH_SIZE)
	private String serializedEntityFetchSize;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CACHE_MODE)
	private String serializedCacheMode;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.MERGE_SEGMENTS_ON_FINISH)
	private String serializedMergeSegmentsOnFinish;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.MERGE_SEGMENTS_AFTER_PURGE)
	private String serializedMergeSegmentsAfterPurge;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.PURGE_ALL_ON_START)
	private String serializedPurgeAllOnStart;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CHECKPOINT_INTERVAL)
	private String serializedCheckpointInterval;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ROWS_PER_PARTITION)
	private String serializedRowsPerPartition;

	@Inject
	private EntityManagerFactoryRegistry emfRegistry;

	@Override
	public void beforeJob() throws Exception {
		validateParameters();
		JobContextUtil.getOrCreateData( jobContext,
				emfRegistry, entityManagerFactoryNamespace, entityManagerFactoryReference,
				serializedEntityTypes );
	}

	/**
	 * Validates job parameters.
	 *
	 * @throws SearchException if any validation fails.
	 */
	private void validateParameters() throws SearchException {
		validateEntityTypes();
		validateQuerying();
		validateChunkSettings();
		validateJobSettings();
	}

	private void validateEntityTypes() {
		ValidationUtil.validateEntityTypes(
				emfRegistry,
				entityManagerFactoryNamespace,
				entityManagerFactoryReference,
				serializedEntityTypes
		);
	}

	private void validateChunkSettings() {
		Integer rowsPerPartition = SerializationUtil.parseIntegerParameterOptional(
				MassIndexingJobParameters.ROWS_PER_PARTITION, serializedRowsPerPartition,
				MassIndexingJobParameters.Defaults.ROWS_PER_PARTITION
		);
		Integer checkpointIntervalRaw = SerializationUtil.parseIntegerParameterOptional(
				MassIndexingJobParameters.CHECKPOINT_INTERVAL, serializedCheckpointInterval, null
		);
		int checkpointInterval =
				MassIndexingJobParameters.Defaults.checkpointInterval( checkpointIntervalRaw, rowsPerPartition );
		Integer entityFetchSizeRaw = SerializationUtil.parseIntegerParameterOptional(
				MassIndexingJobParameters.ENTITY_FETCH_SIZE, serializedEntityFetchSize, null
		);
		int entityFetchSize =
				MassIndexingJobParameters.Defaults.entityFetchSize( entityFetchSizeRaw, checkpointInterval );

		ValidationUtil.validatePositive( MassIndexingJobParameters.ENTITY_FETCH_SIZE, entityFetchSize );
		ValidationUtil.validatePositive( MassIndexingJobParameters.CHECKPOINT_INTERVAL, checkpointInterval );
		ValidationUtil.validatePositive( MassIndexingJobParameters.ROWS_PER_PARTITION, rowsPerPartition );
		ValidationUtil.validateCheckpointInterval( checkpointInterval, rowsPerPartition );
		ValidationUtil.validateEntityFetchSize( entityFetchSize, checkpointInterval );
	}

	private void validateJobSettings() {
		if ( StringHelper.isNotEmpty( serializedMaxThreads ) ) {
			int maxThreads =
					SerializationUtil.parseIntegerParameter( MassIndexingJobParameters.MAX_THREADS, serializedMaxThreads );
			ValidationUtil.validatePositive( MassIndexingJobParameters.MAX_THREADS, maxThreads );
		}

		// A boolean parameter is validated if its deserialization is successful.
		SerializationUtil.parseBooleanParameterOptional( MassIndexingJobParameters.MERGE_SEGMENTS_ON_FINISH,
				serializedMergeSegmentsOnFinish,
				MassIndexingJobParameters.Defaults.MERGE_SEGMENTS_ON_FINISH );
		SerializationUtil.parseBooleanParameterOptional( MassIndexingJobParameters.MERGE_SEGMENTS_AFTER_PURGE,
				serializedMergeSegmentsAfterPurge,
				MassIndexingJobParameters.Defaults.MERGE_SEGMENTS_AFTER_PURGE );
		SerializationUtil.parseBooleanParameterOptional( MassIndexingJobParameters.PURGE_ALL_ON_START,
				serializedPurgeAllOnStart,
				MassIndexingJobParameters.Defaults.PURGE_ALL_ON_START );
	}

	private void validateQuerying() {
		SerializationUtil.parseIntegerParameterOptional( MassIndexingJobParameters.ID_FETCH_SIZE, serializedIdFetchSize,
				MassIndexingJobParameters.Defaults.ID_FETCH_SIZE );

		if ( StringHelper.isNotEmpty( serializedEntityFetchSize ) ) {
			SerializationUtil.parseIntegerParameter( MassIndexingJobParameters.ENTITY_FETCH_SIZE, serializedEntityFetchSize );
		}

		if ( StringHelper.isNotEmpty( serializedMaxResultsPerEntity ) ) {
			int maxResultsPerEntity = SerializationUtil.parseIntegerParameter(
					MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY,
					serializedMaxResultsPerEntity
			);
			ValidationUtil.validatePositive( MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY, maxResultsPerEntity );
		}

		SerializationUtil.parseCacheModeParameter( MassIndexingJobParameters.CACHE_MODE, serializedCacheMode,
				MassIndexingJobParameters.Defaults.CACHE_MODE );
	}

}
