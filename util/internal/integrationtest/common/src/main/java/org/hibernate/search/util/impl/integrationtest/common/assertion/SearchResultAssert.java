/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.Collection;

import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;
import org.hibernate.search.util.impl.common.CollectionHelper;

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

	@SuppressWarnings("unchecked")
	public SearchResultAssert<T> hasHitsExactOrder(T firstHist, T... otherHits) {
		return hasHitsExactOrder( CollectionHelper.asList( firstHist, otherHits ) );
	}

	@SuppressWarnings("unchecked")
	public SearchResultAssert<T> hasHitsAnyOrder(T firstHist, T... otherHits) {
		return hasHitsAnyOrder( CollectionHelper.asList( firstHist, otherHits ) );
	}

	public final SearchResultAssert<T> hasHitsExactOrder(Collection<T> hits) {
		Assertions.assertThat( actual.getHits() )
				.as( "Hits of " + actual )
				.containsExactly( (T[]) hits.toArray() );
		return thisAsSelfType();
	}

	public final SearchResultAssert<T> hasHitsAnyOrder(Collection<T> hits) {
		Assertions.assertThat( actual.getHits() )
				.as( "Hits of " + actual )
				.containsOnly( (T[]) hits.toArray() );
		return thisAsSelfType();
	}


}
