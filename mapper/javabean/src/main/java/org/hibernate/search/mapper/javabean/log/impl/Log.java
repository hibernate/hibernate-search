/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.javabean.log.impl;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.mapper.pojo.logging.spi.PojoTypeModelFormatter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
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

	int ID_OFFSET_1 = MessageConstants.MAPPER_JAVABEAN_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_1 + 1,
			value = "Unable to find property '%2$s' on type '%1$s'.")
	SearchException cannotFindProperty(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			String propertyName);

	@Message(id = ID_OFFSET_1 + 2,
			value = "Cannot read property '%2$s' on type '%1$s'.")
	SearchException cannotReadProperty(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			String propertyName);

	@Message(id = ID_OFFSET_1 + 3, value = "Exception while retrieving the type model for '%1$s'.")
	SearchException errorRetrievingTypeModel(@FormatWith(ClassFormatter.class) Class<?> clazz, @Cause Exception cause);

	@Message(id = ID_OFFSET_1 + 4, value = "Exception while retrieving property type model for '%1$s' on '%2$s'")
	SearchException errorRetrievingPropertyTypeModel(String propertyModelName, @FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> parentTypeModel, @Cause Exception cause);

	@Message(id = ID_OFFSET_1 + 5,
			value = "The JavaBean mapper cannot load entities,"
					+ " but there was an attempt to load the entity corresponding to document '%1$s'."
					+ " There is probably an entity projection in the query definition: it should be removed."
	)
	SearchException cannotLoadEntity(DocumentReference reference);
}
