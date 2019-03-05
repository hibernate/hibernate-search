/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.MinimumShouldMatchContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.test.SubTest;

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

	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void must() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.must( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
				)
				.toQuery();

		assertThat( query ).hasNoHits();

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.must( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void must_function() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f2 -> f2.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void must_separatePredicateObject() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchPredicate predicate = scope.predicate().match().onField( "field1" ).matching( FIELD1_VALUE1 ).toPredicate();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate(	f -> f.bool().must( predicate ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void should() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE2 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void should_function() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f2 -> f2.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f2 -> f2.match().onField( "field1" ).matching( FIELD1_VALUE2 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void should_separatePredicateObject() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchPredicate predicate1 = scope.predicate().match().onField( "field1" ).matching( FIELD1_VALUE1 ).toPredicate();
		SearchPredicate predicate2 = scope.predicate().match().onField( "field1" ).matching( FIELD1_VALUE3 ).toPredicate();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( predicate1 )
						.should( predicate2 )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void mustNot() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
	}

	@Test
	public void mustNot_function() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.mustNot( f2 -> f2.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void mustNot_separatePredicateObject() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchPredicate predicate = scope.predicate().match().onField( "field1" ).matching( FIELD1_VALUE2 ).toPredicate();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.mustNot( predicate )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void filter() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.filter( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.filter( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.filter( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
				)
				.toQuery();

		assertThat( query ).hasNoHits();

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.filter( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.filter( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void filter_function() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.filter( f2 -> f2.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void filter_separatePredicateObject() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SearchPredicate predicate = scope.predicate().match().onField( "field1" ).matching( FIELD1_VALUE1 ).toPredicate();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate(	f -> f.bool().filter( predicate ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void should_mustNot() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void must_mustNot() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
				)
				.toQuery();

		assertThat( query ).hasNoHits();

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE2 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void nested() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.bool()
										.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
										.should( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						)
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.bool()
								.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
								.should( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						)
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void withConstantScore() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						// 0.287682
						.should( f.bool().must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) ) )

						// withConstantScore 0.287682 => 1
						.should( f.bool().withConstantScore().must( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						// withConstantScore 0.287682 => 1
						.should( f.bool().withConstantScore().must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) ) )

						// 0.287682
						.should( f.bool().must( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void predicateLevelBoost_withConstantScore() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.bool().withConstantScore().boostedTo( 7 ).must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) ) )
						.should( f.bool().withConstantScore().boostedTo( 39 ).must( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_1 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.should( f.bool().withConstantScore().boostedTo( 39 ).must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) ) )
						.should( f.bool().withConstantScore().boostedTo( 7 ).must( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) ) )
				)
				.sort( c -> c.byScore() )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void must_should() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		// A boolean predicate with must + should clauses:
		// documents should match regardless of whether should clauses match.

		// Non-matching "should" clauses
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// One matching and one non-matching "should" clause
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
	}

	@Test
	public void filter_should() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		// A boolean predicate with filter + should clauses:
		// documents should match regardless of whether should clauses match.

		// Non-matching "should" clauses
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.filter( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// One matching and one non-matching "should" clause
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.filter( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void mustNot_should() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		// A boolean predicate with mustNot + should clauses:
		// documents should match only if at least one should clause matches

		// Non-matching "should" clauses
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE2 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasNoHits();

		// One matching and one non-matching "should" clause
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
	}

	@Test
	public void minimumShouldMatchNumber_positive() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		// Expect default behavior (1 "should" clause has to match)
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.minimumShouldMatchNumber( 1 )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchNumber( 1 )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.minimumShouldMatchNumber( 2 )
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Expect to require all "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.minimumShouldMatchNumber( 2 )
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchNumber_negative() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		// Expect default behavior (1 "should" clause has to match)
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.minimumShouldMatchNumber( -1 )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchNumber( -1 )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.minimumShouldMatchNumber( -1 )
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchPercent_positive() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		// Expect default behavior (1 "should" clause has to match)
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.minimumShouldMatchPercent( 50 )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchPercent( 50 )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.minimumShouldMatchPercent( 70 ) // The minimum should be rounded down to 2
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Expect to require all "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.minimumShouldMatchPercent( 100 )
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchPercent_negative() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		// Expect default behavior (1 "should" clause has to match)
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.minimumShouldMatchPercent( -50 )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchPercent( -50 )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool()
						.minimumShouldMatchPercent( -40 ) // The minimum should be rounded up to 2
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
				)
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatch_multipleConstraints() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		Consumer<MinimumShouldMatchContext<?>> minimumShouldMatchConstraints = b -> b
				.ifMoreThan( 2 ).thenRequireNumber( -1 )
				.ifMoreThan( 4 ).thenRequirePercent( 70 );

		// 0 "should" clause: expect the constraints to be ignored
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.must( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		// 1 "should" clause: expect to require all "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 2 "should" clauses: expect to require all "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 3 "should" clauses: expect to require 2 "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 4 "should" clauses: expect to require 3 "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 5 "should" clauses: expect to require 3 "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 6 "should" clauses: expect to require 4 "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field5" ).matching( FIELD5_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatch_multipleConstraints_0ceiling() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		Consumer<MinimumShouldMatchContext<?>> minimumShouldMatchConstraints = b -> b
				// Test that we can set the "default" minimum by using a ceiling of 0
				.ifMoreThan( 0 ).thenRequireNumber( 1 )
				.ifMoreThan( 2 ).thenRequireNumber( -1 )
				.ifMoreThan( 4 ).thenRequirePercent( 70 );

		// 1 "should" clause: expect to require 1 "should" clause to match
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 2 "should" clauses: expect to require 1 "should" clause to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		// 3 "should" clauses: expect to require 2 "should" clauses to match
		query = scope.query()
				.asReference()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) );
				} ) )
				.toQuery();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// The rest should behave exactly as in the other multiple-constraints test
	}

	@Test
	public void minimumShouldMatch_error_negativeCeiling() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SubTest.expectException(
				"minimumShouldMatch constraint with negative ignoreConstraintCeiling",
				() -> scope.predicate().bool().minimumShouldMatch()
						.ifMoreThan( -1 ).thenRequireNumber( 1 )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'ignoreConstraintCeiling'" )
				.hasMessageContaining( "must be positive or zero" );

		SubTest.expectException(
				"minimumShouldMatch constraint with negative ignoreConstraintCeiling",
				() -> scope.predicate().bool().minimumShouldMatch()
						.ifMoreThan( -1 ).thenRequirePercent( 50 )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'ignoreConstraintCeiling'" )
				.hasMessageContaining( "must be positive or zero" );
	}

	@Test
	public void minimumShouldMatch_error_multipleConflictingCeilings() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		SubTest.expectException(
				"bool() predicate with minimumShouldMatch constraints with multiple conflicting ceilings",
				() -> scope.predicate().bool().minimumShouldMatch()
						.ifMoreThan( 2 ).thenRequireNumber( -1 )
						.ifMoreThan( 4 ).thenRequirePercent( 70 )
						.ifMoreThan( 4 ).thenRequirePercent( 70 )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting minimumShouldMatch constraints for ceiling" )
				.hasMessageContaining( "'4'" );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexAccessors.field1.write( document, FIELD1_VALUE1 );
			indexAccessors.field2.write( document, FIELD2_VALUE1 );
			indexAccessors.field3.write( document, FIELD3_VALUE1 );
			indexAccessors.field4.write( document, FIELD4_VALUE1AND2 );
			indexAccessors.field5.write( document, FIELD5_VALUE1AND2 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexAccessors.field1.write( document, FIELD1_VALUE2 );
			indexAccessors.field2.write( document, FIELD2_VALUE2 );
			indexAccessors.field3.write( document, FIELD3_VALUE2 );
			indexAccessors.field4.write( document, FIELD4_VALUE1AND2 );
			indexAccessors.field5.write( document, FIELD5_VALUE1AND2 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexAccessors.field1.write( document, FIELD1_VALUE3 );
			indexAccessors.field2.write( document, FIELD2_VALUE3 );
			indexAccessors.field3.write( document, FIELD3_VALUE3 );
			indexAccessors.field4.write( document, FIELD4_VALUE3 );
			indexAccessors.field5.write( document, FIELD5_VALUE3 );
		} );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchScope scope = indexManager.createSearchScope();
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> field1;
		final IndexFieldAccessor<Integer> field2;
		final IndexFieldAccessor<Integer> field3;
		final IndexFieldAccessor<Integer> field4;
		final IndexFieldAccessor<Integer> field5;

		IndexAccessors(IndexSchemaElement root) {
			field1 = root.field( "field1", f -> f.asString() ).createAccessor();
			field2 = root.field( "field2", f -> f.asInteger() ).createAccessor();
			field3 = root.field( "field3", f -> f.asInteger() ).createAccessor();
			field4 = root.field( "field4", f -> f.asInteger() ).createAccessor();
			field5 = root.field( "field5", f -> f.asInteger() ).createAccessor();
		}
	}
}
