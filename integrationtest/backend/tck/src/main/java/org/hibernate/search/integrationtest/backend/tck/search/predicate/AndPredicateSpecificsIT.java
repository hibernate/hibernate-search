/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AndPredicateSpecificsIT {

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

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	void empty() {
		assertThatQuery( index.query()
				.where( f -> f.and() ) )
				.hasNoHits();
	}

	@Test
	void and() {
		assertThatQuery( index.query()
				.where( f -> f.and( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( index.query()
				.where( f -> f.and(
						f.match().field( "field1" ).matching( FIELD1_VALUE1 ),
						f.match().field( "field2" ).matching( FIELD2_VALUE2 )
				) ) )
				.hasNoHits();

		assertThatQuery( index.query()
				.where( f -> f.and(
						f.match().field( "field1" ).matching( FIELD1_VALUE1 ),
						f.match().field( "field2" ).matching( FIELD2_VALUE1 )
				) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void and_separatePredicateObject() {
		StubMappingScope scope = index.createScope();

		SearchPredicate predicate1 = scope.predicate().match().field( "field1" ).matching( FIELD1_VALUE1 ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().match().field( "field2" ).matching( FIELD2_VALUE1 ).toPredicate();
		SearchPredicate predicate3 = scope.predicate().match().field( "field2" ).matching( FIELD2_VALUE2 ).toPredicate();

		assertThatQuery( scope.query()
				.where( f -> f.and( predicate1 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( index.query()
				.where( f -> f.and(
						predicate1,
						predicate3
				) ) )
				.hasNoHits();

		assertThatQuery( index.query()
				.where( f -> f.and(
						predicate1,
						predicate2
				) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void nested() {
		assertThatQuery( index.query()
				.where( f -> f.and( f.or(
						f.match().field( "field1" ).matching( FIELD1_VALUE1 ),
						f.match().field( "field1" ).matching( FIELD1_VALUE3 )
				) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	void add() {
		assertThatQuery( index.query()
				.where( f -> f.and()
						.add( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( index.query()
				.where( f -> f.and()
						.add( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.add( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) ) ) )
				.hasNoHits();

		assertThatQuery( index.query()
				.where( f -> f.and()
						.add( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.add( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void add_separatePredicateObject() {
		StubMappingScope scope = index.createScope();

		SearchPredicate predicate1 = scope.predicate().match().field( "field1" ).matching( FIELD1_VALUE1 ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().match().field( "field2" ).matching( FIELD2_VALUE1 ).toPredicate();
		SearchPredicate predicate3 = scope.predicate().match().field( "field2" ).matching( FIELD2_VALUE2 ).toPredicate();

		assertThatQuery( index.query()
				.where( f -> f.and()
						.add( predicate1 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( index.query()
				.where( f -> f.and()
						.add( predicate1 )
						.add( predicate3 ) ) )
				.hasNoHits();

		assertThatQuery( index.query()
				.where( f -> f.and()
						.add( predicate1 )
						.add( predicate2 ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void add_function() {
		assertThatQuery( index.query()
				.where( f -> f.and()
						.add( f2 -> f2.match().field( "field1" ).matching( FIELD1_VALUE1 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( index.query()
				.where( f -> f.and()
						.add( f2 -> f2.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.add( f2 -> f2.match().field( "field2" ).matching( FIELD2_VALUE2 ) ) ) )
				.hasNoHits();

		assertThatQuery( index.query()
				.where( f -> f.and()
						.add( f2 -> f2.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.add( f2 -> f2.match().field( "field2" ).matching( FIELD2_VALUE1 ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void where() {
		assertThatQuery( index.query()
				.where( (f, root) -> {
					root.add( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
				} ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( index.query()
				.where( (f, root) -> {
					root.add( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
					root.add( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) );
				} ) )
				.hasNoHits();

		assertThatQuery( index.query()
				.where( (f, root) -> {
					root.add( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
					root.add( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) );
				} ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void with() {
		assertThatQuery( index.query()
				.where( f -> f.and().with( and -> and.add( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThatQuery( index.query()
				.where( f -> f.and().with( and -> {
					and.add( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
					and.add( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) );
				} ) ) )
				.hasNoHits();

		assertThatQuery( index.query()
				.where( f -> f.and()
						.with( and -> and.add( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) ) )
						.with( and -> and.add( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) ) ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	void hasClause() {
		StubMappingScope scope = index.createScope();

		assertThat(
				scope.predicate().and().hasClause()
		).isFalse();

		assertThat(
				scope.predicate().and()
						.with( and -> and.add( f -> f.match().field( "field1" ).matching( FIELD1_VALUE1 ) ) )
						.with( and -> and.add( f -> f.match().field( "field2" ).matching( FIELD2_VALUE1 ) ) )
						.hasClause()
		).isTrue();
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
			field1 = root.field( "field1", IndexFieldTypeFactory::asString ).toReference();
			field2 = root.field( "field2", IndexFieldTypeFactory::asInteger ).toReference();
			field3 = root.field( "field3", IndexFieldTypeFactory::asInteger ).toReference();
			field4 = root.field( "field4", IndexFieldTypeFactory::asInteger ).toReference();
			field5 = root.field( "field5", IndexFieldTypeFactory::asInteger ).toReference();
		}
	}
}
