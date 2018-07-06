/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.javabean.log.impl;

import org.hibernate.search.mapper.pojo.logging.spi.PojoTypeModelFormatter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH-JAVABEAN")
public interface Log extends BasicLogger {

	@Message(id = 1, value = "Unable to find property '%2$s' on type '%1$s'.")
	SearchException cannotFindProperty(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			String propertyName);

	@Message(id = 2, value = "Cannot read property '%2$s' on type '%1$s'.")
	SearchException cannotReadProperty(@FormatWith(PojoTypeModelFormatter.class) PojoRawTypeModel<?> typeModel,
			String propertyName);

}
