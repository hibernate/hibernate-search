/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.log.impl;

import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRange(min = MessageConstants.MAPPER_JAVABEAN_ID_RANGE_MIN, max = MessageConstants.MAPPER_JAVABEAN_ID_RANGE_MAX)
public interface Log extends BasicLogger {

	int ID_OFFSET = MessageConstants.MAPPER_JAVABEAN_ID_RANGE_MIN;

	@Message(id = ID_OFFSET + 3, value = "Unable to retrieve type model for class '%1$s'.")
	SearchException errorRetrievingTypeModel(@FormatWith(ClassFormatter.class) Class<?> clazz, @Cause Exception cause);

	@Message(id = ID_OFFSET + 6, value = "Multiple entity types configured with the same name '%1$s': '%2$s', '%3$s'")
	SearchException multipleEntityTypesWithSameName(String entityName, Class<?> previousType, Class<?> type);

	@Message(id = ID_OFFSET + 7,
			value = "Type with name '%1$s' does not exist: the JavaBean mapper does not support named types."
	)
	SearchException namedTypesNotSupported(String name);

	@Message(id = ID_OFFSET + 8,
			value = "Unable to set up entity loading for type '%s', because no entity loader was registered for this type.")
	SearchException entityLoaderNotRegistered(PojoRawTypeIdentifier<?> typeIdentifier);

	@Message(id = ID_OFFSET + 9, value = "Type '%1$s' is not an entity type, or this entity type is not indexed.")
	SearchException notIndexedEntityType(@FormatWith(ClassFormatter.class) Class<?> type);

	@Message(id = ID_OFFSET + 10, value = "Entity type '%1$s' does not exist or is not indexed.")
	SearchException notIndexedEntityName(String name);

	@Message(id = ID_OFFSET + 11,
			value = "Invalid String value for the bean provider: '%s'. The bean provider must be an instance of '%s'.")
	SearchException invalidStringForBeanProvider(String value, Class<BeanProvider> expectedType);

	@Message(id = ID_OFFSET + 12,
			value = "Unable to set up entity loading for type '%s', because no entity loading strategy was registered for this type.")
	SearchException entityLoadingStrategyNotRegistered(PojoRawTypeIdentifier<?> typeIdentifier);

	@Message(id = ID_OFFSET + 13, value = "Context is interrupted while indexing entity '%1$s'. Consider increasing the connection time-out.")
	SearchException contextInterruptedWhileProducingIdsForBatchIndexing(String entityName);

	@Message(id = ID_OFFSET + 14, value = "Unable to access search session: %1$s")
	SearchException hibernateSessionAccessError(String causeMessage);

}
