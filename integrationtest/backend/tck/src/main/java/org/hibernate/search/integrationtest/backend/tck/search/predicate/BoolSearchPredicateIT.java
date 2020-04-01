/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.predicate.dsl.MinimumShouldMatchConditionStep;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.util.common.SearchException;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BoolSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

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

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void must() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.must( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) )
				)
				.toQuery();

		assertThat( query ).hasNoHits();

		query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.must( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void must_function() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.must( f2 -> f2.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void must_separatePredicateObject() {
		StubMappingScope scope = indexManager.createScope();

		SearchPredicate predicate = scope.predicate().match().field( "field1" ).matching( FIELD1_VALUE1 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.where(	f -> f.bool().must( predicate ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void should() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().field( "field1" ).matching( FIELD1_VALUE2 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void should_function() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f2 -> f2.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f2 -> f2.match().field( "field1" ).matching( FIELD1_VALUE2 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void should_separatePredicateObject() {
		StubMappingScope scope = indexManager.createScope();

		SearchPredicate predicate1 = scope.predicate().match().field( "field1" ).matching( FIELD1_VALUE1 ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().match().field( "field1" ).matching( FIELD1_VALUE3 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( predicate1 )
						.should( predicate2 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void mustNot() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

		query = scope.query()
				.where( f -> f.bool()
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
	}

	@Test
	public void mustNot_function() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.mustNot( f2 -> f2.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void mustNot_separatePredicateObject() {
		StubMappingScope scope = indexManager.createScope();

		SearchPredicate predicate = scope.predicate().match().field( "field1" ).matching( FIELD1_VALUE2 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.mustNot( predicate )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void filter() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.filter( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.filter( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.filter( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) )
				)
				.toQuery();

		assertThat( query ).hasNoHits();

		query = scope.query()
				.where( f -> f.bool()
						.filter( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.filter( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void filter_function() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.filter( f2 -> f2.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void filter_separatePredicateObject() {
		StubMappingScope scope = indexManager.createScope();

		SearchPredicate predicate = scope.predicate().match().field( "field1" ).matching( FIELD1_VALUE1 ).toPredicate();

		SearchQuery<DocumentReference> query = scope.query()
				.where(	f -> f.bool().filter( predicate ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void should_mustNot() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) )
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void must_mustNot() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query ).hasNoHits();

		query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE2 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void nested() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.must( f.bool()
										.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
										.should( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) )
						)
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		query = scope.query()
				.where( f -> f.bool()
						.must( f.bool()
								.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
								.should( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) )
						)
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void constantScore() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						// 0.287682
						.should( f.bool().must( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) ) )

						// withConstantScore 0.287682 => 1
						.should( f.bool().constantScore().must( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) ) )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						// withConstantScore 0.287682 => 1
						.should( f.bool().constantScore().must( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) ) )

						// 0.287682
						.should( f.bool().must( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) ) )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void predicateLevelBoost_withConstantScore() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.bool().constantScore().boost( 7 ).must( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) ) )
						.should( f.bool().constantScore().boost( 39 ).must( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) ) )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );

		query = scope.query()
				.where( f -> f.bool()
						.should( f.bool().constantScore().boost( 39 ).must( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) ) )
						.should( f.bool().constantScore().boost( 7 ).must( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) ) )
				)
				.sort( f -> f.score() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void must_should() {
		StubMappingScope scope = indexManager.createScope();

		// A boolean predicate with must + should clauses:
		// documents should match regardless of whether should clauses match.

		// Non-matching "should" clauses
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// One matching and one non-matching "should" clause
		query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field1" ).matching( FIELD1_VALUE2 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
	}

	@Test
	public void filter_should() {
		StubMappingScope scope = indexManager.createScope();

		// A boolean predicate with filter + should clauses:
		// documents should match regardless of whether should clauses match.

		// Non-matching "should" clauses
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.filter( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// One matching and one non-matching "should" clause
		query = scope.query()
				.where( f -> f.bool()
						.filter( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void mustNot_should() {
		StubMappingScope scope = indexManager.createScope();

		// A boolean predicate with mustNot + should clauses:
		// documents should match only if at least one should clause matches

		// Non-matching "should" clauses
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE2 ) )
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasNoHits();

		// One matching and one non-matching "should" clause
		query = scope.query()
				.where( f -> f.bool()
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().field( "field1" ).matching( FIELD1_VALUE3 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
	}

	@Test
	public void minimumShouldMatchNumber_positive() {
		StubMappingScope scope = indexManager.createScope();

		// Expect default behavior (1 "should" clause has to match)
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.minimumShouldMatchNumber( 1 )
						.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchNumber( 1 )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = scope.query()
				.where( f -> f.bool()
						.minimumShouldMatchNumber( 2 )
						.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Expect to require all "should" clauses to match
		query = scope.query()
				.where( f -> f.bool()
						.minimumShouldMatchNumber( 2 )
						.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchNumber_negative() {
		StubMappingScope scope = indexManager.createScope();

		// Expect default behavior (1 "should" clause has to match)
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.minimumShouldMatchNumber( -1 )
						.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchNumber( -1 )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = scope.query()
				.where( f -> f.bool()
						.minimumShouldMatchNumber( -1 )
						.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchPercent_positive() {
		StubMappingScope scope = indexManager.createScope();

		// Expect default behavior (1 "should" clause has to match)
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.minimumShouldMatchPercent( 50 )
						.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchPercent( 50 )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = scope.query()
				.where( f -> f.bool()
						.minimumShouldMatchPercent( 70 ) // The minimum should be rounded down to 2
						.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Expect to require all "should" clauses to match
		query = scope.query()
				.where( f -> f.bool()
						.minimumShouldMatchPercent( 100 )
						.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchPercent_negative() {
		StubMappingScope scope = indexManager.createScope();

		// Expect default behavior (1 "should" clause has to match)
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.minimumShouldMatchPercent( -50 )
						.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = scope.query()
				.where( f -> f.bool()
						.must( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchPercent( -50 )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = scope.query()
				.where( f -> f.bool()
						.minimumShouldMatchPercent( -40 ) // The minimum should be rounded up to 2
						.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().field( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatch_multipleConstraints() {
		StubMappingScope scope = indexManager.createScope();

		Consumer<MinimumShouldMatchConditionStep<?>> minimumShouldMatchConstraints = b -> b
				.ifMoreThan( 2 ).thenRequireNumber( -1 )
				.ifMoreThan( 4 ).thenRequirePercent( 70 );

		// 0 "should" clause: expect the constraints to be ignored
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.must( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		// 1 "should" clause: expect to require all "should" clauses to match
		query = scope.query()
				.where( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 2 "should" clauses: expect to require all "should" clauses to match
		query = scope.query()
				.where( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 3 "should" clauses: expect to require 2 "should" clauses to match
		query = scope.query()
				.where( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE3 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 4 "should" clauses: expect to require 3 "should" clauses to match
		query = scope.query()
				.where( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 5 "should" clauses: expect to require 3 "should" clauses to match
		query = scope.query()
				.where( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE3 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 6 "should" clauses: expect to require 4 "should" clauses to match
		query = scope.query()
				.where( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().field( "field5" ).matching( FIELD5_VALUE1AND2 ) );
					b.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE1 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE3 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatch_multipleConstraints_0ceiling() {
		StubMappingScope scope = indexManager.createScope();

		Consumer<MinimumShouldMatchConditionStep<?>> minimumShouldMatchConstraints = b -> b
				// Test that we can set the "default" minimum by using a ceiling of 0
				.ifMoreThan( 0 ).thenRequireNumber( 1 )
				.ifMoreThan( 2 ).thenRequireNumber( -1 )
				.ifMoreThan( 4 ).thenRequirePercent( 70 );

		// 1 "should" clause: expect to require 1 "should" clause to match
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 2 "should" clauses: expect to require 1 "should" clause to match
		query = scope.query()
				.where( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE2 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		// 3 "should" clauses: expect to require 2 "should" clauses to match
		query = scope.query()
				.where( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().field( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().field( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().field( "field2" ).matching( FIELD2_VALUE3 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// The rest should behave exactly as in the other multiple-constraints test
	}

	@Test
	public void minimumShouldMatch_error_negativeCeiling() {
		StubMappingScope scope = indexManager.createScope();

		Assertions.assertThatThrownBy(
				() -> scope.predicate().bool().minimumShouldMatch()
						.ifMoreThan( -1 ).thenRequireNumber( 1 ),
				"minimumShouldMatch constraint with negative ignoreConstraintCeiling"
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'ignoreConstraintCeiling'" )
				.hasMessageContaining( "must be positive or zero" );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().bool().minimumShouldMatch()
						.ifMoreThan( -1 ).thenRequirePercent( 50 ),
				"minimumShouldMatch constraint with negative ignoreConstraintCeiling"
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'ignoreConstraintCeiling'" )
				.hasMessageContaining( "must be positive or zero" );
	}

	@Test
	public void minimumShouldMatch_error_multipleConflictingCeilings() {
		StubMappingScope scope = indexManager.createScope();

		Assertions.assertThatThrownBy(
				() -> scope.predicate().bool().minimumShouldMatch()
						.ifMoreThan( 2 ).thenRequireNumber( -1 )
						.ifMoreThan( 4 ).thenRequirePercent( 70 )
						.ifMoreThan( 4 ).thenRequirePercent( 70 ),
				"bool() predicate with minimumShouldMatch constraints with multiple conflicting ceilings"
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting minimumShouldMatch constraints for ceiling" )
				.hasMessageContaining( "'4'" );
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-3534" )
	public void minimumShouldMatch_default() {
		StubMappingScope scope = indexManager.createScope();

		// If the should is alone ( not having any sibling must ),
		// the default minimum should match will be 1.
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( "field1" ).matching( "no-match" ) )
				)
				.toQuery();

		assertThat( query ).hasNoHits();

		// If the should has a sibling must,
		// the default minimum should match will be 0.
		query = scope.query()
				.where( f -> f.bool()
						.should( f.match().field( "field1" ).matching( "no-match" ) )
						.must( f.match().field( "field5" ).matching( FIELD5_VALUE1AND2 ) ) // match 1 and 2
				)
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		// If there exists a must or a filter, but they are not a sibling of the should,
		// the default minimum should match will be 1.
		query = scope.query()
				.where( f -> f.bool()
						.filter( f.bool()
								.should( f.match().field( "field1" ).matching( "no-match" ) )
						)
						.must( f.match().field( "field3" ).matching( FIELD3_VALUE1 ) ) // match 1
				)
				.toQuery();

		assertThat( query ).hasNoHits();
	}

	@Test
	@TestForIssue( jiraKey = "HSEARCH-3534" )
	public void minimumShouldMatch_default_withinFilter_mustSibling() {
		StubMappingScope scope = indexManager.createScope();

		// We're following here the Lucene's conventions.
		// If the should has a sibling must, even if the should is inside a filter,
		// the default minimum should match will be 0.
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.filter( f.bool()
								.should( f.match().field( "field1" ).matching( "no-match" ) )
								.must( f.match().field( "field5" ).matching( FIELD5_VALUE1AND2 ) ) // match 1 and 2
						)
				)
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void minimumShouldMatch_default_withinFilter_mustNotSibling() {
		StubMappingScope scope = indexManager.createScope();

		// Differently from must predicate,
		// if the should has a sibling must-not inside a filter,
		// the default minimum should match will be still 1.
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.filter( f.bool()
								.should( f.match().field( "field1" ).matching( "no-match" ) )
								.mustNot( f.match().field( "field5" ).matching( FIELD5_VALUE1AND2 ) ) // match 3
						)
				)
				.toQuery();

		assertThat( query ).hasNoHits();
	}

	@Test
	public void minimumShouldMatch_default_withinFilter_filterSibling() {
		StubMappingScope scope = indexManager.createScope();

		// We're following here the Lucene's conventions.
		// If the should has a sibling filter, even if the should is inside a filter,
		// the default minimum should match will be 0.
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.bool()
						.filter( f.bool()
								.should( f.match().field( "field1" ).matching( "no-match" ) )
								.filter( p -> f.match().field( "field5" ).matching( FIELD5_VALUE1AND2 ) ) // match 1 and 2
						)
				)
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	private void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( indexMapping.field1, FIELD1_VALUE1 );
			document.addValue( indexMapping.field2, FIELD2_VALUE1 );
			document.addValue( indexMapping.field3, FIELD3_VALUE1 );
			document.addValue( indexMapping.field4, FIELD4_VALUE1AND2 );
			document.addValue( indexMapping.field5, FIELD5_VALUE1AND2 );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.field1, FIELD1_VALUE2 );
			document.addValue( indexMapping.field2, FIELD2_VALUE2 );
			document.addValue( indexMapping.field3, FIELD3_VALUE2 );
			document.addValue( indexMapping.field4, FIELD4_VALUE1AND2 );
			document.addValue( indexMapping.field5, FIELD5_VALUE1AND2 );
		} );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( indexMapping.field1, FIELD1_VALUE3 );
			document.addValue( indexMapping.field2, FIELD2_VALUE3 );
			document.addValue( indexMapping.field3, FIELD3_VALUE3 );
			document.addValue( indexMapping.field4, FIELD4_VALUE3 );
			document.addValue( indexMapping.field5, FIELD5_VALUE3 );
		} );

		plan.execute().join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> field1;
		final IndexFieldReference<Integer> field2;
		final IndexFieldReference<Integer> field3;
		final IndexFieldReference<Integer> field4;
		final IndexFieldReference<Integer> field5;

		IndexMapping(IndexSchemaElement root) {
			field1 = root.field( "field1", f -> f.asString() ).toReference();
			field2 = root.field( "field2", f -> f.asInteger() ).toReference();
			field3 = root.field( "field3", f -> f.asInteger() ).toReference();
			field4 = root.field( "field4", f -> f.asInteger() ).toReference();
			field5 = root.field( "field5", f -> f.asInteger() ).toReference();
		}
	}
}
