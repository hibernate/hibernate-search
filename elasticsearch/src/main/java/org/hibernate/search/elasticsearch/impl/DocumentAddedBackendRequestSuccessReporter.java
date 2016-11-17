/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.elasticsearch.client.impl.BackendRequestSuccessReporter;

import io.searchbox.core.BulkResult.BulkResultItem;
import io.searchbox.core.DocumentResult;


/**
 * @author Yoann Rodiere
 */
class DocumentAddedBackendRequestSuccessReporter implements BackendRequestSuccessReporter<DocumentResult> {

	public static final DocumentAddedBackendRequestSuccessReporter INSTANCE = new DocumentAddedBackendRequestSuccessReporter();

	private DocumentAddedBackendRequestSuccessReporter() {
		// Private constructor
	}

	@Override
	public void report(DocumentResult result, IndexingMonitor monitor) {
		monitor.documentsAdded( 1L );
	}

	@Override
	public void report(BulkResultItem bulkResultItem, IndexingMonitor monitor) {
		monitor.documentsAdded( 1L );
	}

}
