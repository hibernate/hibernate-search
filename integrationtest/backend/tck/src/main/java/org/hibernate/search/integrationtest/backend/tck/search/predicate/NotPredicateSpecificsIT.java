/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Arrays;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubLoadingOptionsStep;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class NotPredicateSpecificsIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";
	private static final String DOCUMENT_3 = "3";

	// Document 1

	private static final String FIELD1_VALUE1 = "Irving";
	private static final Integer FIELD2_VALUE1 = 3;
	private static final Integer FIELD3_VALUE1 = 4;
	private static final Integer FIELD4_VALUE1AND2 = 1_000;
	private static final Integer FIELD5_VALUE1AND2 = 2_000;

	// Document 2

	private static final String FIELD1_VALUE2 = "Auster";
	private static final Integer FIELD2_VALUE2 = 13;
	private static final Integer FIELD3_VALUE2 = 14;
	// Field 4: Same as document 1
	// Field 5: Same as document 1

	// Document 3

	private static final String FIELD1_VALUE3 = "Coe";
	private static final Integer FIELD2_VALUE3 = 25;
	private static final Integer FIELD3_VALUE3 = 42;
	private static final Integer FIELD4_VALUE3 = 42_000; // Different from document 1
	private static final Integer FIELD5_VALUE3 = 142_000; // Different from document 1

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void notMatchAll() {
		assertThatQuery( index.query()
				.where( f -> f.not( f.matchAll() ) ) )
				.hasNoHits();
	}

	@Test
	public void notMatchNone() {
		assertThatQuery( index.query()
				.where( f -> f.not( f.matchNone() ) ) )
				.hasTotalHitCount( 3 )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void notMatchSpecificValue() {
		assertThatQuery( index.query()
				.where( f -> f.not( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) ) ) )
				.hasTotalHitCount( 2 )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void mustWithNotInside() {
		assertThatQuery( index.query()
				.where( f -> f.bool().must( f.not( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) ) ) ) )
				.hasTotalHitCount( 2 )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void manyNestedNot() {
		SearchPredicateFactory f = index.createScope().predicate();
		BooleanPredicateClausesStep<?> step = f.bool().must( f.not( f.not( f.not( f.not( f.not( f.not( f.not( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) ) ) ) ) ) ) ) );

		// as query strings are backend specific let's check that generated string is among expected.
		// in this case there's an odd number of nested `not` hence we should end up having mustNot/- query
		SearchQueryOptionsStep<?, DocumentReference, StubLoadingOptionsStep, ?, ?> query = index.query()
				.where( ( step ).toPredicate() );
		assertThat(
				Arrays.asList(
						"+(-field2:[3 TO 3] #*:*)", // Lucene query
						"{\"query\":{\"bool\":{\"must_not\":{\"match\":{\"field2\":{\"query\":3}}}}},\"_source\":false}" // Elasticsearch query
				)
		).contains( query.toQuery().queryString() );

		assertThatQuery( query )
				.hasTotalHitCount( 2 )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_2, DOCUMENT_3 );

		// as query strings are backend specific let's check that generated string is among expected.
		// in this case there's an even number of nested `not` hence we should end up having simple match/+ query
		query = index.query()
				.where( f.not( ( step ) ).toPredicate() );
		assertThat(
				Arrays.asList(
						"+field2:[3 TO 3]", // Lucene query
						"{\"query\":{\"match\":{\"field2\":{\"query\":3}}},\"_source\":false}" // Elasticsearch query
				)
		).contains( query.toQuery().queryString() );

		assertThatQuery( query )
				.hasTotalHitCount( 1 )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	private static void initData() {
		BulkIndexer indexer = index.bulkIndexer();
		indexer.add( DOCUMENT_1, document -> {
			document.addValue( index.binding().field1, FIELD1_VALUE1 );
			document.addValue( index.binding().field2, FIELD2_VALUE1 );
			document.addValue( index.binding().field3, FIELD3_VALUE1 );
			document.addValue( index.binding().field4, FIELD4_VALUE1AND2 );
			document.addValue( index.binding().field5, FIELD5_VALUE1AND2 );
		} );
		indexer.add( DOCUMENT_2, document -> {
			document.addValue( index.binding().field1, FIELD1_VALUE2 );
			document.addValue( index.binding().field2, FIELD2_VALUE2 );
			document.addValue( index.binding().field3, FIELD3_VALUE2 );
			document.addValue( index.binding().field4, FIELD4_VALUE1AND2 );
			document.addValue( index.binding().field5, FIELD5_VALUE1AND2 );
		} );
		indexer.add( DOCUMENT_3, document -> {
			document.addValue( index.binding().field1, FIELD1_VALUE3 );
			document.addValue( index.binding().field2, FIELD2_VALUE3 );
			document.addValue( index.binding().field3, FIELD3_VALUE3 );
			document.addValue( index.binding().field4, FIELD4_VALUE3 );
			document.addValue( index.binding().field5, FIELD5_VALUE3 );
		} );
		indexer.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> field1;
		final IndexFieldReference<Integer> field2;
		final IndexFieldReference<Integer> field3;
		final IndexFieldReference<Integer> field4;
		final IndexFieldReference<Integer> field5;

		IndexBinding(IndexSchemaElement root) {
			field1 = root.field( "field1", f -> f.asString() ).toReference();
			field2 = root.field( "field2", f -> f.asInteger() ).toReference();
			field3 = root.field( "field3", f -> f.asInteger() ).toReference();
			field4 = root.field( "field4", f -> f.asInteger() ).toReference();
			field5 = root.field( "field5", f -> f.asInteger() ).toReference();
		}
	}
}
