/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.javabean.log.impl;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;

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

	@Message(id = ID_OFFSET + 3, value = "Exception while retrieving the type model for '%1$s'.")
	SearchException errorRetrievingTypeModel(@FormatWith(ClassFormatter.class) Class<?> clazz, @Cause Exception cause);

	@Message(id = ID_OFFSET + 5,
			value = "The JavaBean mapper cannot load entities,"
					+ " but there was an attempt to load the entity corresponding to document '%1$s'."
					+ " There is probably an entity projection in the query definition: it should be removed."
	)
	SearchException cannotLoadEntity(DocumentReference reference);

	@Message(id = ID_OFFSET + 6, value = "Multiple entity types configured with the same name '%1$s': '%2$s', '%3$s'")
	SearchException multipleEntityTypesWithSameName(String entityName, Class<?> previousType, Class<?> type);

	@Message(id = ID_OFFSET + 7,
			value = "The JavaBean mapper does not support named types. The type with name '%1$s' does not exist."
	)
	SearchException namedTypesNotSupported(String name);

	@Message(id = ID_OFFSET + 8,
			value = "The JavaBean mapper cannot load entities,"
					+ " but there was an attempt to configure entity loading."
	)
	SearchException entityLoadingConfigurationNotSupported();

	@Message(id = ID_OFFSET + 9, value = "Type '%1$s' is not an entity type, or the entity is not indexed.")
	SearchException notIndexedEntityType(@FormatWith(ClassFormatter.class) Class<?> type);

	@Message(id = ID_OFFSET + 10, value = "Entity '%1$s' does not exist or is not indexed.")
	SearchException notIndexedEntityName(String name);
}
