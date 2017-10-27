/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.logging.impl;

import java.util.Collection;

import org.hibernate.search.util.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH-ORM")
public interface Log extends BasicLogger {

	@Message(id = 1, value = "Hibernate Search was not initialized.")
	SearchException hibernateSearchNotInitialized();

	@Message(id = 2, value = "Unexpected entity type for a search hit: %1$s. Expected one of %2$s.")
	SearchException unexpectedSearchHitType(Class<?> entityType, Collection<? extends Class<?>> expectedTypes);

	@Message(id = 3, value = "Unknown indexing mode: %1$s")
	SearchException unknownIndexingMode(String indexingMode);

	@Message(id = 4, value = "Cannot create mapping for non-entity type: %1$s")
	SearchException cannotMapNonEntityType(Class<?> type);
}
