/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.embedded.sorting;

import static org.fest.assertions.Assertions.assertThat;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "HSEARCH-2069")
public class EmbeddedSortableIdFieldTest extends SearchTestBase {

	private static final String LEX = "Lex Luthor";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Before
	public void before() throws Exception {
		try (Session session = openSession()) {
			Transaction transaction = session.beginTransaction();
			Villain lex = new Villain( Integer.MIN_VALUE, LEX );
			Hero hero = new Hero( Integer.MAX_VALUE );

			hero.setVillain( lex );
			lex.setHero( hero );

			session.save( hero );
			session.save( lex );

			transaction.commit();
		}
	}

	@Test
	public void testExceptionForNotEmbeddedField() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000301" );

		try (Session session = openSession()) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction transaction = fullTextSession.beginTransaction();

			Sort sort = new Sort( new SortField( "villain.name", SortField.Type.STRING ) );

			QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Hero.class ).get();
			Query q = queryBuilder.keyword().onField( "villain.name" ).matching( LEX ).createQuery();
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q, Hero.class );
			fullTextQuery.setSort( sort );
			fullTextQuery.list();

			transaction.commit();
		}
	}

	@Test
	public void testSortableOnIndexedFieldId() {
		try (Session session = openSession()) {
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			Transaction transaction = fullTextSession.beginTransaction();

			Sort sort = new Sort( new SortField( "id", SortField.Type.INT ) );

			QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Villain.class ).get();
			Query q = queryBuilder.keyword().onField( "name" ).matching( LEX ).createQuery();
			FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( q, Hero.class );
			fullTextQuery.setSort( sort );

			assertThat( fullTextQuery.list() ).onProperty( "name" ).containsExactly( LEX );
			transaction.commit();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Hero.class, Villain.class };
	}

}
