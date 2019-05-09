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

public class SearchResultAssert<H> {

	public static <H> SearchResultAssert<H> assertThat(SearchQuery<? extends H> searchQuery) {
		return SearchResultAssert.<H>assertThat( searchQuery.fetch() ).fromQuery( searchQuery );
	}

	public static <H> SearchResultAssert<H> assertThat(SearchResult<? extends H> actual) {
		return new SearchResultAssert<>( actual );
	}

	public static <H> SearchHitsAssert<H> assertThat(List<? extends H> actual) {
		return SearchHitsAssert.assertThat( actual );
	}

	private final SearchResult<? extends H> actual;
	private String queryDescription = "<unknown>";

	private SearchResultAssert(SearchResult<? extends H> actual) {
		this.actual = actual;
	}

	public SearchResultAssert<H> fromQuery(SearchQuery<?> query) {
		this.queryDescription = query.toString();
		return this;
	}

	public SearchResultAssert<H> hasNoHits() {
		assertHits().isEmpty();
		return this;
	}

	public SearchResultAssert<H> hasTotalHitCount(long expected) {
		Assertions.assertThat( actual.getTotalHitCount() )
				.as( "Total hit count of " + queryDescription )
				.isEqualTo( expected );
		return this;
	}

	@SafeVarargs
	public final SearchResultAssert<H> hasHitsExactOrder(H... hits) {
		assertHits().hasHitsExactOrder( hits );
		return this;
	}

	@SafeVarargs
	public final SearchResultAssert<H> hasHitsAnyOrder(H... hits) {
		assertHits().hasHitsAnyOrder( hits );
		return this;
	}

	public final SearchResultAssert<H> hasHitsExactOrder(Collection<H> hits) {
		assertHits().hasHitsExactOrder( hits );
		return this;
	}

	public final SearchResultAssert<H> hasHitsAnyOrder(Collection<H> hits) {
		assertHits().hasHitsAnyOrder( hits );
		return this;
	}

	public SearchResultAssert<H> hasDocRefHitsExactOrder(String indexName, String firstId, String... otherIds) {
		assertHits().hasDocRefHitsExactOrder( indexName, firstId, otherIds );
		return this;
	}

	public SearchResultAssert<H> hasDocRefHitsAnyOrder(String indexName, String firstId, String... otherIds) {
		assertHits().hasDocRefHitsAnyOrder( indexName, firstId, otherIds );
		return this;
	}

	public SearchResultAssert<H> hasDocRefHitsExactOrder(Consumer<DocumentReferenceHitsBuilder> expectation) {
		assertHits().hasDocRefHitsExactOrder( expectation );
		return this;
	}

	public SearchResultAssert<H> hasDocRefHitsAnyOrder(Consumer<DocumentReferenceHitsBuilder> expectation) {
		assertHits().hasDocRefHitsAnyOrder( expectation );
		return this;
	}

	public SearchResultAssert<H> hasListHitsExactOrder(Consumer<ListHitsBuilder> expectation) {
		assertHits().hasListHitsExactOrder( expectation );
		return this;
	}

	public SearchResultAssert<H> hasListHitsAnyOrder(Consumer<ListHitsBuilder> expectation) {
		assertHits().hasListHitsAnyOrder( expectation );
		return this;
	}

	private SearchHitsAssert<H> assertHits() {
		return SearchHitsAssert.<H>assertThat( actual.getHits() ).as( "Hits of " + queryDescription );
	}

}
