/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.logging.impl;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.jakarta.batch.core.massindexing.MassIndexingJobParameters;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

/**
 * Hibernate Search log abstraction for the Jakarta Batch integration.
 *
 * @author Mincong Huang
 */
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.JAKARTA_BATCH_CORE_ID_RANGE_MIN,
				max = MessageConstants.JAKARTA_BATCH_CORE_ID_RANGE_MAX),
})
public interface Log extends BasicLogger {

	int ID_OFFSET = MessageConstants.JAKARTA_BATCH_CORE_ID_RANGE_MIN;

	@Message(id = ID_OFFSET + 1,
			value = "The '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE + "' parameter was defined,"
					+ " but the '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE + "' parameter is empty."
					+ " Set the '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE + "' parameter"
					+ " to select an entity manager factory, or do not set the '"
					+ MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE
					+ "' parameter to try to use a default entity manager factory."
	)
	SearchException entityManagerFactoryReferenceIsEmpty();

	@Message(id = ID_OFFSET + 3,
			value = "Unknown entity manager factory namespace: '%1$s'. Use a supported namespace.")
	SearchException unknownEntityManagerFactoryNamespace(String namespace);

	@Message(id = ID_OFFSET + 6,
			value = "No entity manager factory has been created with this persistence unit name yet: '%1$s'."
					+ " Make sure that you use the JPA API to create your entity manager factory (use a 'persistence.xml' file),"
					+ " that it has already been created,"
					+ " and that it hasn't been closed yet."
	)
	SearchException cannotFindEntityManagerFactoryByPUName(String persistentUnitName);

	@Message(id = ID_OFFSET + 7,
			value = "No entity manager factory has been created with this name yet: '%1$s'."
					+ " Make sure that your entity manager factory is named (for instance by setting the '"
					+ AvailableSettings.SESSION_FACTORY_NAME + "' option),"
					+ " that it has already been created,"
					+ " and that it hasn't been closed yet."
	)
	SearchException cannotFindEntityManagerFactoryByName(String entityManagerFactoryName);

	@Message(id = ID_OFFSET + 8,
			value = "No entity manager factory has been created yet."
					+ " Make sure that the entity manager factory has already been created"
					+ " and that the entity manager factory hasn't been closed."
	)
	SearchException noEntityManagerFactoryCreated();

	@Message(id = ID_OFFSET + 9,
			value = "Multiple entity manager factories are currently active."
					+ " Set the '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE
					+ " parameter to select a persistence unit."
					+ " You may also set the '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE
					+ "' parameter for more referencing options."
	)
	SearchException tooManyActiveEntityManagerFactories();

	@LogMessage(level = Level.INFO)
	@Message(id = ID_OFFSET + 10,
			value = "%1$s"
	)
	void analyzeIndexProgress(String progress);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ID_OFFSET + 17,
			value = "Checkpoint reached. Sending checkpoint ID to batch runtime... (entity='%1$s', checkpointInfo='%2$s')"
	)
	void checkpointReached(String entityName, Object checkpointInfo);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ID_OFFSET + 18,
			value = "Opening EntityIdReader of partitionId='%1$s', entity='%2$s'."
	)
	void openingReader(String partitionId, String entityName);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ID_OFFSET + 19,
			value = "Closing EntityIdReader of partitionId='%1$s', entity='%2$s'."
	)
	void closingReader(String partitionId, String entityName);

	@LogMessage(level = Level.TRACE)
	@Message(id = ID_OFFSET + 21,
			value = "Reading entity identifier..."
	)
	void readingEntityId();

	@LogMessage(level = Level.INFO)
	@Message(id = ID_OFFSET + 22,
			value = "No more results, read ends."
	)
	void noMoreResults();

	@LogMessage(level = Level.TRACE)
	@Message(id = ID_OFFSET + 23,
			value = "Processing entity: '%1$s'"
	)
	void processEntity(Object entity);

	@LogMessage(level = Level.INFO)
	@Message(id = ID_OFFSET + 26,
			value = "%1$d partitions, %2$d threads."
	)
	void partitionsPlan(int partitionSize, int threadSize);

	@LogMessage(level = Level.INFO)
	@Message(id = ID_OFFSET + 27,
			value = "entityName: '%1$s', rowsToIndex: %2$d")
	void rowsToIndex(String entityName, Long rowsToIndex);

	@Message(id = ID_OFFSET + 29,
			value = "Invalid value for job parameter '%1$s': '%2$s'. %3$s"
	)
	SearchException unableToParseJobParameter(String parameterName, Object parameterValue,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 30,
			value = "The value of parameter '" + MassIndexingJobParameters.CHECKPOINT_INTERVAL
					+ "' (value=%1$d) should be equal to or less than the value of parameter '"
					+ MassIndexingJobParameters.ROWS_PER_PARTITION + "' (value=%2$d)."
	)
	SearchException illegalCheckpointInterval(int checkpointInterval, int rowsPerPartition);

	@Message(id = ID_OFFSET + 31,
			value = "The value of parameter '%1$s' (value=%2$d) should be greater than 0."
	)
	SearchException negativeValueOrZero(String parameterName, Number parameterValue);

	@Message(id = ID_OFFSET + 32,
			value = "The following selected entity types aren't indexable: %1$s."
					+ " Check whether they are annotated with '@Indexed'."
	)
	SearchException failingEntityTypes(String failingEntityNames);

	@Message(id = ID_OFFSET + 33,
			value = "The value of parameter '" + MassIndexingJobParameters.ENTITY_FETCH_SIZE
					+ "' (value=%1$d) should be equal to or less than the value of parameter '"
					+ MassIndexingJobParameters.CHECKPOINT_INTERVAL + "' (value=%2$d)."
	)
	SearchException illegalEntityFetchSize(int entityFetchSize, int checkpointInterval);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ID_OFFSET + 34,
			value = "Opening EntityWriter of partitionId='%1$s', entity='%2$s'.")
	void openingEntityWriter(String partitionId, String entityName);

	@LogMessage(level = Level.DEBUG)
	@Message(id = ID_OFFSET + 35,
			value = "Closing EntityWriter of partitionId='%1$s', entity='%2$s'.")
	void closingEntityWriter(String partitionId, String entityName);

}
