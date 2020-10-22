/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Locale;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class SearchQueryScrollIT {

	private static final int DOCUMENT_COUNT = 200;
	private static final int CHUNK_SIZE = 30;
	private static final int EXACT_DIVISOR_CHUNK_SIZE = 25;

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void none() {
		try ( SearchScroll<DocumentReference> scroll = matchNoneQuery().scroll( CHUNK_SIZE ) ) {
			SearchScrollResult<DocumentReference> scrollResult = scroll.next();
			assertThat( scrollResult.hasHits() ).isFalse();
			assertThat( scrollResult.hits() ).isEmpty();
			assertThat( scrollResult.total().hitCount() ).isEqualTo( 0L );
		}
	}

	@Test
	public void one() {
		try ( SearchScroll<DocumentReference> scroll = matchOneQuery( 4 ).scroll( CHUNK_SIZE ) ) {
			SearchScrollResult<DocumentReference> scrollResult = scroll.next();
			assertThat( scrollResult.hasHits() ).isTrue();
			assertThatHits( scrollResult.hits() ).hasDocRefHitsExactOrder( index.typeName(), docId( 4 ) );
			assertThat( scrollResult.total().hitCount() ).isEqualTo( 1L );

			scrollResult = scroll.next();
			assertThat( scrollResult.hasHits() ).isFalse();
			assertThat( scrollResult.total().hitCount() ).isEqualTo( 1L );
		}
	}

	@Test
	public void all() {
		try ( SearchScroll<DocumentReference> scroll = matchAllQuery().scroll( CHUNK_SIZE ) ) {
			checkScrolling( scroll, DOCUMENT_COUNT, CHUNK_SIZE );
		}
	}

	@Test
	public void all_exactDivisorPageSize() {
		try ( SearchScroll<DocumentReference> scroll = matchAllQuery().scroll( EXACT_DIVISOR_CHUNK_SIZE ) ) {
			checkScrolling( scroll, DOCUMENT_COUNT, EXACT_DIVISOR_CHUNK_SIZE );
		}
	}

	@Test
	public void firstHalf() {
		try ( SearchScroll<DocumentReference> scroll = matchFirstHalfQuery().scroll( CHUNK_SIZE ) ) {
			checkScrolling( scroll, DOCUMENT_COUNT / 2, CHUNK_SIZE );
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
		try ( SearchScroll<DocumentReference> scroll = matchAllQuery().scroll( CHUNK_SIZE ) ) {
			SearchScrollResult<DocumentReference> result = scroll.next();

			assertNotNull( result.took() );
			assertNotNull( result.timedOut() );
			assertFalse( result.timedOut() );
		}
	}

	private void checkScrolling(SearchScroll<DocumentReference> scroll, int documentCount, int chunkSize) {
		int docIndex = 0;
		int quotient = documentCount / chunkSize;

		for ( int i = 0; i < quotient; i++ ) {
			SearchScrollResult<DocumentReference> scrollResult = scroll.next();
			assertThat( scrollResult.hasHits() ).isTrue();

			List<DocumentReference> hits = scrollResult.hits();
			assertThat( hits ).hasSize( chunkSize );
			for ( int j = 0; j < chunkSize; j++ ) {
				assertThat( hits.get( j ) ).extracting( DocumentReference::id ).isEqualTo( docId( docIndex++ ) );
				assertThat( hits.get( j ) ).extracting( DocumentReference::typeName ).isEqualTo( index.typeName() );
			}

			assertThat( scrollResult.total().hitCount() ).isEqualTo( documentCount );
		}

		int remainder = documentCount % chunkSize;
		SearchScrollResult<DocumentReference> scrollResult;

		if ( remainder != 0 ) {
			scrollResult = scroll.next();
			assertThat( scrollResult.hasHits() ).isTrue();

			List<DocumentReference> hits = scrollResult.hits();
			assertThat( hits ).hasSize( remainder );
			for ( int j = 0; j < remainder; j++ ) {
				assertThat( hits.get( j ) ).extracting( DocumentReference::id ).isEqualTo( docId( docIndex++ ) );
				assertThat( hits.get( j ) ).extracting( DocumentReference::typeName ).isEqualTo( index.typeName() );
			}

			assertThat( scrollResult.total().hitCount() ).isEqualTo( documentCount );
		}

		scrollResult = scroll.next();
		assertThat( scrollResult.hasHits() ).isFalse();
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

	private static void initData() {
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
