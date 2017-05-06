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

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jsr352.context.jpa.EntityManagerFactoryRegistry;
import org.hibernate.search.jsr352.inject.scope.HibernateSearchJobScoped;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.jsr352.massindexing.impl.util.JobContextUtil;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.jsr352.massindexing.impl.util.ValidationUtil.validateCheckpointInterval;
import static org.hibernate.search.jsr352.massindexing.impl.util.ValidationUtil.validatePositive;

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

	private static final Log log = LoggerFactory.make( Log.class );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_SCOPE)
	private String entityManagerFactoryScope;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE)
	private String entityManagerFactoryReference;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ENTITY_TYPES)
	private String entityTypes;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CUSTOM_QUERY_CRITERIA)
	private String serializedCustomQueryCriteria;

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
				emfRegistry, entityManagerFactoryScope, entityManagerFactoryReference,
				entityTypes, serializedCustomQueryCriteria );
	}

	/**
	 * Validates job parameters.
	 *
	 * @throws SearchException if any validation fails.
	 */
	private void validateParameters() throws SearchException {
		int checkpointInterval = parseInt( MassIndexingJobParameters.CHECKPOINT_INTERVAL, serializedCheckpointInterval );
		int rowsPerPartition = parseInt( MassIndexingJobParameters.ROWS_PER_PARTITION, serializedRowsPerPartition );

		validatePositive( MassIndexingJobParameters.CHECKPOINT_INTERVAL, checkpointInterval );
		validatePositive( MassIndexingJobParameters.ROWS_PER_PARTITION, rowsPerPartition );
		validateCheckpointInterval( checkpointInterval, rowsPerPartition );
	}

	private int parseInt(String parameterName, String parameterValue) {
		try {
			return Integer.parseInt( parameterValue );
		}
		catch (NumberFormatException e) {
			throw log.unableToParseJobParameter( parameterName, parameterValue, e );
		}
	}

}
