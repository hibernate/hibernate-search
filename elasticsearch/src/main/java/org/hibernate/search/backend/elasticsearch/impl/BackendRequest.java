/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

	private Action<T> action;
	private Set<Integer> ignoredErrorStatuses;

	public BackendRequest(Action<T> action, int... ignoredErrorStatuses) {
		this.action = action;
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

	public Action<T> getAction() {
		return action;
	}

	public Set<Integer> getIgnoredErrorStatuses() {
		return ignoredErrorStatuses;
	}

	@Override
	public String toString() {
		return "BackendRequest [action=" + action + ", ignoredErrorStatuses=" + ignoredErrorStatuses + "]";
	}
}
