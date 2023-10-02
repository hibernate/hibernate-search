/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.logging.impl;

import java.util.Collection;
import java.util.List;

import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.CommaSeparatedClassesFormatter;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRange(min = MessageConstants.MAPPER_POJO_STANDALONE_ID_RANGE_MIN,
		max = MessageConstants.MAPPER_POJO_STANDALONE_ID_RANGE_MAX)
public interface Log extends BasicLogger {

	int ID_OFFSET = MessageConstants.MAPPER_POJO_STANDALONE_ID_RANGE_MIN;

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

	@Message(id = ID_OFFSET + 11,
			value = "Invalid String value for the bean provider: '%s'. The bean provider must be an instance of '%s'.")
	SearchException invalidStringForBeanProvider(String value, Class<BeanProvider> expectedType);

	@Message(id = ID_OFFSET + 14, value = "Unable to access search session: %1$s")
	SearchException hibernateSessionAccessError(String causeMessage);

	@Message(id = ID_OFFSET + 15, value = "Invalid schema management strategy name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidSchemaManagementStrategyName(String invalidRepresentation,
			List<String> validRepresentations);

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
