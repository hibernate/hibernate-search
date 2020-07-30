/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class SearchQueryScrollIT {

	private static final int DOCUMENT_COUNT = 200;
	private static final int PAGE_SIZE = 30;
	private static final int EXACT_DIVISOR_PAGE_SIZE = 25;

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void none() {
		try ( SearchScroll<DocumentReference> scroll = matchNoneQuery().scroll( PAGE_SIZE ) ) {
			SearchScrollResult<DocumentReference> scrollResult = scroll.next();
			Assertions.assertThat( scrollResult.hasHits() ).isFalse();
			Assertions.assertThat( scrollResult.hits() ).isEmpty();
		}
	}

	@Test
	public void one() {
		try ( SearchScroll<DocumentReference> scroll = matchOneQuery( 4 ).scroll( PAGE_SIZE ) ) {
			SearchScrollResult<DocumentReference> scrollResult = scroll.next();
			Assertions.assertThat( scrollResult.hasHits() ).isTrue();
			assertThat( scrollResult.hits() ).hasDocRefHitsExactOrder( index.typeName(), docId( 4 ) );

			scrollResult = scroll.next();
			Assertions.assertThat( scrollResult.hasHits() ).isFalse();
		}
	}

	@Test
	public void all() {
		try ( SearchScroll<DocumentReference> scroll = matchAllQuery().scroll( PAGE_SIZE ) ) {
			checkScrolling( scroll, DOCUMENT_COUNT, PAGE_SIZE );
		}
	}

	@Test
	public void all_exactDivisorPageSize() {
		try ( SearchScroll<DocumentReference> scroll = matchAllQuery().scroll( EXACT_DIVISOR_PAGE_SIZE ) ) {
			checkScrolling( scroll, DOCUMENT_COUNT, EXACT_DIVISOR_PAGE_SIZE );
		}
	}

	@Test
	public void all_failAfter() {
		Assertions.assertThatThrownBy( () -> matchAllQuery().failAfter( 1L, TimeUnit.NANOSECONDS ).scroll( PAGE_SIZE ).next() )
				.isInstanceOf( SearchTimeoutException.class )
				.hasMessageContaining( " exceeded the timeout of 0s, 0ms and 1ns: " );
	}

	@Test
	public void all_truncateAfter() {
		Assume.assumeTrue(
				"backend should have a fast timeout resolution in order to run this test correctly",
				TckConfiguration.get().getBackendFeatures().fastTimeoutResolution()
		);

		try ( SearchScroll<DocumentReference> scroll = matchAllQuery().truncateAfter( 1L, TimeUnit.NANOSECONDS ).scroll( DOCUMENT_COUNT ) ) {
			Assertions.assertThat( scroll.next().hits() ).hasSizeLessThan( DOCUMENT_COUNT );
		}
	}

	@Test
	public void firstHalf() {
		try ( SearchScroll<DocumentReference> scroll = matchFirstHalfQuery().scroll( PAGE_SIZE ) ) {
			checkScrolling( scroll, DOCUMENT_COUNT / 2, PAGE_SIZE );
		}
	}

	@Test
	public void firstHalf_onePage() {
		try ( SearchScroll<DocumentReference> scroll = matchFirstHalfQuery().scroll( DOCUMENT_COUNT / 2 ) ) {
			checkScrolling( scroll, DOCUMENT_COUNT / 2, DOCUMENT_COUNT / 2 );
		}
	}

	@Test
	public void firstHalf_largerPage() {
		try ( SearchScroll<DocumentReference> scroll = matchFirstHalfQuery().scroll( DOCUMENT_COUNT / 2 + 10 ) ) {
			checkScrolling( scroll, DOCUMENT_COUNT / 2, DOCUMENT_COUNT / 2 );
		}
	}

	@Test
	public void tookAndTimedOut() {
		try ( SearchScroll<DocumentReference> scroll = matchAllQuery().scroll( PAGE_SIZE ) ) {
			SearchScrollResult<DocumentReference> result = scroll.next();

			assertNotNull( result.took() );
			assertNotNull( result.timedOut() );
			assertFalse( result.timedOut() );
		}
	}

	private void checkScrolling(SearchScroll<DocumentReference> scroll, int documentCount, int pageSize) {
		int docIndex = 0;
		int quotient = documentCount / pageSize;

		for ( int i = 0; i < quotient; i++ ) {
			SearchScrollResult<DocumentReference> scrollResult = scroll.next();
			Assertions.assertThat( scrollResult.hasHits() ).isTrue();

			List<DocumentReference> hits = scrollResult.hits();
			Assertions.assertThat( hits ).hasSize( pageSize );
			for ( int j = 0; j < pageSize; j++ ) {
				Assertions.assertThat( hits.get( j ) ).extracting( DocumentReference::id ).isEqualTo( docId( docIndex++ ) );
				Assertions.assertThat( hits.get( j ) ).extracting( DocumentReference::typeName ).isEqualTo( index.typeName() );
			}
		}

		int remainder = documentCount % pageSize;
		SearchScrollResult<DocumentReference> scrollResult;

		if ( remainder != 0 ) {
			scrollResult = scroll.next();
			Assertions.assertThat( scrollResult.hasHits() ).isTrue();

			List<DocumentReference> hits = scrollResult.hits();
			Assertions.assertThat( hits ).hasSize( remainder );
			for ( int j = 0; j < remainder; j++ ) {
				Assertions.assertThat( hits.get( j ) ).extracting( DocumentReference::id ).isEqualTo( docId( docIndex++ ) );
				Assertions.assertThat( hits.get( j ) ).extracting( DocumentReference::typeName ).isEqualTo( index.typeName() );
			}
		}

		scrollResult = scroll.next();
		Assertions.assertThat( scrollResult.hasHits() ).isFalse();
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchAllQuery() {
		return index.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "integer" ).asc() );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchFirstHalfQuery() {
		return index.query()
				.where( f -> f.range().field( "integer" ).lessThan( DOCUMENT_COUNT / 2 ) )
				.sort( f -> f.field( "integer" ).asc() );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchOneQuery(int id) {
		return index.query()
				.where( f -> f.match().field( "integer" ).matching( id ) );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchNoneQuery() {
		return index.query()
				.where( f -> f.match().field( "integer" ).matching( DOCUMENT_COUNT + 2 ) );
	}

	private void initData() {
		index.bulkIndexer()
				.add( DOCUMENT_COUNT, i -> documentProvider(
						docId( i ),
						document -> document.addValue( index.binding().integer, i )
				) )
				.join();
	}

	private static String docId(int i) {
		return String.format( Locale.ROOT, "document_%05d", i );
	}

	private static class IndexBinding {
		final IndexFieldReference<Integer> integer;

		IndexBinding(IndexSchemaElement root) {
			integer = root.field( "integer", f -> f.asInteger().sortable( Sortable.YES ) )
					.toReference();
		}
	}
}
