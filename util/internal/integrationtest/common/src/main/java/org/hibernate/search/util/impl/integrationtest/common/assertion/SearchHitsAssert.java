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

	public ListAssert<DocumentReference> asNormalizedDocRefs() {
		shouldHaveOnlyElementsOfTypeOrNull( asIs(), DocumentReference.class );
		@SuppressWarnings( "unchecked" ) // We check that at runtime, that's what the assertion is for
		List<DocumentReference> normalized = normalizeDocRefHits( ( (List<? extends DocumentReference>) actual ) );
		return Assertions.assertThat( normalized ).as( description );
	}

	public ListAssert<List<?>> asNormalizedLists() {
		shouldHaveOnlyElementsOfTypeOrNull( asIs(), List.class );
		@SuppressWarnings( "unchecked" ) // We check that at runtime, that's what the assertion is for
		List<List<?>> normalized = ( (List<? extends List<?>>) actual ).stream()
				.map( NormalizationUtils::normalizeList )
				.collect( Collectors.toList() );
		return Assertions.assertThat( normalized ).as( description );
	}

	public ListAssert<H> asIs() {
		return Assertions.<H>assertThat( actual )
				.as( description );
	}

	public SearchHitsAssert<H> isEmpty() {
		Assertions.<H>assertThat( actual )
				.as( description )
				.isEmpty();
		return this;
	}

	@SafeVarargs
	public final SearchHitsAssert<H> hasHitsExactOrder(H... hits) {
		asIs().containsExactly( hits );
		return this;
	}

	@SafeVarargs
	public final SearchHitsAssert<H> hasHitsAnyOrder(H... hits) {
		asIs().containsExactlyInAnyOrder( hits );
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

	public SearchHitAssert<H> ordinal(int i) {
		return new SearchHitAssert<>( actual.get( i ) );
	}

	public SearchHitsAssert<H> ordinals(int... ordinals) {
		List<H> newActuals = new ArrayList<>();
		for ( int ordinal : ordinals ) {
			Assertions.assertThat( ordinal ).isLessThan( actual.size() );
			newActuals.add( actual.get( ordinal ) );
		}

		return new SearchHitsAssert( newActuals );
	}

	public SearchHitsAssert<H> hasDocRefHitsExactOrder(Consumer<NormalizedDocRefHit.Builder> expectation) {
		asNormalizedDocRefs().containsExactly( NormalizedDocRefHit.of( expectation ) );
		return this;
	}

	public SearchHitsAssert<H> hasDocRefHitsAnyOrder(Consumer<NormalizedDocRefHit.Builder> expectation) {
		asNormalizedDocRefs().containsExactlyInAnyOrder( NormalizedDocRefHit.of( expectation ) );
		return this;
	}

	public SearchHitsAssert<H> hasListHitsExactOrder(Consumer<NormalizedListHit.Builder> expectation) {
		asNormalizedLists().containsExactly( NormalizedListHit.of( expectation ) );
		return this;
	}

	public SearchHitsAssert<H> hasListHitsAnyOrder(Consumer<NormalizedListHit.Builder> expectation) {
		asNormalizedLists().containsExactlyInAnyOrder( NormalizedListHit.of( expectation ) );
		return this;
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
