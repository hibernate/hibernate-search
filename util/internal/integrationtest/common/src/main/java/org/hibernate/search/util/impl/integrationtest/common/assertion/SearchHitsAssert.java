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
import java.util.stream.Collectors;

import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

public class SearchHitsAssert<H> {

	public static <H> SearchHitsAssert<H> assertThat(List<? extends H> actual) {
		return new SearchHitsAssert<>( actual );
	}

	private final List<? extends H> actual;
	private String description = "<unknown>";

	private SearchHitsAssert(List<? extends H> actual) {
		this.actual = actual;
	}

	public SearchHitsAssert<H> fromQuery(SearchQuery<?> query) {
		return as( "Hits of " + query.toString() );
	}

	public SearchHitsAssert<H> as(String description) {
		this.description = description;
		return this;
	}

	public SearchHitsAssert<H> isEmpty() {
		Assertions.<H>assertThat( actual )
				.as( description )
				.isEmpty();
		return this;
	}

	@SafeVarargs
	public final SearchHitsAssert<H> hasHitsExactOrder(H... hits) {
		Assertions.<H>assertThat( actual )
				.as( description )
				.containsExactly( hits );
		return this;
	}

	@SafeVarargs
	public final SearchHitsAssert<H> hasHitsAnyOrder(H... hits) {
		Assertions.<H>assertThat( actual )
				.as( description )
				.containsExactlyInAnyOrder( hits );
		return this;
	}

	@SuppressWarnings("unchecked")
	public final SearchHitsAssert<H> hasHitsExactOrder(Collection<H> hits) {
		return hasHitsExactOrder( (H[]) hits.toArray() );
	}

	@SuppressWarnings("unchecked")
	public final SearchHitsAssert<H> hasHitsAnyOrder(Collection<H> hits) {
		return hasHitsAnyOrder( (H[]) hits.toArray() );
	}

	public SearchHitsAssert<H> hasDocRefHitsExactOrder(String indexName, String firstId, String... otherIds) {
		return hasDocRefHitsExactOrder( ctx -> {
			ctx.doc( indexName, firstId, otherIds );
		} );
	}

	public SearchHitsAssert<H> hasDocRefHitsAnyOrder(String indexName, String firstId, String... otherIds) {
		return hasDocRefHitsAnyOrder( ctx -> {
			ctx.doc( indexName, firstId, otherIds );
		} );
	}

	public SearchHitsAssert<H> hasDocRefHitsExactOrder(Consumer<DocumentReferenceHitsBuilder> expectation) {
		DocumentReferenceHitsBuilder context = new DocumentReferenceHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualDocumentReferencesHits() )
				.as( description )
				.containsExactly( context.getExpectedHits() );
		return this;
	}

	public SearchHitsAssert<H> hasDocRefHitsAnyOrder(Consumer<DocumentReferenceHitsBuilder> expectation) {
		DocumentReferenceHitsBuilder context = new DocumentReferenceHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualDocumentReferencesHits() )
				.as( description )
				.containsExactlyInAnyOrder( context.getExpectedHits() );
		return this;
	}

	public SearchHitsAssert<H> hasListHitsExactOrder(Consumer<ListHitsBuilder> expectation) {
		ListHitsBuilder context = new ListHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualListHits() )
				.as( description )
				.containsExactly( context.getExpectedHits() );
		return this;
	}

	public SearchHitsAssert<H> hasListHitsAnyOrder(Consumer<ListHitsBuilder> expectation) {
		ListHitsBuilder context = new ListHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualListHits() )
				.as( description )
				.containsExactlyInAnyOrder( context.getExpectedHits() );
		return this;
	}

	@SuppressWarnings( "unchecked" ) // We check that at runtime, that's what the assertion is for
	private List<DocumentReference> getNormalizedActualDocumentReferencesHits() {
		shouldHaveOnlyElementsOfTypeOrNull(
				Assertions.assertThat( actual )
						.as( description ),
				DocumentReference.class
		);
		return normalizeDocRefHits( ( (List<? extends DocumentReference>) actual ) );
	}

	@SuppressWarnings( "unchecked" ) // We check that at runtime, that's what the assertion is for
	private List<List<?>> getNormalizedActualListHits() {
		shouldHaveOnlyElementsOfTypeOrNull(
				Assertions.assertThat( actual )
						.as( description ),
				List.class
		);
		return ( (List<? extends List<?>>) actual ).stream()
				.map( NormalizationUtils::normalizeList )
				.collect( Collectors.toList() );
	}

	private static <T> void shouldHaveOnlyElementsOfTypeOrNull(ListAssert<? extends T> theAssert, Class<?> type) {
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

	private static List<DocumentReference> normalizeDocRefHits(List<? extends DocumentReference> hits) {
		return hits.stream()
				.map( NormalizationUtils::normalizeReference )
				.collect( Collectors.toList() );
	}

}
