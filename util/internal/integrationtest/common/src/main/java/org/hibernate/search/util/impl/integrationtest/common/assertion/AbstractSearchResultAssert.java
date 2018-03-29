/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;

import org.assertj.core.api.Assertions;

public abstract class AbstractSearchResultAssert<S, T> {

	final SearchResult<T> actual;
	String queryDescription = "<unknown>";

	AbstractSearchResultAssert(SearchResult<T> actual) {
		this.actual = actual;
	}

	public S fromQuery(SearchQuery<T> query) {
		this.queryDescription = query.toString();
		return thisAsSelfType();
	}

	public S hasNoHits() {
		Assertions.assertThat( actual.getHits() )
				.as( "Hits of " + queryDescription )
				.isEmpty();
		return thisAsSelfType();
	}

	public S hasHitCount(long expected) {
		Assertions.assertThat( actual.getHitCount() )
				.as( "Total hit count of " + queryDescription )
				.isEqualTo( expected );
		return thisAsSelfType();
	}

	protected final S thisAsSelfType() {
		return (S) this;
	}

}
