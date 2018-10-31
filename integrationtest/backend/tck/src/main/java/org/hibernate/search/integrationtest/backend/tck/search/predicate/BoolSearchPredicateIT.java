/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.function.Consumer;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.MinimumShouldMatchContext;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
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
	private MappedIndexManager<?> indexManager;
	private SessionContext sessionContext = new StubSessionContext();

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void must() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.must( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query ).hasNoHits();

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.must( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void must_function() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f2 -> f2.match().onField( "field1" ).matching( FIELD1_VALUE1 ).toPredicate() )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void must_separatePredicateObject() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchPredicate predicate = searchTarget.predicate().match().onField( "field1" ).matching( FIELD1_VALUE1 ).toPredicate();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate(	f -> f.bool().must( predicate ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void should() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE2 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void should_function() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.should( f2 -> f2.match().onField( "field1" ).matching( FIELD1_VALUE1 ).toPredicate() )
						.should( f2 -> f2.match().onField( "field1" ).matching( FIELD1_VALUE2 ).toPredicate() )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void should_separatePredicateObject() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchPredicate predicate1 = searchTarget.predicate().match().onField( "field1" ).matching( FIELD1_VALUE1 ).toPredicate();
		SearchPredicate predicate2 = searchTarget.predicate().match().onField( "field1" ).matching( FIELD1_VALUE3 ).toPredicate();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.should( predicate1 )
						.should( predicate2 )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void mustNot() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
	}

	@Test
	public void mustNot_function() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.mustNot( f2 -> f2.match().onField( "field1" ).matching( FIELD1_VALUE1 ).toPredicate() )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void mustNot_separatePredicateObject() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchPredicate predicate = searchTarget.predicate().match().onField( "field1" ).matching( FIELD1_VALUE2 ).toPredicate();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.mustNot( predicate )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void should_mustNot() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void must_mustNot() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query ).hasNoHits();

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE2 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void nested() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.bool()
										.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
										.should( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						)
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.bool()
								.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
								.should( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						)
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void must_should() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// A boolean predicate with must + should clauses:
		// documents should match regardless of whether should clauses match.

		// Non-matching "should" clauses
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// One matching and one non-matching "should" clause
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field1" ).matching( FIELD1_VALUE2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
	}

	@Test
	public void filter_should() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// A boolean predicate with filter + should clauses:
		// documents should match regardless of whether should clauses match.

		// Non-matching "should" clauses
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.filter( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// One matching and one non-matching "should" clause
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.filter( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void mustNot_should() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// A boolean predicate with mustNot + should clauses:
		// documents should match only if at least one should clause matches

		// Non-matching "should" clauses
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE2 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasNoHits();

		// One matching and one non-matching "should" clause
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.mustNot( f.match().onField( "field1" ).matching( FIELD1_VALUE3 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );
	}

	@Test
	public void minimumShouldMatchNumber_positive() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// Expect default behavior (1 "should" clause has to match)
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.minimumShouldMatchNumber( 1 )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchNumber( 1 )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.minimumShouldMatchNumber( 2 )
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Expect to require all "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.minimumShouldMatchNumber( 2 )
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchNumber_negative() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// Expect default behavior (1 "should" clause has to match)
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.minimumShouldMatchNumber( -1 )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchNumber( -1 )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.minimumShouldMatchNumber( -1 )
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchPercent_positive() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// Expect default behavior (1 "should" clause has to match)
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.minimumShouldMatchPercent( 50 )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchPercent( 50 )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.minimumShouldMatchPercent( 70 ) // The minimum should be rounded down to 2
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// Expect to require all "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.minimumShouldMatchPercent( 100 )
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchPercent_negative() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		// Expect default behavior (1 "should" clause has to match)
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.minimumShouldMatchPercent( -50 )
						.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.must( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.minimumShouldMatchPercent( -50 )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool()
						.minimumShouldMatchPercent( -40 ) // The minimum should be rounded up to 2
						.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) )
						.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) )
						.should( f.match().onField( "field3" ).matching( FIELD3_VALUE3 ) )
						.toPredicate()
				)
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatch_multipleConstraints() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		Consumer<MinimumShouldMatchContext<?>> minimumShouldMatchConstraints = b -> b
				.ifMoreThan( 2 ).thenRequireNumber( -1 )
				.ifMoreThan( 4 ).thenRequirePercent( 70 );

		// 0 "should" clause: expect the constraints to be ignored
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.must( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
				} ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		// 1 "should" clause: expect to require all "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
				} ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 2 "should" clauses: expect to require all "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
				} ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 3 "should" clauses: expect to require 2 "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) );
				} ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 4 "should" clauses: expect to require 3 "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) );
				} ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 5 "should" clauses: expect to require 3 "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) );
				} ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 6 "should" clauses: expect to require 4 "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field5" ).matching( FIELD5_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) );
				} ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatch_multipleConstraints_0ceiling() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		Consumer<MinimumShouldMatchContext<?>> minimumShouldMatchConstraints = b -> b
				// Test that we can set the "default" minimum by using a ceiling of 0
				.ifMoreThan( 0 ).thenRequireNumber( 1 )
				.ifMoreThan( 2 ).thenRequireNumber( -1 )
				.ifMoreThan( 4 ).thenRequirePercent( 70 );

		// 1 "should" clause: expect to require 1 "should" clause to match
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
				} ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// 2 "should" clauses: expect to require 1 "should" clause to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE2 ) );
				} ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		// 3 "should" clauses: expect to require 2 "should" clauses to match
		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.bool( b -> {
					b.minimumShouldMatch( minimumShouldMatchConstraints );
					b.should( f.match().onField( "field4" ).matching( FIELD4_VALUE1AND2 ) );
					b.should( f.match().onField( "field1" ).matching( FIELD1_VALUE1 ) );
					b.should( f.match().onField( "field2" ).matching( FIELD2_VALUE3 ) );
				} ).toPredicate() )
				.build();

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// The rest should behave exactly as in the other multiple-constraints test
	}

	@Test
	public void minimumShouldMatch_error_negativeCeiling() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"minimumShouldMatch constraint with negative ignoreConstraintCeiling",
				() -> searchTarget.predicate().bool().minimumShouldMatch()
						.ifMoreThan( -1 ).thenRequireNumber( 1 )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'ignoreConstraintCeiling'" )
				.hasMessageContaining( "must be positive or zero" );

		SubTest.expectException(
				"minimumShouldMatch constraint with negative ignoreConstraintCeiling",
				() -> searchTarget.predicate().bool().minimumShouldMatch()
						.ifMoreThan( -1 ).thenRequirePercent( 50 )
		)
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'ignoreConstraintCeiling'" )
				.hasMessageContaining( "must be positive or zero" );
	}

	@Test
	public void minimumShouldMatch_error_multipleConflictingCeilings() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SubTest.expectException(
				"bool() predicate with minimumShouldMatch constraints with multiple conflicting ceilings",
				() -> searchTarget.predicate().bool().minimumShouldMatch()
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
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
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
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> field1;
		final IndexFieldAccessor<Integer> field2;
		final IndexFieldAccessor<Integer> field3;
		final IndexFieldAccessor<Integer> field4;
		final IndexFieldAccessor<Integer> field5;

		IndexAccessors(IndexSchemaElement root) {
			field1 = root.field( "field1" ).asString().createAccessor();
			field2 = root.field( "field2" ).asInteger().createAccessor();
			field3 = root.field( "field3" ).asInteger().createAccessor();
			field4 = root.field( "field4" ).asInteger().createAccessor();
			field5 = root.field( "field5" ).asInteger().createAccessor();
		}
	}
}
