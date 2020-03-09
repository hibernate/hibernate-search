/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.util.Collection;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.mapping.Value;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.pojo.logging.spi.PojoModelPathFormatter;
import org.hibernate.search.mapper.pojo.logging.spi.PojoTypeModelFormatter;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.ORM_ID_RANGE_MIN, max = MessageConstants.ORM_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5
		@ValidIdRange(min = 27, max = 28),
		@ValidIdRange(min = 30, max = 30),
		@ValidIdRange(min = 31, max = 31),
		@ValidIdRange(min = 34, max = 34),
		@ValidIdRange(min = 36, max = 36),
		@ValidIdRange(min = 62, max = 62),
		@ValidIdRange(min = 65, max = 65),
		@ValidIdRange(min = 116, max = 116),
		@ValidIdRange(min = 183, max = 183),
		@ValidIdRange(min = 211, max = 212),
		@ValidIdRange(min = 235, max = 235),
		@ValidIdRange(min = 276, max = 276),
		@ValidIdRange(min = 348, max = 349)
		// TODO HSEARCH-3308 add exceptions here for legacy messages from Search 5. See the Lucene logger for examples.
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_1 = MessageConstants.ENGINE_ID_RANGE_MIN;

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_1 + 27, value = "Going to reindex %d entities")
	void indexingEntities(long count);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_1 + 28, value = "Reindexed %1$d entities")
	void indexingEntitiesCompleted(long nbrOfEntities);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_1 + 30, value = "%1$d documents indexed in %2$d ms")
	void indexingDocumentsCompleted(long doneCount, long elapsedMs);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_1 + 31, value = "Indexing speed: %1$f documents/second; progress: %2$.2f%%")
	void indexingSpeed(float estimateSpeed, float estimatePercentileComplete);

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_1 + 34, value = "Hibernate Search version %1$s")
	void version(String versionString);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_1 + 36, value = "Cannot guess the Transaction Status: not starting a JTA transaction")
	void cannotGuessTransactionStatus(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(id = ID_OFFSET_1 + 62, value = "Mass indexing was interrupted")
	void interruptedBatchIndexing();

	@LogMessage(level = ERROR)
	@Message(id = ID_OFFSET_1 + 65, value = "Error while rolling back transaction after %1$s")
	void errorRollingBackTransaction(String message, @Cause Exception e1);

	/*
	 * This is not an exception factory nor a logging statement.
	 * The returned string is passed to the FailureHandler,
	 * which is not necessarily using a logger but we still
	 * want to internationalize the message.
	 */
	@Message(value = "MassIndexer operation")
	String massIndexerOperation();

	@Message(value = "Indexing instance of entity '%s' during mass indexing")
	String massIndexerIndexingInstance(String entityName);

	@Message(value = "Fetching identifiers of entities to index for entity '%s' during mass indexing")
	String massIndexerFetchingIds(String entityName);

	@Message(value = "Loading and extracting entity data for entity '%s' during mass indexing")
	String massIndexingLoadingAndExtractingEntityData(String entityName);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET_1 + 235, value = "Default automatic indexing synchronization strategy set to '%s'." )
	void defaultAutomaticIndexingSynchronizationStrategy(Object strategy);

	@Message(id = ID_OFFSET_1 + 276, value = "No transaction is active while indexing entity '%1$s'; Consider increasing the connection time-out")
	SearchException transactionNotActiveWhileProducingIdsForBatchIndexing(String entityName);

	// TODO HSEARCH-3308 migrate relevant messages from Search 5 here

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET_2 = MessageConstants.ORM_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_2 + 1,
			value = "Hibernate Search was not initialized.")
	SearchException hibernateSearchNotInitialized();

	@Message(id = ID_OFFSET_2 + 2,
			value = "Unexpected entity name for a query hit: '%1$s'. Expected one of %2$s.")
	SearchException unexpectedSearchHitEntityName(String entityName, Collection<String> expectedNames);

	@Message(id = ID_OFFSET_2 + 3,
			value = "Invalid automatic indexing strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidAutomaticIndexingStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET_2 + 7,
			value = "Path '%1$s' cannot be resolved to a persisted value in Hibernate ORM metadata."
					+ " If this path points to a transient value, use @IndexingDependency(derivedFrom = ...)"
					+ " to specify which persisted values it is derived from."
					+ " See the reference documentation for more information.")
	SearchException unknownPathForDirtyChecking(@FormatWith(PojoModelPathFormatter.class) PojoModelPath path, @Cause Exception e);

	@Message(id = ID_OFFSET_2 + 8,
			value = "Path '%1$s' can be resolved using Hibernate ORM metadata,"
					+ " but points to value '%2$s' that will never be reported as dirty by Hibernate ORM."
					+ " Check that you didn't declare an invalid indexing dependency.")
	SearchException unreportedPathForDirtyChecking(@FormatWith(PojoModelPathFormatter.class) PojoModelPath path, Value value);

	@Message(id = ID_OFFSET_2 + 9,
			value = "Container value extractor with name '%2$s' cannot be applied to"
					+ " Hibernate ORM metadata node of type '%1$s'.")
	SearchException invalidContainerExtractorForDirtyChecking(Class<?> ormMappingClass, String extractorName);

	@Message(id = ID_OFFSET_2 + 10,
			value = "Unable to find a readable property '%2$s' on type '%1$s'.")
	SearchException cannotFindReadableProperty(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			String propertyName);

	@Message(id = ID_OFFSET_2 + 11, value = "Mapping service cannot create a SearchSession using a different session factory. Expected: '%1$s'. In use: '%2$s'.")
	SearchException usingDifferentSessionFactories(SessionFactory expectedSessionFactory, SessionFactory usedSessionFactory);

	@Message(id = ID_OFFSET_2 + 12, value = "Exception while retrieving property type model for '%1$s' on '%2$s'.")
	SearchException errorRetrievingPropertyTypeModel(String propertyModelName, @FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> parentTypeModel, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 13, value = "Mass indexing was interrupted; index will be left in unknown state!")
	SearchException massIndexingThreadInterrupted(@Cause InterruptedException e);

	@Message(id = ID_OFFSET_2 + 15,
			value = "Invalid reflection strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidReflectionStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@Message(id = ID_OFFSET_2 + 16, value = "Error trying to access Hibernate ORM session." )
	SearchException hibernateSessionAccessError(@Cause IllegalStateException cause);

	@Message(id = ID_OFFSET_2 + 17, value = "Underlying Hibernate ORM Session seems to be closed." )
	SearchException hibernateSessionIsClosed(@Cause IllegalStateException cause);

	@Message(id = ID_OFFSET_2 + 18,
			value = "Invalid automatic indexing synchronization strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidAutomaticIndexingSynchronizationStrategyName(String invalidRepresentation, List<String> validRepresentations);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET_2 + 19,
			value = "The entity loader for '%1$s' will ignore the cache lookup strategy '%2$s',"
					+ " because document IDs are distinct from entity IDs "
					+ "and thus cannot be used for persistence context or second level cache lookups.")
	void skippingPreliminaryCacheLookupsForNonEntityIdEntityLoader(String entityName,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET_2 + 20,
			value = "The entity loader for '%1$s' will ignore the second-level cache "
					+ " even though it was instructed to use it,"
					+ " because caching is not enabled for this entity type.")
	void skippingSecondLevelCacheLookupsForNonCachedEntityTypeEntityLoader(String entityName);

	@Message(id = ID_OFFSET_2 + 21, value = "Error trying to access Hibernate ORM session factory." )
	SearchException hibernateSessionFactoryAccessError(@Cause IllegalStateException cause);

	@Message(id = ID_OFFSET_2 + 22, value = "Indexing failure: %1$s.\nThe following entities may not have been updated correctly in the index: %2$s." )
	SearchException indexingFailure(String causeMessage, List<EntityReference> failingEntities, @Cause Throwable cause);

	@Message(value = "Automatic indexing of Hibernate ORM entities")
	String automaticIndexing();

	@Message(id = ID_OFFSET_2 + 23, value = "Automatic indexing failed before transaction completion: %1$s" )
	SearchException synchronizationBeforeTransactionFailure(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET_2 + 24, value = "Automatic indexing failed after transaction completion: %1$s" )
	SearchException synchronizationAfterTransactionFailure(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET_2 + 25, value = "Exception while handling transactions: %1$s")
	SearchException massIndexingTransactionHandlingException(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET_2 + 26, value = "%1$s entities could not be indexed. See the logs for details."
			+ " First failure on entity '%2$s': %3$s")
	SearchException massIndexingEntityFailures(long finalFailureCount,
			EntityReference firstFailureEntity, String firstFailureMessage,
			@Cause Throwable firstFailure);

	@Message(id = ID_OFFSET_2 + 27,
			value = "Unknown type: '%1$s'. Available named types: %2$s."
					+ " For entity types, the correct type name is the entity name."
					+ " For component types (embeddeds, ...) in dynamic-map entities,"
					+ " the correct type name is name of the owner entity"
					+ " followed by a dot ('.') followed by the dot-separated path to the component,"
					+ " e.g. 'MyEntity.myEmbedded' or 'MyEntity.myEmbedded.myNestedEmbedded'."
	)
	SearchException unknownNamedType(String typeName, Collection<String> availableNamedTypes);

	@Message(id = ID_OFFSET_2 + 28,
			value = "Unknown entity name: '%1$s'. Available entity names: %2$s."
	)
	SearchException invalidEntityName(String entityName, Collection<String> availableEntityNames);

	@Message(id = ID_OFFSET_2 + 29,
			value = "Invalid type for '%1$s': expected the entity to extend '%2$s'," +
					" but entity type '%3$s' does not."
	)
	SearchException invalidEntitySuperType(String entityName,
			@FormatWith(ClassFormatter.class) Class<?> expectedSuperType,
			@FormatWith(ClassFormatter.class) Class<?> actualJavaType);

	@Message(id = ID_OFFSET_2 + 30,
			value = "Type '%1$s' doesn't have any representation in the JPA metamodel."
					+ " As a result, Hibernate Search cannot use the Criteria API to automatically build queries targeting this type."
					+ " This means in particular that this type cannot:"
					+ " 1) be mass-indexed or 2) set its document ID to a property that is not its entity ID."
	)
	SearchException nonJpaEntityType(PojoRawTypeIdentifier<?> typeIdentifier);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET_2 + 31,
			value = "The mass indexing failure handler threw an exception while handling a previous failure."
					+ " The failure may not have been reported.")
	void failureInMassIndexingFailureHandler(@Cause Throwable t);

	@Message(id = ID_OFFSET_2 + 32, value = "Invalid schema management strategy name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidSchemaManagementStrategyName(String invalidRepresentation, List<String> validRepresentations);
}
