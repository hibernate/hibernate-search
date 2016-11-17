/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.elasticsearch.client.impl.BackendRequestSuccessReporter;

import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult.BulkResultItem;


/**
 * @author Yoann Rodiere
 */
public class NoopBackendRequestSuccessHandler implements BackendRequestSuccessReporter<JestResult> {

	public static final NoopBackendRequestSuccessHandler INSTANCE = new NoopBackendRequestSuccessHandler();

	private NoopBackendRequestSuccessHandler() {
		// Private constructor
	}

	@Override
	public void report(JestResult result, IndexingMonitor monitor) {
		// Do nothing
	}

	@Override
	public void report(BulkResultItem bulkResultItem, IndexingMonitor monitor) {
		// Do nothing
	}

}
