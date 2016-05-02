/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

/**
 * Represents a group of backend requests, which may either be backed by an actual bulk request or by a single
 * request "pseudo group". Allows for uniform handling of these two cases.
 */
public interface BackendRequestGroup {
	void execute();
	void ensureRefreshed();
	int getSize();
}
