/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.log.impl;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for event contexts in the JavaBean mapper.
 */
@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface JavaBeanEventContextMessages {

	@Message(value = "JavaBean mapping")
	String mapping();

}
