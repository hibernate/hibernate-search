/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

public class SearchResultAssert<T> {

	public static <T> SearchResultAssert<T> assertThat(SearchQuery<? extends T> searchQuery) {
		return SearchResultAssert.<T>assertThat( searchQuery.fetch() ).fromQuery( searchQuery );
	}

	public static <T> SearchResultAssert<T> assertThat(SearchResult<? extends T> actual) {
		return new SearchResultAssert<>( actual );
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
		Assertions.<T>assertThat( actual.getHits() )
				.as( "Hits of " + queryDescription )
				.isEmpty();
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
		Assertions.<T>assertThat( actual.getHits() )
				.as( "Hits of " + queryDescription )
				.containsExactly( hits );
		return this;
	}

	@SafeVarargs
	public final SearchResultAssert<T> hasHitsAnyOrder(T... hits) {
		Assertions.<T>assertThat( actual.getHits() )
				.as( "Hits of " + queryDescription )
				.containsExactlyInAnyOrder( hits );
		return this;
	}

	@SuppressWarnings("unchecked")
	public final SearchResultAssert<T> hasHitsExactOrder(Collection<T> hits) {
		return hasHitsExactOrder( (T[]) hits.toArray() );
	}

	@SuppressWarnings("unchecked")
	public final SearchResultAssert<T> hasHitsAnyOrder(Collection<T> hits) {
		return hasHitsAnyOrder( (T[]) hits.toArray() );
	}

	public SearchResultAssert<T> hasDocRefHitsExactOrder(String indexName, String firstId, String... otherIds) {
		return hasDocRefHitsExactOrder( ctx -> {
			ctx.doc( indexName, firstId, otherIds );
		} );
	}

	public SearchResultAssert<T> hasDocRefHitsAnyOrder(String indexName, String firstId, String... otherIds) {
		return hasDocRefHitsAnyOrder( ctx -> {
			ctx.doc( indexName, firstId, otherIds );
		} );
	}

	public SearchResultAssert<T> hasDocRefHitsExactOrder(Consumer<DocumentReferenceHitsBuilder> expectation) {
		DocumentReferenceHitsBuilder context = new DocumentReferenceHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualDocumentReferencesHits() )
				.as( "Hits of " + queryDescription )
				.containsExactly( context.getExpectedHits() );
		return this;
	}

	public SearchResultAssert<T> hasDocRefHitsAnyOrder(Consumer<DocumentReferenceHitsBuilder> expectation) {
		DocumentReferenceHitsBuilder context = new DocumentReferenceHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualDocumentReferencesHits() )
				.as( "Hits of " + queryDescription )
				.containsExactlyInAnyOrder( context.getExpectedHits() );
		return this;
	}

	public SearchResultAssert<T> hasListHitsExactOrder(Consumer<ListHitsBuilder> expectation) {
		ListHitsBuilder context = new ListHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualListHits() )
				.as( "Hits of " + queryDescription )
				.containsExactly( context.getExpectedHits() );
		return this;
	}

	public SearchResultAssert<T> hasListHitsAnyOrder(Consumer<ListHitsBuilder> expectation) {
		ListHitsBuilder context = new ListHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualListHits() )
				.as( "Hits of " + queryDescription )
				.containsExactlyInAnyOrder( context.getExpectedHits() );
		return this;
	}

	@SuppressWarnings( "unchecked" ) // We check that at runtime, that's what the assertion is for
	private List<DocumentReference> getNormalizedActualDocumentReferencesHits() {
		List<? extends T> hits = actual.getHits();
		shouldHaveOnlyElementsOfTypeOrNull(
				Assertions.assertThat( hits )
						.as( "Hits of " + queryDescription ),
				DocumentReference.class
		);
		return ( (List<? extends DocumentReference>) hits ).stream()
				.map( NormalizationUtils::normalizeReference )
				.collect( Collectors.toList() );
	}

	@SuppressWarnings( "unchecked" ) // We check that at runtime, that's what the assertion is for
	private List<List<?>> getNormalizedActualListHits() {
		List<? extends T> hits = actual.getHits();
		shouldHaveOnlyElementsOfTypeOrNull(
				Assertions.assertThat( hits )
						.as( "Hits of " + queryDescription ),
				List.class
		);
		return ( (List<? extends List<?>>) hits ).stream()
				.map( NormalizationUtils::normalizeList )
				.collect( Collectors.toList() );
	}

	private void shouldHaveOnlyElementsOfTypeOrNull(ListAssert<? extends T> theAssert, Class<?> type) {
		theAssert.satisfies( actual -> {
			for ( Object element : actual ) {
				if ( element != null && !type.isInstance( element ) ) {
					throw Failures.instance().failure(
							theAssert.getWritableAssertionInfo(),
							new BasicErrorMessageFactory(
									"%nExpecting:%n  <%s>%nto only have elements that are null or of type:%n  <%s>%nbut found:%n  <%s>",
									actual, type, element.getClass()
							)
					);
				}
			}
		} );
	}

	public class DocumentReferenceHitsBuilder {

		private final List<DocumentReference> expectedHits = new ArrayList<>();

		private DocumentReferenceHitsBuilder() {
		}

		public DocumentReferenceHitsBuilder doc(String indexName, String firstId, String... otherIds) {
			expectedHits.add( NormalizationUtils.reference( indexName, firstId ) );
			for ( String id : otherIds ) {
				expectedHits.add( NormalizationUtils.reference( indexName, id ) );
			}
			return this;
		}

		private DocumentReference[] getExpectedHits() {
			return expectedHits.toArray( new DocumentReference[0] );
		}
	}

	public class ListHitsBuilder {
		private final List<List<?>> expectedHits = new ArrayList<>();

		private ListHitsBuilder() {
		}

		public ListHitsBuilder list(Object firstProjectionItem, Object ... otherProjectionItems) {
			List<?> projectionItems = CollectionHelper.asList( firstProjectionItem, otherProjectionItems );
			expectedHits.add( NormalizationUtils.normalizeList( projectionItems ) );
			return this;
		}

		@SuppressWarnings("rawtypes")
		private List[] getExpectedHits() {
			return expectedHits.toArray( new List[0] );
		}
	}

}
