/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.Criteria;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jsr352.context.jpa.spi.EntityManagerFactoryRegistry;
import org.hibernate.search.jsr352.inject.scope.spi.HibernateSearchJobScoped;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.Defaults;
import org.hibernate.search.jsr352.massindexing.impl.util.JobContextUtil;
import org.hibernate.search.jsr352.massindexing.impl.util.SerializationUtil;
import org.hibernate.search.jsr352.massindexing.impl.util.ValidationUtil;
import org.hibernate.search.util.StringHelper;

import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.CACHE_MODE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.CHECKPOINT_INTERVAL;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.CUSTOM_QUERY_CRITERIA;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.CUSTOM_QUERY_HQL;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.ENTITY_FETCH_SIZE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.ENTITY_TYPES;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.MAX_RESULTS_PER_ENTITY;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.ID_FETCH_SIZE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.MAX_THREADS;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.OPTIMIZE_AFTER_PURGE;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.OPTIMIZE_ON_FINISH;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.PURGE_ALL_ON_START;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.ROWS_PER_PARTITION;
import static org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters.SESSION_CLEAR_INTERVAL;

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
	@BatchProperty(name = ENTITY_MANAGER_FACTORY_NAMESPACE)
	private String entityManagerFactoryNamespace;

	@Inject
	@BatchProperty(name = ENTITY_MANAGER_FACTORY_REFERENCE)
	private String entityManagerFactoryReference;

	@Inject
	@BatchProperty(name = ENTITY_TYPES)
	private String serializedEntityTypes;

	@Inject
	@BatchProperty(name = MAX_THREADS)
	private String serializedMaxThreads;

	@Inject
	@BatchProperty(name = MAX_RESULTS_PER_ENTITY)
	private String serializedMaxResultsPerEntity;

	@Inject
	@BatchProperty(name = ID_FETCH_SIZE)
	private String serializedIdFetchSize;

	@Inject
	@BatchProperty(name = ENTITY_FETCH_SIZE)
	private String serializedEntityFetchSize;

	@Inject
	@BatchProperty(name = CACHE_MODE)
	private String serializedCacheMode;

	@Inject
	@BatchProperty(name = OPTIMIZE_ON_FINISH)
	private String serializedOptimizedOnFinish;

	@Inject
	@BatchProperty(name = OPTIMIZE_AFTER_PURGE)
	private String serializedOptimizedAfterPurge;

	@Inject
	@BatchProperty(name = PURGE_ALL_ON_START)
	private String serializedPurgeAllOnStart;

	@Inject
	@BatchProperty(name = CHECKPOINT_INTERVAL)
	private String serializedCheckpointInterval;

	@Inject
	@BatchProperty(name = SESSION_CLEAR_INTERVAL)
	private String serializedSessionClearInterval;

	@Inject
	@BatchProperty(name = ROWS_PER_PARTITION)
	private String serializedRowsPerPartition;

	@Inject
	@BatchProperty(name = CUSTOM_QUERY_CRITERIA)
	private String serializedCustomQueryCriteria;

	@Inject
	private EntityManagerFactoryRegistry emfRegistry;

	@Override
	public void beforeJob() throws Exception {
		validateParameters();
		JobContextUtil.getOrCreateData( jobContext,
				emfRegistry, entityManagerFactoryNamespace, entityManagerFactoryReference,
				serializedEntityTypes, serializedCustomQueryCriteria );
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
				ROWS_PER_PARTITION, serializedRowsPerPartition, Defaults.ROWS_PER_PARTITION
		);
		Integer checkpointIntervalRaw = SerializationUtil.parseIntegerParameterOptional(
				CHECKPOINT_INTERVAL, serializedCheckpointInterval, null
		);
		int checkpointInterval = Defaults.checkpointInterval( checkpointIntervalRaw, rowsPerPartition );
		Integer sessionClearIntervalRaw = SerializationUtil.parseIntegerParameterOptional(
				SESSION_CLEAR_INTERVAL, serializedSessionClearInterval, null
		);
		int sessionClearInterval = Defaults.sessionClearInterval( sessionClearIntervalRaw, checkpointInterval );

		ValidationUtil.validatePositive( SESSION_CLEAR_INTERVAL, sessionClearInterval );
		ValidationUtil.validatePositive( CHECKPOINT_INTERVAL, checkpointInterval );
		ValidationUtil.validatePositive( ROWS_PER_PARTITION, rowsPerPartition );
		ValidationUtil.validateCheckpointInterval( checkpointInterval, rowsPerPartition );
		ValidationUtil.validateSessionClearInterval( sessionClearInterval, checkpointInterval );
	}

	private void validateJobSettings() {
		if ( StringHelper.isNotEmpty( serializedMaxThreads ) ) {
			int maxThreads = SerializationUtil.parseIntegerParameter( MAX_THREADS, serializedMaxThreads );
			ValidationUtil.validatePositive( MAX_THREADS, maxThreads );
		}

		// A boolean parameter is validated if its deserialization is successful.
		SerializationUtil.parseBooleanParameterOptional( OPTIMIZE_ON_FINISH , serializedOptimizedOnFinish, Defaults.OPTIMIZE_ON_FINISH );
		SerializationUtil.parseBooleanParameterOptional( OPTIMIZE_AFTER_PURGE, serializedOptimizedAfterPurge, Defaults.OPTIMIZE_AFTER_PURGE );
		SerializationUtil.parseBooleanParameterOptional( PURGE_ALL_ON_START, serializedPurgeAllOnStart, Defaults.PURGE_ALL_ON_START );
	}

	private void validateQuerying() {
		SerializationUtil.parseIntegerParameterOptional( ID_FETCH_SIZE, serializedIdFetchSize, Defaults.ID_FETCH_SIZE );

		if ( StringHelper.isNotEmpty( serializedEntityFetchSize ) ) {
			SerializationUtil.parseIntegerParameter( ENTITY_FETCH_SIZE, serializedEntityFetchSize );
		}

		if ( StringHelper.isNotEmpty( serializedMaxResultsPerEntity ) ) {
			int maxResultsPerEntity = SerializationUtil.parseIntegerParameter(
					MAX_RESULTS_PER_ENTITY,
					serializedMaxResultsPerEntity
			);
			ValidationUtil.validatePositive( MAX_RESULTS_PER_ENTITY, maxResultsPerEntity );
		}

		SerializationUtil.parseCacheModeParameter( CACHE_MODE, serializedCacheMode, Defaults.CACHE_MODE );

		if ( StringHelper.isNotEmpty( serializedCustomQueryCriteria ) ) {
			SerializationUtil.parseParameter( Criteria.class, CUSTOM_QUERY_HQL, serializedCustomQueryCriteria );
		}
	}

}
