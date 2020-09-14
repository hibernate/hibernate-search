/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test backported from Search 6, was BoolSearchPredicateIT.
 * Should not be ported when merging the Search 6 code, but simply removed.
 */
public class BoolDSLTest {

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
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( IndexedEntity.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Before
	public void setup() {
		initData();
	}

	@Test
	public void must() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );

		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.must( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE2 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query ).matchesNone();

		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.must( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE1 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );
	}

	@Test
	public void should() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.should( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );

		query = helper.hsQuery(
				queryBuilder.bool()
						.should( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE2 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1, DOCUMENT_2 );
	}

	@Test
	public void mustNot() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() ).not()
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_2, DOCUMENT_3 );

		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() ).not()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE3 ).createQuery() ).not()
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_2 );
	}

	@Test
	public void should_mustNot() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.should( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE3 ).createQuery() )
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() ).not()
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_3 );
	}

	@Test
	public void must_mustNot() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() ).not()
				.createQuery()
		);

		helper.assertThat( query ).matchesNone();

		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE2 ).createQuery() ).not()
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );
	}

	@Test
	public void must_should() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		// A boolean predicate with must + should clauses:
		// documents should match regardless of whether should clauses match.

		// Non-matching "should" clauses
		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );

		// One matching and one non-matching "should" clause
		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_2 );
	}

	@Test
	public void filter_should() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		// A boolean predicate with filter + should clauses:
		// documents should match regardless of whether should clauses match.

		// Non-matching "should" clauses
		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() ).disableScoring()
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );

		// One matching and one non-matching "should" clause
		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() ).disableScoring()
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );
	}

	@Test
	public void mustNot_should() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		// A boolean predicate with mustNot + should clauses:
		// documents should match only if at least one should clause matches

		// Non-matching "should" clauses
		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE2 ).createQuery() ).not()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE3 ).createQuery() ).not()
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesNone();

		// One matching and one non-matching "should" clause
		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() ).not()
						.must( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE3 ).createQuery() ).not()
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_2 );
	}

	@Test
	public void minimumShouldMatchNumber_positive() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		// Expect default behavior (1 "should" clause has to match)
		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.minimumShouldMatchNumber( 1 )
						.should( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field4" ).matching( FIELD4_VALUE1AND2 ).createQuery() )
						.minimumShouldMatchNumber( 1 )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = helper.hsQuery(
				queryBuilder.bool()
						.minimumShouldMatchNumber( 2 )
						.should( queryBuilder.keyword().onField( "field4" ).matching( FIELD4_VALUE1AND2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );

		// Expect to require all "should" clauses to match
		query = helper.hsQuery(
				queryBuilder.bool()
						.minimumShouldMatchNumber( 2 )
						.should( queryBuilder.keyword().onField( "field4" ).matching( FIELD4_VALUE1AND2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE1 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchNumber_negative() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		// Expect default behavior (1 "should" clause has to match)
		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.minimumShouldMatchNumber( -1 )
						.should( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field4" ).matching( FIELD4_VALUE1AND2 ).createQuery() )
						.minimumShouldMatchNumber( -1 )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = helper.hsQuery(
				queryBuilder.bool()
						.minimumShouldMatchNumber( -1 )
						.should( queryBuilder.keyword().onField( "field4" ).matching( FIELD4_VALUE1AND2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchPercent_positive() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		// Expect default behavior (1 "should" clause has to match)
		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.minimumShouldMatchPercent( 50 )
						.should( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field4" ).matching( FIELD4_VALUE1AND2 ).createQuery() )
						.minimumShouldMatchPercent( 50 )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = helper.hsQuery(
				queryBuilder.bool()
						.minimumShouldMatchPercent( 70 ) // The minimum should be rounded down to 2
						.should( queryBuilder.keyword().onField( "field4" ).matching( FIELD4_VALUE1AND2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );

		// Expect to require all "should" clauses to match
		query = helper.hsQuery(
				queryBuilder.bool()
						.minimumShouldMatchPercent( 100 )
						.should( queryBuilder.keyword().onField( "field4" ).matching( FIELD4_VALUE1AND2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE1 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatchPercent_negative() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		// Expect default behavior (1 "should" clause has to match)
		HSQuery query = helper.hsQuery(
				queryBuilder.bool()
						.minimumShouldMatchPercent( -50 )
						.should( queryBuilder.keyword().onField( "field1" ).matching( FIELD1_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1, DOCUMENT_3 );

		// Expect to require 1 "should" clause to match even though there's a "must"
		query = helper.hsQuery(
				queryBuilder.bool()
						.must( queryBuilder.keyword().onField( "field4" ).matching( FIELD4_VALUE1AND2 ).createQuery() )
						.minimumShouldMatchPercent( -50 )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_2 );

		// Expect to require 2 "should" clauses to match
		query = helper.hsQuery(
				queryBuilder.bool()
						.minimumShouldMatchPercent( -40 ) // The minimum should be rounded up to 2
						.should( queryBuilder.keyword().onField( "field4" ).matching( FIELD4_VALUE1AND2 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field2" ).matching( FIELD2_VALUE1 ).createQuery() )
						.should( queryBuilder.keyword().onField( "field3" ).matching( FIELD3_VALUE3 ).createQuery() )
				.createQuery()
		);

		helper.assertThat( query )
				.matchesUnorderedIds( DOCUMENT_1 );
	}

	@Test
	public void minimumShouldMatch_error_multipleConflictingConstraints() {
		QueryBuilder queryBuilder = helper.queryBuilder( IndexedEntity.class );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "Multiple conflicting minimumShouldMatch constraints" );

		queryBuilder.bool().minimumShouldMatchNumber( -1 ).minimumShouldMatchPercent( 100 );
	}

	private void initData() {
		helper.index(
				new IndexedEntity(
						DOCUMENT_1,
						FIELD1_VALUE1,
						FIELD2_VALUE1,
						FIELD3_VALUE1,
						FIELD4_VALUE1AND2,
						FIELD5_VALUE1AND2
				),
				new IndexedEntity(
						DOCUMENT_2,
						FIELD1_VALUE2,
						FIELD2_VALUE2,
						FIELD3_VALUE2,
						FIELD4_VALUE1AND2,
						FIELD5_VALUE1AND2
				),
				new IndexedEntity(
						DOCUMENT_3,
						FIELD1_VALUE3,
						FIELD2_VALUE3,
						FIELD3_VALUE3,
						FIELD4_VALUE3,
						FIELD5_VALUE3
				)
		);

		// Check that all documents are searchable
		helper.assertThat( helper.hsQuery( IndexedEntity.class ) )
				.matchesUnorderedIds( DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Indexed
	private static class IndexedEntity {
		@DocumentId
		private String id;
		@Field
		private String field1;
		@Field
		private Integer field2;
		@Field
		private Integer field3;
		@Field
		private Integer field4;
		@Field
		private Integer field5;

		IndexedEntity(String id, String field1, Integer field2, Integer field3, Integer field4, Integer field5) {
			this.id = id;
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.field4 = field4;
			this.field5 = field5;
		}
	}
}
