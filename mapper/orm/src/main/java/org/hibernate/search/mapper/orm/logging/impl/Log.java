/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.mapper.orm.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.ScrollMode;
import org.hibernate.SessionFactory;
import org.hibernate.mapping.Value;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.search.engine.environment.bean.spi.BeanNotFoundException;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.pojo.logging.spi.PojoModelPathFormatter;
import org.hibernate.search.mapper.pojo.logging.spi.PojoTypeModelFormatter;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;
import org.hibernate.search.util.common.logging.impl.EventContextFormatter;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

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
		@ValidIdRange(min = MessageConstants.MAPPER_ORM_ID_RANGE_MIN, max = MessageConstants.MAPPER_ORM_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5 (engine module)
		@ValidIdRange(min = 34, max = 34),
		@ValidIdRange(min = 36, max = 36),
		@ValidIdRange(min = 39, max = 39),
		@ValidIdRange(min = 62, max = 62),
		@ValidIdRange(min = 116, max = 116),
		@ValidIdRange(min = 183, max = 183),
		@ValidIdRange(min = 211, max = 212),
		@ValidIdRange(min = 276, max = 276),
		@ValidIdRange(min = 348, max = 349)
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY_ENGINE = MessageConstants.ENGINE_ID_RANGE_MIN;

	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 34, value = "Hibernate Search version %1$s")
	void version(String versionString);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 36,
			value = "Unable to guess the transaction status: not starting a JTA transaction.")
	void cannotGuessTransactionStatus(@Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 39, value = "Unable to properly close scroll in ScrollableResults.")
	void unableToCloseSearcherInScrollableResult(@Cause Exception e);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 276, value = "No transaction active. Consider increasing the connection time-out.")
	SearchException transactionNotActiveWhileProducingIdsForBatchIndexing();

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET = MessageConstants.MAPPER_ORM_ID_RANGE_MIN;

	@Message(id = ID_OFFSET + 1,
			value = "Hibernate Search was not initialized.")
	SearchException hibernateSearchNotInitialized();

	@Message(id = ID_OFFSET + 3,
			value = "Invalid automatic indexing strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidAutomaticIndexingStrategyName(String invalidRepresentation,
			List<String> validRepresentations);

	@Message(id = ID_OFFSET + 7,
			value = "Unable to resolve path '%1$s' to a persisted attribute in Hibernate ORM metadata."
					+ " If this path points to a transient attribute, use @IndexingDependency(derivedFrom = ...)"
					+ " to specify which persisted attributes it is derived from."
					+ " See the reference documentation for more information.")
	SearchException unknownPathForDirtyChecking(@FormatWith(PojoModelPathFormatter.class) PojoModelPath path,
			@Cause Exception e);

	@Message(id = ID_OFFSET + 8,
			value = "Path '%1$s' points to attribute '%2$s' that will never be reported as dirty by Hibernate ORM."
					+ " Check that you didn't declare an invalid indexing dependency.")
	SearchException unreportedPathForDirtyChecking(@FormatWith(PojoModelPathFormatter.class) PojoModelPath path,
			Value value);

	@Message(id = ID_OFFSET + 9,
			value = "Unable to apply container value extractor with name '%2$s' to"
					+ " Hibernate ORM metadata node of type '%1$s'.")
	SearchException invalidContainerExtractorForDirtyChecking(Class<?> ormMappingClass, String extractorName);

	@Message(id = ID_OFFSET + 11,
			value = "Unable to create a SearchSession for sessions created using a different session factory."
					+ " Expected: '%1$s'. In use: '%2$s'.")
	SearchException usingDifferentSessionFactories(SessionFactory expectedSessionFactory,
			SessionFactory usedSessionFactory);

	@Message(id = ID_OFFSET + 12, value = "Unable to retrieve property type model for '%1$s' on '%2$s': %3$s")
	SearchException errorRetrievingPropertyTypeModel(String propertyModelName,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> parentTypeModel,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 16, value = "Unable to access Hibernate ORM session: %1$s")
	SearchException hibernateSessionAccessError(String causeMessage, @Cause IllegalStateException cause);

	@Message(id = ID_OFFSET + 17, value = "Underlying Hibernate ORM Session is closed.")
	SearchException hibernateSessionIsClosed(@Cause IllegalStateException cause);

	@Message(id = ID_OFFSET + 18,
			value = "Invalid entity loading cache lookup strategy name: '%1$s'. Valid names are: %2$s.")
	SearchException invalidEntityLoadingCacheLookupStrategyName(String invalidRepresentation,
			List<String> validRepresentations);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 19,
			value = "The entity loader for '%1$s' is ignoring the cache lookup strategy '%2$s',"
					+ " because document IDs are distinct from entity IDs"
					+ " and thus cannot be used for persistence context or second level cache lookups.")
	void skippingPreliminaryCacheLookupsForNonEntityIdEntityLoader(String entityName,
			EntityLoadingCacheLookupStrategy cacheLookupStrategy);

	@LogMessage(level = DEBUG)
	@Message(id = ID_OFFSET + 20,
			value = "The entity loader for '%1$s' is ignoring the second-level cache"
					+ " even though it was instructed to use it,"
					+ " because caching is not enabled for this entity type.")
	void skippingSecondLevelCacheLookupsForNonCachedEntityTypeEntityLoader(String entityName);

	@Message(id = ID_OFFSET + 21, value = "Unable to access Hibernate ORM session factory: %1$s")
	SearchException hibernateSessionFactoryAccessError(String causeMessage, @Cause IllegalStateException cause);

	@Message(id = ID_OFFSET + 22,
			value = "Indexing failure: %1$s.\nThe following entities may not have been updated correctly in the index: %2$s.")
	SearchException indexingFailure(String causeMessage, List<?> failingEntities, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 23, value = "Unable to process entities for indexing before transaction completion: %1$s")
	SearchException synchronizationBeforeTransactionFailure(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 24, value = "Unable to index documents for indexing after transaction completion: %1$s")
	SearchException synchronizationAfterTransactionFailure(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 25, value = "Unable to handle transaction: %1$s")
	SearchException transactionHandlingException(String causeMessage, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 27,
			value = "Unknown type: '%1$s'. Available named types: %2$s."
					+ " For entity types, the correct type name is the entity name."
					+ " For component types (embeddeds, ...) in dynamic-map entities,"
					+ " the correct type name is name of the owner entity"
					+ " followed by a dot ('.') followed by the dot-separated path to the component,"
					+ " e.g. 'MyEntity.myEmbedded' or 'MyEntity.myEmbedded.myNestedEmbedded'."
	)
	SearchException unknownNamedType(String typeName, Collection<String> availableNamedTypes);

	@Message(id = ID_OFFSET + 32, value = "Invalid schema management strategy name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidSchemaManagementStrategyName(String invalidRepresentation,
			List<String> validRepresentations);

	@Message(id = ID_OFFSET + 33, value = "No matching indexed entity type for class '%1$s'."
			+ " Either this class is not an entity type, or the entity type is not indexed in Hibernate Search."
			+ " Valid classes for indexed entity types are: %2$s")
	SearchException unknownClassForIndexedEntityType(@FormatWith(ClassFormatter.class) Class<?> invalidClass,
			@FormatWith(CommaSeparatedClassesFormatter.class) Collection<Class<?>> validClasses);

	@Message(id = ID_OFFSET + 34, value = "No matching indexed entity type for name '%1$s'."
			+ " Either this is not the name of an entity type, or the entity type is not indexed in Hibernate Search."
			+ " Valid names for indexed entity types are: %2$s")
	SearchException unknownEntityNameForIndexedEntityType(String invalidName, Collection<String> validNames);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET + 35, value = "Unable to shut down Hibernate Search:")
	void shutdownFailed(@Cause Throwable cause);

	@Message(id = ID_OFFSET + 36, value = "Cannot use scroll() with scroll mode '%1$s' with Hibernate Search queries:"
			+ " only ScrollMode.FORWARDS_ONLY is supported.")
	SearchException canOnlyUseScrollWithScrollModeForwardsOnly(ScrollMode scrollMode);

	@Message(id = ID_OFFSET + 37, value = "Cannot scroll backwards with Hibernate Search scrolls: they are forwards-only."
			+ " Ensure you always increment the scroll position, and never decrement it.")
	SearchException cannotScrollBackwards();

	@Message(id = ID_OFFSET + 38, value = "Cannot set the scroll position relative to the end with Hibernate Search scrolls."
			+ " Ensure you always pass a positive number to setRowNumber().")
	SearchException cannotSetScrollPositionRelativeToEnd();

	@Message(id = ID_OFFSET + 39, value = "Cannot use this ScrollableResults instance: it is closed.")
	SearchException cannotUseClosedScrollableResults();

	@Message(id = ID_OFFSET + 40, value = "Multiple instances of entity type '%1$s' have their property '%2$s' set to '%3$s'."
			+ " '%2$s' is the document ID and must be assigned unique values.")
	SearchException foundMultipleEntitiesForDocumentId(String entityName, String documentIdSourcePropertyName,
			Object id);

	@Message(id = ID_OFFSET + 41, value = "No such bean in bean container '%1$s'.")
	BeanNotFoundException beanNotFoundInBeanContainer(BeanContainer beanContainer);

	@Message(id = ID_OFFSET + 42, value = "Cannot customize the indexing plan synchronization strategy: "
			+ " the selected coordination strategy always processes events asynchronously, through a queue.")
	SearchException cannotConfigureSynchronizationStrategyWithIndexingEventQueue();

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 53, value = "Configuration property '%1$s' is deprecated; use '%2$s' instead.")
	void deprecatedPropertyUsedInsteadOfNew(String resolveOrRaw, String resolveOrRaw1);

	@Message(id = ID_OFFSET + 54, value = "Cannot determine the set of all possible tenant identifiers."
			+ " You must provide this information by setting configuration property '%1$s'"
			+ " to a comma-separated string containing all possible tenant identifiers.")
	SearchException missingTenantIdConfiguration(String tenantIdsConfigurationPropertyKey);

	@Message(id = ID_OFFSET + 55, value = "Cannot target tenant '%1$s' because this tenant identifier"
			+ " was not listed in the configuration provided on startup."
			+ " To target this tenant, you must provide the tenant identifier through configuration property '%3$s',"
			+ " which should be set to a comma-separated string containing all possible tenant identifiers."
			+ " Currently configured tenant identifiers: %2$s.")
	SearchException invalidTenantId(String tenantId, Set<String> allTenantIds, String tenantIdsConfigurationPropertyKey);

	@LogMessage(level = Logger.Level.INFO)
	@Message(id = ID_OFFSET + 56, value = "Ignoring unrecognized query hint [%s]")
	void ignoringUnrecognizedQueryHint(String hintName);

	@Message(id = ID_OFFSET + 57,
			value = "Cannot set the fetch size of Hibernate Search ScrollableResults after having created them."
					+ " If you want to define the size of batches for entity loading, set loading options when defining the query instead,"
					+ " for example with .loading(o -> o.fetchSize(50))."
					+ " See the reference documentation for more information.")
	SearchException cannotSetFetchSize();

	@Message(id = ID_OFFSET + 59, value = "No matching indexed entity type for type identifier '%1$s'."
			+ " Either this type is not an entity type, or the entity type is not indexed in Hibernate Search."
			+ " Valid identifiers for indexed entity types are: %2$s")
	SearchException unknownTypeIdentifierForIndexedEntityType(PojoRawTypeIdentifier<?> invalidTypeId,
			Collection<PojoRawTypeIdentifier<?>> validTypeIds);

	@Message(id = ID_OFFSET + 60, value = "No matching entity type for class '%1$s'."
			+ " Either this class is not an entity type, or the entity type is not mapped in Hibernate Search."
			+ " Valid classes for mapped entity types are: %2$s")
	SearchException unknownClassForMappedEntityType(@FormatWith(ClassFormatter.class) Class<?> invalidClass,
			@FormatWith(CommaSeparatedClassesFormatter.class) Collection<Class<?>> validClasses);

	@Message(id = ID_OFFSET + 61, value = "No matching entity type for name '%1$s'."
			+ " Either this is not the name of an entity type, or the entity type is not mapped in Hibernate Search."
			+ " Valid names for mapped entity types are: %2$s")
	SearchException unknownEntityNameForMappedEntityType(String invalidName, Collection<String> validNames);

	@Message(id = ID_OFFSET + 64, value = "No matching entity type for name '%1$s'."
			+ " Either this is not the Hibernate ORM name of an entity type, or the entity type is not mapped in Hibernate Search."
			+ " Valid Hibernate ORM names for mapped entities are: %2$s")
	SearchException unknownHibernateOrmEntityNameForMappedEntityType(String invalidName, Collection<String> validNames);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET + 121,
			value = "An unexpected failure occurred while resolving the representation of path '%1$s' in the entity state array,"
					+ " which is necessary to configure resolution of association inverse side for reindexing."
					+ " This may lead to incomplete reindexing and thus out-of-sync indexes."
					+ " The exception is being ignored to preserve backwards compatibility with earlier versions of Hibernate Search."
					+ " Failure: %3$s"
					+ " %2$s") // Context
	void failedToResolveStateRepresentation(String path, @FormatWith(EventContextFormatter.class) EventContext context,
			String causeMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 122,
			value = "Both '%1$s' and '%2$s' are configured. Use only '%1$s' to set the indexing plan synchronization strategy. ")
	SearchException bothNewAndOldConfigurationPropertiesForIndexingPlanSyncAreUsed(String key1, String key2);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 123, value = "Configuration property '%1$s' is deprecated; use '%2$s' instead.")
	void automaticIndexingSynchronizationStrategyIsDeprecated(String deprecatedProperty, String newProperty);

	@Message(id = ID_OFFSET + 124,
			value = "Unable to apply the given filter at the session level with the outbox polling coordination strategy. " +
					"With this coordination strategy, applying a session-level indexing plan filter is only allowed if it excludes all types.")
	SearchException cannotApplySessionFilterWhenAsyncProcessingIsUsed();

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 125, value = "Configuration property '%1$s' is deprecated. "
			+ "This setting will be removed in a future version. "
			+ "There will be no alternative provided to replace it. "
			+ "After the removal of this property in a future version, "
			+ "a dirty check will always be performed when considering whether to trigger reindexing.")
	void automaticIndexingEnableDirtyCheckIsDeprecated(String deprecatedProperty);

	@Message(id = ID_OFFSET + 126,
			value = "Both '%1$s' and '%2$s' are configured. Use only '%2$s' to enable indexing listeners. ")
	SearchException bothNewAndOldConfigurationPropertiesForIndexingListenersAreUsed(String key1, String key2);
}
