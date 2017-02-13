/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.hibernate.search.elasticsearch.client.impl.BackendRequestResultAssessor;
import org.hibernate.search.exception.SearchException;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult.BulkResultItem;


/**
 * @author Yoann Rodiere
 */
public class NoOpBackendRequestResultAssessor implements BackendRequestResultAssessor<JestResult> {

	public static final NoOpBackendRequestResultAssessor INSTANCE = new NoOpBackendRequestResultAssessor();

	private NoOpBackendRequestResultAssessor() {
		// Private; use INSTANCE instead
	}

	@Override
	public void checkSuccess(Action<? extends JestResult> request, JestResult result) throws SearchException {
		// Consider every result as a success
	}

	@Override
	public boolean isSuccess(BulkResultItem bulkResultItem) {
		// Consider every result as a success
		return true;
	}

}
