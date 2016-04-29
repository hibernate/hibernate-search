/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.exception.SearchException;

import io.searchbox.client.JestResult;

/**
 * A failure during applying a bulk of index changes. Provides access to the failed requests and in turn Lucene works.
 *
 * @author Gunnar Morling
 */
public class BulkRequestFailedException extends SearchException {

	private final List<BackendRequest<?>> erroneousItems;

	public BulkRequestFailedException(String message, List<BackendRequest<? extends JestResult>> erroneousItems) {
		super( message );
		this.erroneousItems = Collections.unmodifiableList( erroneousItems );
	}

	public List<BackendRequest<?>> getErroneousItems() {
		return erroneousItems;
	}
}
