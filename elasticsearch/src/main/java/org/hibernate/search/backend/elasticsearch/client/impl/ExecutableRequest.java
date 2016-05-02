/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.util.Set;

/**
 * A request which is executed against the Elasticsarch backend. May either be backed by a bulk request or by a single
 * request. Allows for uniform handling of these two cases.
 */
public interface ExecutableRequest {
	void execute();
	int getSize();

	/**
	 * Returns the names of the indexes touched by this request.
	 */
	Set<String> getTouchedIndexes();

	/**
	 * Returns the names of the indexes which got refreshed during executing this request, i.e. no subsequent refresh is needed.
	 */
	Set<String> getRefreshedIndexes();
}
