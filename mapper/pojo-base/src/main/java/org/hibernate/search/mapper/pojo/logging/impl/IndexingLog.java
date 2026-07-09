/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.logging.spi.MappableTypeModelFormatter;
import org.hibernate.search.mapper.pojo.common.annotation.impl.SearchProcessingWithContextException;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = IndexingLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface IndexingLog {
	String CATEGORY_NAME = "org.hibernate.search.indexing.mapper";

	IndexingLog INSTANCE = LoggerFactory.make( IndexingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------


	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	// Not using a message ID: this exception is just a simple wrapper
	@Message(value = "%1$s")
	SearchProcessingWithContextException searchProcessingFailure(@Cause Throwable cause, String causeMessage,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 38, value = "The entity identifier must not be null.")
	SearchException nullProvidedIdentifier();

	@Message(id = ID_OFFSET + 60,
			value = "Unable to trigger entity processing while already processing entities."
					+ " Make sure you do not change entities within an entity getter or a custom bridge used for indexing,"
					+ " and avoid any event that could trigger entity processing."
					+ " Hibernate ORM flushes, in particular, must be avoided in entity getters and bridges.")
	SearchException recursiveIndexingPlanProcess();

	@Message(id = ID_OFFSET + 83,
			value = "Exception while building document for entity '%1$s': %2$s")
	SearchException errorBuildingDocument(EntityReference entityReference, String message, @Cause Exception e);

	@Message(id = ID_OFFSET + 84,
			value = "Exception while resolving other entities to reindex as a result of changes on entity '%1$s': %2$s")
	SearchException errorResolvingEntitiesToReindex(EntityReference entityReference, String message, @Cause Exception e);

	@Message(id = ID_OFFSET + 87, value = "Invalid indexing request:"
			+ " if the entity is null, the identifier must be provided explicitly.")
	SearchException nullProvidedIdentifierAndEntity();

	@Message(id = ID_OFFSET + 88, value = "Invalid indexing request:"
			+ " the add and update operations require a non-null entity.")
	SearchException nullEntityForIndexerAddOrUpdate();

	@Message(id = ID_OFFSET + 124,
			value = "Indexing failure: %1$s.\nThe following entities may not have been updated correctly in the index: %2$s.")
	SearchException indexingFailure(String causeMessage, List<?> failingEntities, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 128,
			value = "Indexing plan for '%1$s' cannot be created as this type is excluded by the indexing plan filter.")
	SearchException attemptToCreateIndexingPlanForExcludedType(PojoRawTypeIdentifier<?> typeIdentifier);

	@Message(id = ID_OFFSET + 129,
			value = "'%1$s' cannot be included and excluded at the same time within one filter. " +
					"Already included types: '%2$s'. " +
					"Already excluded types: '%3$s'.")
	SearchException indexingPlanFilterCannotIncludeExcludeSameType(PojoRawTypeIdentifier<?> typeIdentifier,
			Set<PojoRawTypeIdentifier<?>> includes, Set<PojoRawTypeIdentifier<?>> excludes);

	@Message(id = ID_OFFSET + 155,
			value = "Cannot load entities of type '%s': no selection loading strategy registered for this type.")
	SearchException noSelectionLoadingStrategy(String entityName);

	@Message(id = ID_OFFSET + 156,
			value = "Cannot load entities of type '%s': no mass loading strategy registered for this type.")
	SearchException noMassLoadingStrategy(String entityName);

	@Message(id = ID_OFFSET + 157,
			value = "Type mismatch when applying loading binder to type '%1$s': the binder expects the entity type to extend '%2$s',"
					+ " but entity type '%1$s' does not."
	)
	SearchException loadingConfigurationTypeMismatch(
			@FormatWith(MappableTypeModelFormatter.class) PojoRawTypeModel<?> entityType,
			@FormatWith(ClassFormatter.class) Class<?> expectedSuperType);

}
