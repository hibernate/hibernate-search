/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.event.autoindexembeddable;

import java.util.List;
import javax.persistence.EntityManager;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.test.jpa.JPATestCase;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1358")
public class EventBasedEmbeddableCollectionUpdateTest extends JPATestCase {

	private EntityManager entityManager;

	@Before
	@Override
	public void setUp() {
		super.setUp();
		entityManager = factory.createEntityManager();
	}

	@After
	@Override
	public void tearDown() {
		FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager( entityManager );
		fullTextEntityManager.purgeAll( Book.class );
		fullTextEntityManager.flushToIndexes();
		fullTextEntityManager.close();
		super.tearDown();
	}

	@Test
	public void testUpdateOfEmbeddedElementCollectionTriggersIndexUpdate() throws Exception {
		indexBookAndEnsureItIsIndexed();

		// find and update book by adding "Bar" into Embedded ElementCollection
		entityManager.getTransaction().begin();

		Book book = entityManager.find( Book.class, 1234L );
		book.getEmbeddableCategories().getCategories().remove( 12L );
		book.getEmbeddableCategories().getCategories().put( 13L, "Bar" );

		entityManager.persist( book );
		entityManager.getTransaction().commit();

		assertEquals(
				"Foo should have been removed by indexed update",
				0,
				search( "embeddableCategories.categories:Foo" ).size()
		);
		assertEquals(
				"Bar should have been added by indexed update",
				1,
				search( "embeddableCategories.categories:Bar" ).size()
		);
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { }; // configured in persistence.xml
	}

	private void indexBookAndEnsureItIsIndexed() throws ParseException {
		entityManager.getTransaction().begin();

		Book book = new Book();
		book.setId( 1234L );
		book.getEmbeddableCategories().getCategories().put( 12L, "Foo" );

		entityManager.persist( book );
		entityManager.getTransaction().commit();

		assertEquals(
				"Foo should have been added during indexing",
				1,
				search( "embeddableCategories.categories:Foo" ).size()
		);
		assertEquals( "Bar was not yet added", 0, search( "embeddableCategories.categories:Bar" ).size() );
	}

	@SuppressWarnings("unchecked")
	private List<Book> search(String searchQuery) throws ParseException {
		QueryParser parser = new MultiFieldQueryParser(
				new String[] { },
				new StandardAnalyzer()
		);
		FullTextQuery query = Search.getFullTextEntityManager( entityManager )
				.createFullTextQuery( parser.parse( searchQuery ), Book.class );
		return (List<Book>) query.getResultList();
	}
}


