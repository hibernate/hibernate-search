/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

public interface IndexingWork<T> extends BulkableWork<T> {

	/**
	 * @return A string that will be used to route the work to a specific queue.
	 * Never {@code null}.
	 * Works that must be executed in the same relative order they were submitted in
	 * (i.e. works pertaining to the same document) should return the same string.
	 */
	String getQueuingKey();

}
