/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import org.hibernate.search.backend.IndexingMonitor;
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
	private final LuceneWork luceneWork;
	private final String indexName;
	private final IndexingMonitor indexingMonitor;
	private final BackendRequestResultAssessor<? super T> resultAssessor;
	private final BackendRequestSuccessReporter<? super T> successReporter;
	private final boolean refreshAfterWrite;

	public BackendRequest(Action<T> action, LuceneWork luceneWork, String indexName,
			BackendRequestResultAssessor<? super T> resultAssessor,
			IndexingMonitor indexingMonitor, BackendRequestSuccessReporter<? super T> successReporter,
			boolean refreshAfterWrite) {
		this.action = action;
		this.luceneWork = luceneWork;
		this.indexName = indexName;
		this.resultAssessor = resultAssessor;
		this.indexingMonitor = indexingMonitor;
		this.successReporter = successReporter;
		this.refreshAfterWrite = refreshAfterWrite;
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

	public BackendRequestResultAssessor<? super T> getResultAssessor() {
		return resultAssessor;
	}

	public IndexingMonitor getIndexingMonitor() {
		return indexingMonitor;
	}

	public BackendRequestSuccessReporter<? super T> getSuccessReporter() {
		return successReporter;
	}

	/**
	 * Whether performing an explicit index refresh after executing this action is needed or not.
	 */
	public boolean needsRefreshAfterWrite() {
		return refreshAfterWrite;
	}

	@Override
	public String toString() {
		return "BackendRequest [action=" + action + ", resultAssessor=" + resultAssessor + "]";
	}
}
