/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.exception.SearchException;

import io.searchbox.core.BulkResult.BulkResultItem;

/**
 * A failure during applying a bulk of index changes. Provides access to the failed requests and in turn Lucene works.
 *
 * @author Gunnar Morling
 */
public class BulkRequestFailedException extends SearchException {

	private final Map<BulkableElasticsearchWork<?>, BulkResultItem> successfulItems;

	private final List<BulkableElasticsearchWork<?>> erroneousItems;

	public BulkRequestFailedException(String message, Map<BulkableElasticsearchWork<?>, BulkResultItem> successfulItems,
			List<BulkableElasticsearchWork<?>> erroneousItems) {
		super( message );
		this.successfulItems = Collections.unmodifiableMap( successfulItems );
		this.erroneousItems = Collections.unmodifiableList( erroneousItems );
	}

	public Map<BulkableElasticsearchWork<?>, BulkResultItem> getSuccessfulItems() {
		return successfulItems;
	}

	public List<BulkableElasticsearchWork<?>> getErroneousItems() {
		return erroneousItems;
	}
}
