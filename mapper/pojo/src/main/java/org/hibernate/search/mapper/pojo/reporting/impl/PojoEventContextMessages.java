/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.reporting.impl;

import org.hibernate.search.util.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for event contexts in the POJO mapper.
 */
@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface PojoEventContextMessages {

	@Message(value = "path '%1$s'")
	String path(String pathString);

	@Message(value = "annotation '%1$s'")
	String annotation(String annotationString);

}
