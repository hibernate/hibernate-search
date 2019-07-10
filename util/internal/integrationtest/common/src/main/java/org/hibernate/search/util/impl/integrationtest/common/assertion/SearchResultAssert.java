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

import org.assertj.core.api.AbstractLongAssert;
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

	public AbstractLongAssert<?> totalHitCount() {
		return Assertions.assertThat( actual.getTotalHitCount() )
				.as( "Total hit count of " + queryDescription );
	}

	public SearchHitsAssert<H> hits() {
		return SearchHitsAssert.<H>assertThat( actual.getHits() ).as( "Hits of " + queryDescription );
	}

	public SearchResultAssert<H> hasNoHits() {
		hits().isEmpty();
		return this;
	}

	public SearchResultAssert<H> hasTotalHitCount(long expected) {
		totalHitCount().isEqualTo( expected );
		return this;
	}

	@SafeVarargs
	public final SearchResultAssert<H> hasHitsExactOrder(H... hits) {
		hits().hasHitsExactOrder( hits );
		return this;
	}

	@SafeVarargs
	public final SearchResultAssert<H> hasHitsAnyOrder(H... hits) {
		hits().hasHitsAnyOrder( hits );
		return this;
	}

	public final SearchResultAssert<H> hasHitsExactOrder(Collection<H> hits) {
		hits().hasHitsExactOrder( hits );
		return this;
	}

	public final SearchResultAssert<H> hasHitsAnyOrder(Collection<H> hits) {
		hits().hasHitsAnyOrder( hits );
		return this;
	}

	public SearchResultAssert<H> hasDocRefHitsExactOrder(String indexName, String firstId, String... otherIds) {
		hits().hasDocRefHitsExactOrder( indexName, firstId, otherIds );
		return this;
	}

	public SearchResultAssert<H> hasDocRefHitsAnyOrder(String indexName, String firstId, String... otherIds) {
		hits().hasDocRefHitsAnyOrder( indexName, firstId, otherIds );
		return this;
	}

	public SearchResultAssert<H> hasDocRefHitsExactOrder(Consumer<NormalizedDocRefHit.Builder> expectation) {
		hits().hasDocRefHitsExactOrder( expectation );
		return this;
	}

	public SearchResultAssert<H> hasDocRefHitsAnyOrder(Consumer<NormalizedDocRefHit.Builder> expectation) {
		hits().hasDocRefHitsAnyOrder( expectation );
		return this;
	}

	public SearchResultAssert<H> hasListHitsExactOrder(Consumer<NormalizedListHit.Builder> expectation) {
		hits().hasListHitsExactOrder( expectation );
		return this;
	}

	public SearchResultAssert<H> hasListHitsAnyOrder(Consumer<NormalizedListHit.Builder> expectation) {
		hits().hasListHitsAnyOrder( expectation );
		return this;
	}

}
