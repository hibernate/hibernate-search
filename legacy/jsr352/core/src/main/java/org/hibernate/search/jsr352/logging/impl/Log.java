/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jsr352.massindexing.MassIndexingJobParameters;
import org.hibernate.search.util.logging.impl.BaseHibernateSearchLogger;
import org.hibernate.search.util.logging.impl.ClassFormatter;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Hibernate Search log abstraction for the JSR 352 integration.
 *
 * @author Mincong Huang
 */
@MessageLogger(projectCode = "HSEARCH")
public interface Log extends BaseHibernateSearchLogger {

	int JSR_352_MESSAGES_START_ID = 500000;

	@Message(id = JSR_352_MESSAGES_START_ID + 1,
			value = "An '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE + "' parameter was defined,"
					+ " but the '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE + "' parameter is empty."
					+ " Please also set the '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE + "' parameter"
					+ " to select an entity manager factory, or do not set the '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE
					+ "' parameter to try to use a default entity manager factory."
	)
	SearchException entityManagerFactoryReferenceIsEmpty();

	@Message(id = JSR_352_MESSAGES_START_ID + 2,
			value = "No entity manager factory available in the CDI context with this bean name: '%1$s'."
					+ " Make sure your entity manager factory is a named bean."
	)
	SearchException noAvailableEntityManagerFactoryInCDI(String reference);

	@Message(id = JSR_352_MESSAGES_START_ID + 3,
			value = "Unknown entity manager factory namespace: '%1$s'. Please use a supported namespace.")
	SearchException unknownEntityManagerFactoryNamespace(String namespace);

	@Message(id = JSR_352_MESSAGES_START_ID + 4,
			value = "Exception while retrieving the EntityManagerFactory using @PersistenceUnit."
					+ " This generally happens either because the persistence wasn't configured properly"
					+ " or because there are multiple persistence units."
	)
	SearchException cannotRetrieveEntityManagerFactoryInJsr352();

	@Message(id = JSR_352_MESSAGES_START_ID + 5,
			value = "Multiple entity manager factories have been registered in the CDI context."
					+ " Please provide the bean name for the selected entity manager factory to the batch indexing job through"
					+ " the 'entityManagerFactoryReference' parameter."
	)
	SearchException ambiguousEntityManagerFactoryInJsr352();

	@Message(id = JSR_352_MESSAGES_START_ID + 6,
			value = "No entity manager factory has been created with this persistence unit name yet: '%1$s'."
					+ " Make sure you use the JPA API to create your entity manager factory (use a 'persistence.xml' file)"
					+ " and that the entity manager factory has already been created and wasn't closed before"
					+ " you launch the job."
	)
	SearchException cannotFindEntityManagerFactoryByPUName(String persistentUnitName);

	@Message(id = JSR_352_MESSAGES_START_ID + 7,
			value = "No entity manager factory has been created with this name yet: '%1$s'."
					+ " Make sure your entity manager factory is named (for instance by setting the '"
					+ AvailableSettings.SESSION_FACTORY_NAME + "' option) and that the entity manager factory has"
					+ " already been created and wasn't closed before you launch the job."
	)
	SearchException cannotFindEntityManagerFactoryByName(String entityManagerFactoryName);

	@Message(id = JSR_352_MESSAGES_START_ID + 8,
			value = "No entity manager factory has been created yet."
					+ " Make sure that the entity manager factory has already been created and wasn't closed before"
					+ " you launched the job."
	)
	SearchException noEntityManagerFactoryCreated();

	@Message(id = JSR_352_MESSAGES_START_ID + 9,
			value = "Multiple entity manager factories are currently active."
					+ " Please provide the name of the selected persistence unit to the batch indexing job through"
					+ " the '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_REFERENCE
					+ "' parameter (you may also use the '" + MassIndexingJobParameters.ENTITY_MANAGER_FACTORY_NAMESPACE
					+ "' parameter for more referencing options)."
	)
	SearchException tooManyActiveEntityManagerFactories();

	@LogMessage(level = INFO)
	@Message(id = JSR_352_MESSAGES_START_ID + 10,
			value = "%1$s"
	)
	void analyzeIndexProgress(String progress);

	@LogMessage(level = INFO)
	@Message(id = JSR_352_MESSAGES_START_ID + 15,
			value = "Optimizing all entities ..."
	)
	void startOptimization();

	@LogMessage(level = DEBUG)
	@Message(id = JSR_352_MESSAGES_START_ID + 16,
			value = "%1$d criteria found."
	)
	void criteriaSize(int size);

	@LogMessage(level = DEBUG)
	@Message(id = JSR_352_MESSAGES_START_ID + 17,
			value = "Checkpoint reached. Sending checkpoint ID to batch runtime... (entity='%1$s', checkpointInfo='%2$s')"
	)
	void checkpointReached(String entityName, Object checkpointInfo);

	@LogMessage(level = DEBUG)
	@Message(id = JSR_352_MESSAGES_START_ID + 18,
			value = "Opening EntityReader of partitionId='%1$s', entity='%2$s'."
	)
	void openingReader(String partitionId, String entityName);

	@LogMessage(level = DEBUG)
	@Message(id = JSR_352_MESSAGES_START_ID + 19,
			value = "Closing EntityReader of partitionId='%1$s', entity='%2$s'."
	)
	void closingReader(String partitionId, String entityName);

	@LogMessage(level = TRACE)
	@Message(id = JSR_352_MESSAGES_START_ID + 21,
			value = "Reading entity..."
	)
	void readingEntity();

	@LogMessage(level = INFO)
	@Message(id = JSR_352_MESSAGES_START_ID + 22,
			value = "No more results, read ends."
	)
	void noMoreResults();

	@LogMessage(level = TRACE)
	@Message(id = JSR_352_MESSAGES_START_ID + 23,
			value = "Processing entity with id: '%1$s'"
	)
	void processEntity(Object entityId);

	@LogMessage(level = DEBUG)
	@Message(id = JSR_352_MESSAGES_START_ID + 24,
			value = "Opening LuceneDocWriter of partitionId='%1$s', entity='%2$s'.")
	void openingDocWriter(String partitionId, String entityName);

	@LogMessage(level = DEBUG)
	@Message(id = JSR_352_MESSAGES_START_ID + 25,
			value = "Closing LuceneDocWriter of partitionId='%1$s', entity='%2$s'.")
	void closingDocWriter(String partitionId, String entityName);

	@LogMessage(level = INFO)
	@Message(id = JSR_352_MESSAGES_START_ID + 26,
			value = "%1$d partitions, %2$d threads."
	)
	void partitionsPlan(int partitionSize, int threadSize);

	@LogMessage(level = INFO)
	@Message(id = JSR_352_MESSAGES_START_ID + 27,
			value = "entityName: '%1$s', rowsToIndex: %2$d")
	void rowsToIndex(String entityName, Long rowsToIndex);

	@Message(id = JSR_352_MESSAGES_START_ID + 28,
			value = "Failed to serialize job parameter of type %1$s")
	SearchException failedToSerializeJobParameter(@FormatWith(ClassFormatter.class) Class<?> type, @Cause Throwable e);

	@Message(id = JSR_352_MESSAGES_START_ID + 29,
			value = "Unable to parse value '%2$s' for job parameter '%1$s'."
	)
	SearchException unableToParseJobParameter(String parameterName, Object parameterValue, @Cause Exception e);

	@Message(id = JSR_352_MESSAGES_START_ID + 30,
			value = "The value of parameter '" + MassIndexingJobParameters.CHECKPOINT_INTERVAL
					+ "' (value=%1$d) should be equal to or less than the value of parameter '"
					+ MassIndexingJobParameters.ROWS_PER_PARTITION + "' (value=%2$d)."
	)
	SearchException illegalCheckpointInterval(int checkpointInterval, int rowsPerPartition);

	@Message(id = JSR_352_MESSAGES_START_ID + 31,
			value = "The value of parameter '%1$s' (value=%2$d) should be greater than 0."
	)
	SearchException negativeValueOrZero(String parameterName, Number parameterValue);

	@Message(id = JSR_352_MESSAGES_START_ID + 32,
			value = "The following selected entity types aren't indexable: %1$s. Please check if the annotation"
					+ " '@Indexed' has been added to each of them."
	)
	SearchException failingEntityTypes(String failingEntityNames);

	@Message(id = JSR_352_MESSAGES_START_ID + 33,
			value = "The value of parameter '" + MassIndexingJobParameters.SESSION_CLEAR_INTERVAL
					+ "' (value=%1$d) should be equal to or less than the value of parameter '"
					+ MassIndexingJobParameters.CHECKPOINT_INTERVAL + "' (value=%2$d)."
	)
	SearchException illegalSessionClearInterval(int sessionClearInterval, int checkpointInterval);

}
