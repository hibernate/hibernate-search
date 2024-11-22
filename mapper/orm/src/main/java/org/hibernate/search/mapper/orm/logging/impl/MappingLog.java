/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.logging.impl;

import static org.hibernate.search.mapper.orm.logging.impl.OrmLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.hibernate.mapping.Value;
import org.hibernate.search.mapper.pojo.logging.spi.PojoModelPathFormatter;
import org.hibernate.search.mapper.pojo.logging.spi.PojoTypeModelFormatter;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;
import org.hibernate.search.util.common.logging.impl.EventContextFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = MappingLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface MappingLog {
	String CATEGORY_NAME = "org.hibernate.search.mapping.mapper.orm";

	MappingLog INSTANCE = LoggerFactory.make( MappingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

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

	@Message(id = ID_OFFSET + 12, value = "Unable to retrieve property type model for '%1$s' on '%2$s': %3$s")
	SearchException errorRetrievingPropertyTypeModel(String propertyModelName,
			@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> parentTypeModel,
			String causeMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET + 27,
			value = "Unknown type: '%1$s'. Available named types: %2$s."
					+ " For entity types, the correct type name is the entity name."
					+ " For component types (embeddeds, ...) in dynamic-map entities,"
					+ " the correct type name is name of the owner entity"
					+ " followed by a dot ('.') followed by the dot-separated path to the component,"
					+ " e.g. 'MyEntity.myEmbedded' or 'MyEntity.myEmbedded.myNestedEmbedded'."
	)
	SearchException unknownNamedType(String typeName, Collection<String> availableNamedTypes);

	@Message(id = ID_OFFSET + 33, value = "No matching indexed entity type for class '%1$s'."
			+ " Either this class is not an entity type, or the entity type is not indexed in Hibernate Search."
			+ " Valid classes for indexed entity types are: %2$s")
	SearchException unknownClassForIndexedEntityType(@FormatWith(ClassFormatter.class) Class<?> invalidClass,
			@FormatWith(CommaSeparatedClassesFormatter.class) Collection<Class<?>> validClasses);

	@Message(id = ID_OFFSET + 34, value = "No matching indexed entity type for name '%1$s'."
			+ " Either this is not the name of an entity type, or the entity type is not indexed in Hibernate Search."
			+ " Valid names for indexed entity types are: %2$s")
	SearchException unknownEntityNameForIndexedEntityType(String invalidName, Collection<String> validNames);

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

	@Message(id = ID_OFFSET + 121,
			value = "An unexpected failure occurred while resolving the representation of path '%1$s' in the entity state array,"
					+ " which is necessary to configure resolution of association inverse side for reindexing."
					+ " Cannot proceed further as this may lead to incomplete reindexing and thus out-of-sync indexes."
					+ " Failure: %3$s"
					+ " %2$s") // Context
	SearchException failedToResolveStateRepresentation(String path,
			@FormatWith(EventContextFormatter.class) EventContext context,
			String causeMessage,
			@Cause Exception cause);
}
