/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.bool;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.sort.SortFinalStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactory;
import org.hibernate.search.engine.search.dsl.sort.SearchSortFactory;
import org.hibernate.search.integrationtest.backend.tck.search.predicate.RangeSearchPredicateIT;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.integrationtest.backend.tck.search.sort.FieldSearchSortIT;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests sorting and ranging behaviour querying a boolean type field
 *
 * @see FieldSearchSortIT
 * @see RangeSearchPredicateIT
 */
public class BooleanSortAndRangePredicateIT {

	public static final String INDEX_NAME = "myIndexName";
	public static final String FIELD_PATH = "boolean";

	public static final String DOCUMENT_1 = "1";
	public static final String DOCUMENT_2 = "2";
	public static final String DOCUMENT_3 = "3";
	public static final String DOCUMENT_4 = "4";
	public static final String DOCUMENT_5 = "5";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void before() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	private SearchQuery<DocumentReference> sortQuery(
			Function<? super SearchSortFactory, ? extends SortFinalStep> sortContributor) {
		StubMappingScope scope = indexManager.createScope();
		return scope.query()
				.predicate( f -> f.matchAll() )
				.sort( sortContributor )
				.toQuery();
	}

	private SearchQuery<DocumentReference> rangeQuery(Function<SearchPredicateFactory, PredicateFinalStep> rangePredicate) {
		StubMappingScope scope = indexManager.createScope();
		return scope.query()
				.predicate( rangePredicate )
				.toQuery();
	}

	@Test
	public void sortByFieldQuery() {
		// Default order
		SearchQuery<DocumentReference> query = sortQuery( c -> c.byField( FIELD_PATH ).onMissingValue().sortLast() );
		assertHasHitsWithBooleanProperties( query, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, null );

		// Explicit order with onMissingValue().sortLast()
		query = sortQuery( c -> c.byField( FIELD_PATH ).asc().onMissingValue().sortLast() );
		assertHasHitsWithBooleanProperties( query, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, null );
		query = sortQuery( b -> b.byField( FIELD_PATH ).desc().onMissingValue().sortLast() );
		assertHasHitsWithBooleanProperties( query, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, null );

		// Explicit order with onMissingValue().sortFirst()
		query = sortQuery( c -> c.byField( FIELD_PATH ).asc().onMissingValue().sortFirst() );
		assertHasHitsWithBooleanProperties( query, null, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE );
		query = sortQuery( b -> b.byField( FIELD_PATH ).desc().onMissingValue().sortFirst() );
		assertHasHitsWithBooleanProperties( query, null, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE );
	}

	@Test
	public void rangeQueryAbove() {
		SearchQuery<DocumentReference> query = rangeQuery( f -> f.range().onField( FIELD_PATH ).above( Boolean.TRUE ) );
		assertHasHitsWithBooleanProperties( query, Boolean.TRUE, Boolean.TRUE );
	}

	@Test
	public void rangeQueryBelow() {
		SearchQuery<DocumentReference> query = rangeQuery( f -> f.range().onField( FIELD_PATH ).below( Boolean.FALSE ) );
		assertHasHitsWithBooleanProperties( query, Boolean.FALSE, Boolean.FALSE );
	}

	@Test
	public void rangeQueryFromTo() {
		SearchQuery<DocumentReference> query = rangeQuery( f -> f.range().onField( FIELD_PATH ).from( Boolean.FALSE ).to( Boolean.FALSE ) );
		assertHasHitsWithBooleanProperties( query, Boolean.FALSE, Boolean.FALSE );
	}

	@Test
	public void rangeFromToSortByFieldQuery() {
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.range().onField( FIELD_PATH ).from( Boolean.FALSE ).to( Boolean.TRUE ) )
				.sort( f -> f.byField( FIELD_PATH ).onMissingValue().sortLast() )
				.toQuery();

		assertHasHitsWithBooleanProperties( query, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE );
	}

	private void assertHasHitsWithBooleanProperties(SearchQuery<DocumentReference> query, Boolean... expectedPropertyValues) {
		List<DocumentReference> hits = query.fetch().getHits();
		assertEquals( expectedPropertyValues.length, hits.size() );

		for ( int i = 0; i < expectedPropertyValues.length; i++ ) {
			Boolean expectedPropertyValue = expectedPropertyValues[i];

			if ( expectedPropertyValue == Boolean.TRUE ) {
				assertTrue( isTrueDocument( hits.get( i ) ) );
			}
			else if ( expectedPropertyValue == Boolean.FALSE ) {
				assertTrue( isFalseDocument( hits.get( i ) ) );
			}
			else {
				assertTrue( isNullDocument( hits.get( i ) ) );
			}
		}
	}

	private boolean isTrueDocument(DocumentReference documentReference) {
		String id = documentReference.getId();
		return DOCUMENT_1.equals( id ) || DOCUMENT_3.equals( id );
	}

	private boolean isFalseDocument(DocumentReference documentReference) {
		String id = documentReference.getId();
		return DOCUMENT_2.equals( id ) || DOCUMENT_5.equals( id );
	}

	private boolean isNullDocument(DocumentReference documentReference) {
		String id = documentReference.getId();
		return DOCUMENT_4.equals( id );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( indexMapping.bool, true );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.bool, Boolean.FALSE );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( indexMapping.bool, Boolean.TRUE );
		} );
		workPlan.add( referenceProvider( DOCUMENT_4 ), document -> {
			document.addValue( indexMapping.bool, null );
		} );
		workPlan.add( referenceProvider( DOCUMENT_5 ), document -> {
			document.addValue( indexMapping.bool, false );
		} );

		workPlan.execute().join();
		checkAllDocumentsAreSearchable();
	}

	private void checkAllDocumentsAreSearchable() {
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4, DOCUMENT_5 );
	}

	private static class IndexMapping {
		final IndexFieldReference<Boolean> bool;

		IndexMapping(IndexSchemaElement root) {
			bool = root.field( FIELD_PATH, f -> f.asBoolean().sortable( Sortable.YES ) )
					.toReference();
		}
	}
}
