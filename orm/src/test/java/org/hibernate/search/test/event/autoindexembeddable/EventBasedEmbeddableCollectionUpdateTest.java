/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.test.event.autoindexembeddable;

import java.util.List;
import javax.persistence.EntityManager;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.jpa.JPATestCase;
import org.hibernate.search.test.util.TestForIssue;
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
	public void setUp() {
		super.setUp();
		entityManager = factory.createEntityManager();
	}

	@After
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
				TestConstants.getTargetLuceneVersion(),
				new String[] { },
				new StandardAnalyzer( TestConstants.getTargetLuceneVersion() )
		);
		FullTextQuery query = Search.getFullTextEntityManager( entityManager )
				.createFullTextQuery( parser.parse( searchQuery ), Book.class );
		return (List<Book>) query.getResultList();
	}
}


