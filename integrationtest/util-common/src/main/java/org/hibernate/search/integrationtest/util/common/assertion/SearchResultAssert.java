/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.assertion;

import java.util.Collection;

import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;

import org.assertj.core.api.Assertions;

public class SearchResultAssert<T> extends AbstractSearchResultAssert<SearchResultAssert<T>, T> {

	public static <T> SearchResultAssert<T> assertThat(SearchQuery<T> searchQuery) {
		return assertThat( searchQuery.execute() ).fromQuery( searchQuery );
	}

	public static <T> SearchResultAssert<T> assertThat(SearchResult<T> actual) {
		return new SearchResultAssert<>( actual );
	}

	private SearchResultAssert(SearchResult<T> actual) {
		super( actual );
	}

	public SearchResultAssert<T> hasHitsExactOrder(Collection<T> hits) {
		return hasHitsExactOrder( (T[]) hits.toArray() );
	}

	public SearchResultAssert<T> hasHitsAnyOrder(Collection<T> hits) {
		return hasHitsAnyOrder( (T[]) hits.toArray() );
	}

	@SafeVarargs
	public final SearchResultAssert<T> hasHitsExactOrder(T... hits) {
		Assertions.assertThat( actual.getHits() )
				.as( "Hits of " + actual )
				.containsExactly( hits );
		return thisAsSelfType();
	}

	@SafeVarargs
	public final SearchResultAssert<T> hasHitsAnyOrder(T... hits) {
		Assertions.assertThat( actual.getHits() )
				.as( "Hits of " + actual )
				.containsOnly( hits );
		return thisAsSelfType();
	}


}
