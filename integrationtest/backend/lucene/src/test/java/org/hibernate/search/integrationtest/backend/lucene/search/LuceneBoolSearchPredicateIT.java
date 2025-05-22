/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class LuceneBoolSearchPredicateIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3535")
	void minimumShouldMatch_outOfBounds() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy(
				() -> scope.query()
						.where( f -> f.bool()
								.minimumShouldMatchNumber( 3 )
								.should( f.match().field( "fieldName" ).matching( "blablabla" ) )
						)
						.toQuery(),
				"bool() predicate with a minimumShouldMatch constraint providing an out-of-bounds value"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Computed minimum for minimumShouldMatch constraint is out of bounds" )
				.hasMessageContaining( "expected a number between '1' and '1', got '3'" );
	}

	@Test
	void resultingQueryOptimization() {
		SearchPredicateFactory f = index.createScope().predicate();
		assertThat(
				index.query()
						.where( f.bool()
								.must( f.not( f.not( f.not( f.not(
										f.not( f.not( f.not( f.match().field( "fieldName" ).matching( "test" ) ) ) ) ) ) ) ) )
								.toPredicate() )
						.toQuery()
						.queryString()
		).isEqualTo( "+(-fieldName:test #*:*) #__HSEARCH_type:main" );

		assertThat(
				index.query()
						.where( f
								.not( f.bool()
										.must( f.not( f.not( f.not( f.not( f.not( f
												.not( f.not( f.match().field( "fieldName" ).matching( "test" ) ) ) ) ) ) ) ) ) )
								.toPredicate() )
						.toQuery()
						.queryString()
		).isEqualTo( "+fieldName:test #__HSEARCH_type:main" );

		assertThat(
				index.query()
						.where( f.bool()
								.must( f.match().field( "fieldName" ).matching( "test1" ) )
								.must( f.not( f.match().field( "fieldName" ).matching( "test2" ) ) )
								.mustNot( f.not( f.match().field( "fieldName" ).matching( "test3" ) ) )
								.mustNot( f.matchNone() )
								.toPredicate()
						)
						.toQuery()
						.queryString()
		).isEqualTo( "+(+fieldName:test1 +fieldName:test3 -MatchNoDocsQuery(\"\") -fieldName:test2) #__HSEARCH_type:main" );
	}

	@Test
	void resultingQueryOptimizationWithBoost() {
		SearchPredicateFactory f = index.createScope().predicate();
		// by default Lucene bool query would have a filter on match all
		assertThat(
				index.query()
						.where( f.not( f.match().field( "fieldName" ).matching( "test" ) ).toPredicate() )
						.toQuery()
						.queryString()
		).isEqualTo( "+(-fieldName:test #*:*) #__HSEARCH_type:main" );

		// but having boost in the not predicate should result in having a *:* as "must"
		assertThat(
				index.query()
						.where(
								f.not( f.match().field( "fieldName" ).matching( "test" ) ).boost( 5.0F ).toPredicate()
						)
						.toQuery()
						.queryString()
		).isEqualTo( "+(-fieldName:test +*:*)^5.0 #__HSEARCH_type:main" );
	}

	@Test
	void nested() {
		String expectedQueryString =
				"+(+fieldName:test +ToParentBlockJoinQuery (#__HSEARCH_type:child #__HSEARCH_nested_document_path:nested +(+nested.integer:[5 TO 10] +nested.text:value))) #__HSEARCH_type:main";

		assertThat(
				index.query().where( f -> f.bool()
						.must( f.match().field( "fieldName" ).matching( "test" ) )
						.must( f.nested( "nested" )
								.add( f.range().field( "nested.integer" )
										.between( 5, 10 ) )
								.add( f.match().field( "nested.text" )
										.matching( "value" )
								) )
				).toQuery()
						.queryString()
		).isEqualTo( expectedQueryString );

		// now the same query but using the bool predicate instead:
		assertThat(
				index.query().where( f -> f.bool()
						.must( f.match().field( "fieldName" ).matching( "test" ) )
						.must( f.nested( "nested" )
								.add(
										f.bool()
												.must( f.range().field( "nested.integer" )
														.between( 5, 10 ) )
												.must( f.match().field( "nested.text" )
														.matching( "value" )
												)
								) )
				).toQuery()
						.queryString()
		).isEqualTo( expectedQueryString );
	}

	@Test
	void onlyNested() {
		//bool query remains as there are > 1 clause
		assertThat(
				index.query().where( f -> f.nested( "nested" )
						.add(
								f.bool()
										.must( f.range().field( "nested.integer" )
												.between( 5, 10 )
										)
										.must( f.match().field( "nested.text" )
												.matching( "value" )
										)
						)
				).toQuery()
						.queryString()
		).isEqualTo(
				"+ToParentBlockJoinQuery (#__HSEARCH_type:child #__HSEARCH_nested_document_path:nested +(+nested.integer:[5 TO 10] +nested.text:value)) #__HSEARCH_type:main" );

		// bool query is removed as there's only 1 clause
		assertThat(
				index.query().where( f -> f.nested( "nested" )
						.add(
								f.bool()
										.must( f.range().field( "nested.integer" )
												.between( 5, 10 ) )
						)
				).toQuery()
						.queryString()
		).isEqualTo(
				"+ToParentBlockJoinQuery (#__HSEARCH_type:child #__HSEARCH_nested_document_path:nested +nested.integer:[5 TO 10]) #__HSEARCH_type:main" );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> field;

		IndexBinding(IndexSchemaElement root) {
			field = root.field( "fieldName", c -> c.asString() ).toReference();
			IndexSchemaObjectField nested = root.objectField( "nested", ObjectStructure.NESTED );
			nested.toReference();

			nested.field( "text", c -> c.asString() ).toReference();
			nested.field( "integer", c -> c.asInteger() ).toReference();
		}
	}
}
