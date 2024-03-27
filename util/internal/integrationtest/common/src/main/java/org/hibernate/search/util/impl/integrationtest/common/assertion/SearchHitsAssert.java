/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils;

import org.assertj.core.api.ListAssert;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

public class SearchHitsAssert<H> {

	public static <H> SearchHitsAssert<H> assertThatHits(List<? extends H> actual) {
		return new SearchHitsAssert<>( actual );
	}

	private final boolean usingRecursiveFieldByFieldElementComparator;
	private final List<? extends H> actual;
	private String description = "<unknown>";

	private SearchHitsAssert(List<? extends H> actual) {
		this( false, actual );
	}

	public SearchHitsAssert(boolean usingRecursiveFieldByFieldElementComparator, List<? extends H> actual) {
		this.usingRecursiveFieldByFieldElementComparator = usingRecursiveFieldByFieldElementComparator;
		this.actual = actual;
	}

	public SearchHitsAssert<H> as(String description) {
		this.description = description;
		return this;
	}

	public ListAssert<DocumentReference> asNormalizedDocRefs() {
		shouldHaveOnlyElementsOfTypeOrNull( asIs(), DocumentReference.class );
		@SuppressWarnings("unchecked") // We check that at runtime, that's what the assertion is for
		List<DocumentReference> normalized = normalizeDocRefHits( ( (List<? extends DocumentReference>) actual ) );
		return assertThat( normalized ).as( description );
	}

	public ListAssert<List<?>> asNormalizedLists() {
		shouldHaveOnlyElementsOfTypeOrNull( asIs(), List.class );
		@SuppressWarnings("unchecked") // We check that at runtime, that's what the assertion is for
		List<List<?>> normalized = ( (List<? extends List<?>>) actual ).stream()
				.map( NormalizationUtils::<List<?>>normalize )
				.collect( Collectors.toList() );
		return assertThat( normalized ).as( description );
	}

	@SuppressWarnings("unchecked")
	public ListAssert<H> asIs() {
		return assertThat( (List<H>) actual )
				.as( description );
	}

	@SuppressWarnings("unchecked")
	public SearchHitsAssert<H> isEmpty() {
		assertThat( (List<H>) actual )
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

	public SearchHitsAssert<H> hasDocRefHitsExactOrder(String typeName, String firstId, String... otherIds) {
		return hasDocRefHitsExactOrder( ctx -> {
			ctx.doc( typeName, firstId, otherIds );
		} );
	}

	public SearchHitsAssert<H> hasDocRefHitsAnyOrder(String typeName, String firstId, String... otherIds) {
		return hasDocRefHitsAnyOrder( ctx -> {
			ctx.doc( typeName, firstId, otherIds );
		} );
	}

	public SearchHitAssert<H> ordinal(int i) {
		return new SearchHitAssert<>( actual.get( i ) );
	}

	public SearchHitsAssert<H> ordinals(int... ordinals) {
		List<H> newActuals = new ArrayList<>();
		for ( int ordinal : ordinals ) {
			assertThat( ordinal ).isLessThan( actual.size() );
			newActuals.add( actual.get( ordinal ) );
		}

		return new SearchHitsAssert<>( newActuals );
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
				.map( NormalizationUtils::normalize )
				.collect( Collectors.toList() );
	}
}
