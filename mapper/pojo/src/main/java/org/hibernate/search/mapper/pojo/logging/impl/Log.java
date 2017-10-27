/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.pojo.logging.impl;

import org.hibernate.search.util.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH-POJO")
public interface Log extends BasicLogger {

	@Message(id = 1, value = "Cannot search on an empty target."
			+ " If you want to target all indexes, put Object.class in the collection of target types,"
			+ " or use the method of the same name, but without Class<?> parameters."
	)
	SearchException cannotSearchOnEmptyTarget();

}
