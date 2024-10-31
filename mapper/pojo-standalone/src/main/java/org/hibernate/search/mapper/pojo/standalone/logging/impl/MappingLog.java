/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.logging.impl;

import static org.hibernate.search.mapper.pojo.standalone.logging.impl.StandaloneMapperLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = MappingLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface MappingLog {
	String CATEGORY_NAME = "org.hibernate.search.mapping";

	MappingLog INSTANCE = LoggerFactory.make( MappingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 3, value = "Unable to retrieve type model for class '%1$s'.")
	SearchException errorRetrievingTypeModel(@FormatWith(ClassFormatter.class) Class<?> clazz, @Cause Exception cause);

	@Message(id = ID_OFFSET + 7,
			value = "Type with name '%1$s' does not exist: the standalone POJO mapper does not support named types."
	)
	SearchException namedTypesNotSupported(String name);

	@Message(id = ID_OFFSET + 9, value = "No matching indexed entity type for class '%1$s'."
			+ " Either this class is not an entity type, or the entity type is not indexed in Hibernate Search."
			+ " Valid classes for indexed entity types are: %2$s")
	SearchException unknownClassForIndexedEntityType(@FormatWith(ClassFormatter.class) Class<?> invalidClass,
			@FormatWith(CommaSeparatedClassesFormatter.class) Collection<Class<?>> validClasses);

	@Message(id = ID_OFFSET + 10, value = "No matching indexed entity type for name '%1$s'."
			+ " Either this is not the name of an entity type, or the entity type is not indexed in Hibernate Search."
			+ " Valid names for indexed entity types are: %2$s")
	SearchException unknownEntityNameForIndexedEntityType(String invalidName, Collection<String> validNames);


	@Message(id = ID_OFFSET + 16, value = "No matching entity type for type identifier '%1$s'."
			+ " Either this type is not an entity type, or the entity type is not mapped in Hibernate Search."
			+ " Valid identifiers for mapped entity types are: %2$s")
	SearchException unknownTypeIdentifierForMappedEntityType(PojoRawTypeIdentifier<?> invalidTypeId,
			Collection<PojoRawTypeIdentifier<?>> validTypeIds);

	@Message(id = ID_OFFSET + 17, value = "No matching indexed entity type for type identifier '%1$s'."
			+ " Either this type is not an entity type, or the entity type is not indexed in Hibernate Search."
			+ " Valid identifiers for indexed entity types are: %2$s")
	SearchException unknownTypeIdentifierForIndexedEntityType(PojoRawTypeIdentifier<?> invalidTypeId,
			Collection<PojoRawTypeIdentifier<?>> validTypeIds);

	@Message(id = ID_OFFSET + 18, value = "No matching entity type for class '%1$s'."
			+ " Either this class is not an entity type, or the entity type is not mapped in Hibernate Search."
			+ " Valid classes for mapped entity types are: %2$s")
	SearchException unknownClassForMappedEntityType(@FormatWith(ClassFormatter.class) Class<?> invalidClass,
			@FormatWith(CommaSeparatedClassesFormatter.class) Collection<Class<?>> validClasses);
}
