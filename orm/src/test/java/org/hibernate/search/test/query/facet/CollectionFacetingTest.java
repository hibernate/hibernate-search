/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.query.facet;


import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Elmer van Chastelet
 */

public class CollectionFacetingTest extends SearchTestBase {
	Author voltaire;
	Author hugo;
	Author moliere;
	Author proust;

	@Before
	public void createTestData() {
		voltaire = new Author();
		voltaire.setName( "Voltaire" );

		hugo = new Author();
		hugo.setName( "Victor Hugo" );

		moliere = new Author();
		moliere.setName( "Moliere" );

		proust = new Author();
		proust.setName( "Proust" );

		Book book1 = new Book();
		book1.setName( "Candide" );
		book1.getAuthors().add( hugo );
		book1.getAuthors().add( voltaire );

		Book book2 = new Book();
		book2.setName( "Amphitryon" );
		book2.getAuthors().add( hugo );
		book2.getAuthors().add( moliere );

		Book book3 = new Book();
		book3.setName( "Hernani" );
		book3.getAuthors().add( hugo );
		book3.getAuthors().add( moliere );

		Session session = openSession();
		Transaction tx = session.beginTransaction();
		session.persist( voltaire );
		session.persist( hugo );
		session.persist( moliere );
		session.persist( proust );
		session.persist( book1 );
		session.persist( book2 );
		session.persist( book3 );

		tx.commit();
		session.close();
	}

	@Test
	public void testFacetEmbeddedAndCollections() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery(), Book.class );

		QueryBuilder builder = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity( Book.class ).get();
		FacetingRequest facetReq = builder.facet()
				.name( "someFacet" )
				.onField( "authors.name_untokenized" )
				.discrete()
				.orderedBy( FacetSortOrder.COUNT_DESC )
				.includeZeroCounts( false ).maxFacetCount( 10 )
				.createFacetingRequest();

		List<Facet> facets = fullTextQuery.getFacetManager().enableFaceting( facetReq ).getFacets( "someFacet" );
		assertEquals( "There should be three facets", 3, facets.size() );
		assertFacet( facets.get( 0 ), hugo, 3 );
		assertFacet( facets.get( 1 ), moliere, 2 );
		assertFacet( facets.get( 2 ), voltaire, 1 );

		fullTextSession.close();
	}

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Author.class, Book.class
		};
	}

	private void assertFacet(Facet facet, Author expectedAuthor, int expectedCount) {
		assertEquals( "Wrong facet value", expectedAuthor.getName(), facet.getValue() );
		assertEquals( "Wrong facet count", expectedCount, facet.getCount() );
	}
}
