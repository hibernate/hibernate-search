/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@Category(SkipOnElasticsearch.class) // This test is Lucene-specific. The generic equivalent is NumericFieldTest.
public class LuceneNumericFieldTest extends SearchTestBase {

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		prepareData();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		cleanData();
		super.tearDown();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1987")
	public void testOneOfSeveralFieldsIsNumeric() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();

			QueryContextBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder();
			Query query = queryBuilder.forEntity( TouristAttraction.class ).get().all().createQuery();

			@SuppressWarnings("unchecked")
			List<Object[]> list = fullTextSession.createFullTextQuery( query, TouristAttraction.class )
					.setProjection( ProjectionConstants.DOCUMENT )
					.list();

			assertEquals( 1, list.size() );
			Document document = (Document) list.iterator().next()[0];

			IndexableField scoreNumeric = document.getField( "scoreNumeric" );
			assertThat( scoreNumeric.numericValue() ).isEqualTo( 23 );

			IndexableField scoreString = document.getField( "scoreString" );
			assertThat( scoreString.numericValue() ).isNull();
			assertThat( scoreString.stringValue() ).isEqualTo( "23" );

			tx.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1987")
	public void testNumericMappingOfEmbeddedFields() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();

			QueryContextBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder();
			Query query = queryBuilder.forEntity( ScoreBoard.class ).get().all().createQuery();

			@SuppressWarnings("unchecked")
			List<Object[]> list = fullTextSession.createFullTextQuery( query, ScoreBoard.class )
					.setProjection( ProjectionConstants.DOCUMENT )
					.list();

			assertEquals( 1, list.size() );
			Document document = (Document) list.iterator().next()[0];

			IndexableField scoreNumeric = document.getField( "score_id" );
			assertThat( scoreNumeric.numericValue() ).isEqualTo( 1 );

			IndexableField beta = document.getField( "score_beta" );
			assertThat( beta.numericValue() ).isEqualTo( 100 );

			tx.commit();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ TouristAttraction.class, ScoreBoard.class, Score.class };
	}

	private void prepareData() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();

			TouristAttraction attraction = new TouristAttraction( 1, (short) 23, (short) 46L );
			fullTextSession.save( attraction );

			Score score1 = new Score();
			score1.id = 1;
			score1.subscore = 100;

			fullTextSession.save( score1 );

			ScoreBoard scoreboard = new ScoreBoard();
			scoreboard.id = 1l;
			scoreboard.scores.add( score1 );

			fullTextSession.save( scoreboard );

			tx.commit();
		}
	}

	@SuppressWarnings("unchecked")
	private void cleanData() {
		try ( Session session = openSession() ) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction tx = fullTextSession.beginTransaction();

			List<TouristAttraction> attractions = fullTextSession.createCriteria( TouristAttraction.class ).list();
			for ( TouristAttraction attraction : attractions ) {
				fullTextSession.delete( attraction );
			}

			List<ScoreBoard> scoreboards = fullTextSession.createCriteria( ScoreBoard.class ).list();
			for ( ScoreBoard scoreboard : scoreboards ) {
				fullTextSession.delete( scoreboard );
			}

			List<Score> scores = fullTextSession.createCriteria( Score.class ).list();
			for ( Score score : scores ) {
				fullTextSession.delete( score );
			}

			tx.commit();
		}
	}

	@Indexed
	@Entity
	static class ScoreBoard {

		@Id
		Long id;

		@IndexedEmbedded(includeEmbeddedObjectId = true, prefix = "score_")
		@OneToMany
		Set<Score> scores = new HashSet<Score>();

	}

	@Indexed
	@Entity
	static class Score {

		@Id
		@NumericField
		Integer id;

		@Field(name = "beta", store = Store.YES)
		Integer subscore;
	}
}
