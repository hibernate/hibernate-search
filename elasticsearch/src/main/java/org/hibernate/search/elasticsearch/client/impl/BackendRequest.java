/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.backend.LuceneWork;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;

/**
 * Represents a JEST request and optionally one or more HTTP response codes which - should they occur - are to be
 * silently discarded. E.g. for DELETE operations it makes sense to ignore 404 errors, i.e. when trying to delete a
 * non-existent document.
 *
 * @author Gunnar Morling
 */
public class BackendRequest<T extends JestResult> {

	private final Action<T> action;
	private final Set<Integer> ignoredErrorStatuses;
	private final LuceneWork luceneWork;
	private final String indexName;
	private final boolean refreshAfterWrite;

	public BackendRequest(Action<T> action, LuceneWork luceneWork, String indexName, boolean refreshAfterWrite, int... ignoredErrorStatuses) {
		this.action = action;
		this.luceneWork = luceneWork;
		this.indexName = indexName;
		this.refreshAfterWrite = refreshAfterWrite;
		this.ignoredErrorStatuses = asSet( ignoredErrorStatuses );
	}

	private static Set<Integer> asSet(int... ignoredErrorStatuses) {
		if ( ignoredErrorStatuses == null || ignoredErrorStatuses.length == 0 ) {
			return Collections.emptySet();
		}
		else if ( ignoredErrorStatuses.length == 1 ) {
			return Collections.singleton( ignoredErrorStatuses[0] );
		}
		else {
			Set<Integer> ignored = new HashSet<>();

			for ( int ignoredErrorStatus : ignoredErrorStatuses ) {
				ignored.add( ignoredErrorStatus );
			}

			return Collections.unmodifiableSet( ignored );
		}
	}

	/**
	 * Returns the original Lucene work from which this request was derived.
	 */
	public LuceneWork getLuceneWork() {
		return luceneWork;
	}

	public Action<T> getAction() {
		return action;
	}

	public String getIndexName() {
		return indexName;
	}

	/**
	 * Whether performing an explicit index refresh after executing this action is needed or not.
	 */
	public boolean needsRefreshAfterWrite() {
		return refreshAfterWrite;
	}

	public Set<Integer> getIgnoredErrorStatuses() {
		return ignoredErrorStatuses;
	}

	@Override
	public String toString() {
		return "BackendRequest [action=" + action + ", ignoredErrorStatuses=" + ignoredErrorStatuses + "]";
	}
}
