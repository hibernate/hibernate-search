/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.backend.IndexingMonitor;

import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult.BulkResultItem;

/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchWorkSuccessReporter<T extends JestResult> {

	/**
	 * Reports the given detailed result to the given monitor.
	 * @param result The detailed result.
	 * @param monitor The monitor to report results to.
	 */
	void report(T result, IndexingMonitor monitor);

	/**
	 * Reports the given summary result to the given monitor.
	 * @param bulkResultItem The summary result.
	 * @param monitor The monitor to report results to.
	 */
	void report(BulkResultItem bulkResultItem, IndexingMonitor monitor);

}
