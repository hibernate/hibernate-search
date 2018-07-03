/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for failure contexts in the POJO mapper.
 */
@MessageBundle(projectCode = "HSEARCH")
public interface PojoFailureContextMessages {

	@Message(value = "path '%1$s'")
	String path(String pathString);

	@Message(value = "annotation '%1$s'")
	String annotation(String annotationString);

}
