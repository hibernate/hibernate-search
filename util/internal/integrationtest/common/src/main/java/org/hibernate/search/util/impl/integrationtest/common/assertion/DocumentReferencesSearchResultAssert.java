/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;

import org.assertj.core.api.Assertions;

public class DocumentReferencesSearchResultAssert<T extends DocumentReference>
		extends AbstractSearchResultAssert<DocumentReferencesSearchResultAssert<T>, T> {

	public static <T extends DocumentReference> DocumentReferencesSearchResultAssert<T> assertThat(
			SearchQuery<T> searchQuery) {
		return assertThat( searchQuery.execute() ).fromQuery( searchQuery );
	}

	public static <T extends DocumentReference> DocumentReferencesSearchResultAssert<T> assertThat(
			SearchResult<T> actual) {
		return new DocumentReferencesSearchResultAssert<>( actual );
	}

	private DocumentReferencesSearchResultAssert(SearchResult<T> actual) {
		super( actual );
	}

	public DocumentReferencesSearchResultAssert<T> hasReferencesHitsExactOrder(String indexName, String... ids) {
		return hasReferencesHitsExactOrder( ctx -> {
			ctx = ctx.doc( indexName, ids );
		} );
	}

	public DocumentReferencesSearchResultAssert<T> hasReferencesHitsAnyOrder(String indexName, String... ids) {
		return hasReferencesHitsAnyOrder( ctx -> {
			ctx = ctx.doc( indexName, ids );
		} );
	}

	public DocumentReferencesSearchResultAssert<T> hasReferencesHitsExactOrder(Consumer<ReferencesHitsBuilder> expectation) {
		ReferencesHitsBuilder context = new ReferencesHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualHits() )
				.as( "Hits of " + queryDescription )
				.containsExactly( context.getExpectedHits() );
		return this;
	}

	public DocumentReferencesSearchResultAssert<T> hasReferencesHitsAnyOrder(Consumer<ReferencesHitsBuilder> expectation) {
		ReferencesHitsBuilder context = new ReferencesHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualHits() )
				.as( "Hits of " + queryDescription )
				.containsOnly( context.getExpectedHits() );
		return this;
	}

	private List<DocumentReference> getNormalizedActualHits() {
		return actual.getHits().stream()
				.map( NormalizationUtils::normalizeReference )
				.collect( Collectors.toList() );
	}

	public class ReferencesHitsBuilder {

		private final List<DocumentReference> expectedHits = new ArrayList<>();

		private ReferencesHitsBuilder() {
		}

		public ReferencesHitsBuilder doc(String indexName, String... ids) {
			for ( String id : ids ) {
				expectedHits.add( NormalizationUtils.reference( indexName, id ) );
			}
			return this;
		}

		private DocumentReference[] getExpectedHits() {
			return expectedHits.toArray( new DocumentReference[expectedHits.size()] );
		}
	}

}
