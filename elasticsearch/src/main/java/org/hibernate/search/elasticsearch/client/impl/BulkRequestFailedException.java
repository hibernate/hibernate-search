/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.exception.SearchException;

import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult.BulkResultItem;

/**
 * A failure during applying a bulk of index changes. Provides access to the failed requests and in turn Lucene works.
 *
 * @author Gunnar Morling
 */
public class BulkRequestFailedException extends SearchException {

	private final Map<BackendRequest<?>, BulkResultItem> successfulItems;

	private final List<BackendRequest<?>> erroneousItems;

	public BulkRequestFailedException(String message, Map<BackendRequest<?>, BulkResultItem> successfulItems,
			List<BackendRequest<? extends JestResult>> erroneousItems) {
		super( message );
		this.successfulItems = Collections.unmodifiableMap( successfulItems );
		this.erroneousItems = Collections.unmodifiableList( erroneousItems );
	}

	public Map<BackendRequest<?>, BulkResultItem> getSuccessfulItems() {
		return successfulItems;
	}

	public List<BackendRequest<?>> getErroneousItems() {
		return erroneousItems;
	}
}
