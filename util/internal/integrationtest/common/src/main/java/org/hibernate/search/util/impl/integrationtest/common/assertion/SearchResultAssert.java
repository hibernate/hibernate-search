/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;

import org.assertj.core.api.Assertions;

public class SearchResultAssert<T> {

	public static <T> SearchResultAssert<T> assertThat(SearchQuery<? extends T> searchQuery) {
		return SearchResultAssert.<T>assertThat( searchQuery.fetch() ).fromQuery( searchQuery );
	}

	public static <T> SearchResultAssert<T> assertThat(SearchResult<? extends T> actual) {
		return new SearchResultAssert<>( actual );
	}

	public static <T> SearchHitsAssert<T> assertThat(List<? extends T> actual) {
		return SearchHitsAssert.assertThat( actual );
	}

	private final SearchResult<? extends T> actual;
	private String queryDescription = "<unknown>";

	private SearchResultAssert(SearchResult<? extends T> actual) {
		this.actual = actual;
	}

	public SearchResultAssert<T> fromQuery(SearchQuery<?> query) {
		this.queryDescription = query.toString();
		return this;
	}

	public SearchResultAssert<T> hasNoHits() {
		assertHits().isEmpty();
		return this;
	}

	public SearchResultAssert<T> hasTotalHitCount(long expected) {
		Assertions.assertThat( actual.getTotalHitCount() )
				.as( "Total hit count of " + queryDescription )
				.isEqualTo( expected );
		return this;
	}

	@SafeVarargs
	public final SearchResultAssert<T> hasHitsExactOrder(T... hits) {
		assertHits().hasHitsExactOrder( hits );
		return this;
	}

	@SafeVarargs
	public final SearchResultAssert<T> hasHitsAnyOrder(T... hits) {
		assertHits().hasHitsAnyOrder( hits );
		return this;
	}

	public final SearchResultAssert<T> hasHitsExactOrder(Collection<T> hits) {
		assertHits().hasHitsExactOrder( hits );
		return this;
	}

	public final SearchResultAssert<T> hasHitsAnyOrder(Collection<T> hits) {
		assertHits().hasHitsAnyOrder( hits );
		return this;
	}

	public SearchResultAssert<T> hasDocRefHitsExactOrder(String indexName, String firstId, String... otherIds) {
		assertHits().hasDocRefHitsExactOrder( indexName, firstId, otherIds );
		return this;
	}

	public SearchResultAssert<T> hasDocRefHitsAnyOrder(String indexName, String firstId, String... otherIds) {
		assertHits().hasDocRefHitsAnyOrder( indexName, firstId, otherIds );
		return this;
	}

	public SearchResultAssert<T> hasDocRefHitsExactOrder(Consumer<DocumentReferenceHitsBuilder> expectation) {
		assertHits().hasDocRefHitsExactOrder( expectation );
		return this;
	}

	public SearchResultAssert<T> hasDocRefHitsAnyOrder(Consumer<DocumentReferenceHitsBuilder> expectation) {
		assertHits().hasDocRefHitsAnyOrder( expectation );
		return this;
	}

	public SearchResultAssert<T> hasListHitsExactOrder(Consumer<ListHitsBuilder> expectation) {
		assertHits().hasListHitsExactOrder( expectation );
		return this;
	}

	public SearchResultAssert<T> hasListHitsAnyOrder(Consumer<ListHitsBuilder> expectation) {
		assertHits().hasListHitsAnyOrder( expectation );
		return this;
	}

	private SearchHitsAssert<T> assertHits() {
		return SearchHitsAssert.<T>assertThat( actual.getHits() ).as( "Hits of " + queryDescription );
	}

}
