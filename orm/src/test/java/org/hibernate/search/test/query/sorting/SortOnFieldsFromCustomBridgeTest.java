/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.sorting;

import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.query.Book;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for sortable fields contributed through custom field bridges. Those bridges must expose the required sortable
 * field meta-data so they can be used for sorting without falling back to index uninverting.
 *
 * @author Gunnar Morling
 */
@TestForIssue(jiraKey = "HSEARCH-2021")
public class SortOnFieldsFromCustomBridgeTest extends SearchTestBase {

	@Before
	public void insertTestData() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		Territory novaScotia = new Territory( 1, "Nova Scotia" );
		fullTextSession.save( novaScotia );
		Territory alaska = new Territory( 2, "Alaska" );
		fullTextSession.save( alaska );
		Territory tierraDelFuego = new Territory( 3, "Tierra del Fuego" );
		fullTextSession.save( tierraDelFuego );

		fullTextSession.save( new Explorer( 1, 23, novaScotia, "Sam", "1st", "Seaman" ) );
		fullTextSession.save( new Explorer( 2, 22, alaska, "Sam", "2nd", "Traveller" ) );
		fullTextSession.save( new Explorer( 3, 22, tierraDelFuego, "Collin", "1st", "Conqueror" ) );

		tx.commit();
		fullTextSession.close();
	}

	@After
	public void deleteTestData() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		fullTextSession.delete( new Explorer( 1 ) );
		fullTextSession.delete( new Explorer( 2 ) );
		fullTextSession.delete( new Explorer( 3 ) );

		fullTextSession.delete( new Territory( 1 ) );
		fullTextSession.delete( new Territory( 2 ) );
		fullTextSession.delete( new Territory( 3 ) );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testSortableFieldConfiguredThroughClassLevelBridge() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		@SuppressWarnings("unchecked")
		List<Book> result = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Explorer.class )
			.setSort(
					new Sort(
							new SortField( "fn_firstName", SortField.Type.STRING ),
							new SortField( "fn_middleName", SortField.Type.STRING )
					)
			)
			.list();

		assertNotNull( result );
		assertThat( result ).onProperty( "id" ).containsExactly( 3, 1, 2 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testSortableFieldConfiguredThroughCustomFieldLevelBridge() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		@SuppressWarnings("unchecked")
		List<Book> result = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Explorer.class )
			.setSort( new Sort( new SortField( "nameParts_lastName", SortField.Type.STRING ) ) )
			.list();

		assertNotNull( result );
		assertThat( result ).onProperty( "id" ).containsExactly( 3, 1, 2 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testTwoSortableFieldsConfiguredThroughAnnotationAndCustomFieldLevelBridge() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		@SuppressWarnings("unchecked")
		List<Book> result = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Explorer.class )
			.setSort(
					new Sort(
							new SortField( "exploredCountries", SortField.Type.INT ),
							new SortField( "nameParts_lastName", SortField.Type.STRING )
					)
			)
			.list();

		assertNotNull( result );
		assertThat( result ).onProperty( "id" ).containsExactly( 3, 2, 1 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2325")
	public void testNumericCustomFieldLevelBridge() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		@SuppressWarnings("unchecked")
		List<Book> result = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Explorer.class )
			.setSort(
					new Sort(
							new SortField( "favoriteTerritory.idFromBridge", SortField.Type.INT )
					)
			)
			.list();

		assertNotNull( result );
		assertThat( result ).onProperty( "id" ).containsExactly( 1, 2, 3 );

		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testSortableFieldConfiguredThroughClassLevelBridgeOnEmbeddedEntity() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		Transaction tx = fullTextSession.beginTransaction();

		@SuppressWarnings("unchecked")
		List<Book> result = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Explorer.class )
			.setSort( new Sort( new SortField( "favoriteTerritory.territoryName", SortField.Type.STRING ) ) )
			.list();

		assertNotNull( result );
		assertThat( result ).onProperty( "id" ).containsExactly( 2, 1, 3 );

		tx.commit();
		fullTextSession.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Explorer.class, Territory.class };
	}

}
