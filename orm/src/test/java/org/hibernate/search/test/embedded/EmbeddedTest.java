/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.embedded;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class EmbeddedTest extends SearchTestCase {

	public void testEmbeddedIndexing() throws Exception {
		Tower tower = new Tower();
		tower.setName( "JBoss tower" );
		Address a = new Address();
		a.setStreet( "Tower place" );
		a.getTowers().add( tower );
		tower.setAddress( a );
		Person o = new Owner();
		o.setName( "Atlanta Renting corp" );
		a.setOwnedBy( o );
		o.setAddress( a );
		Country c = new Country();
		c.setName( "France" );
		a.setCountry( c );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( tower );
		tx.commit();

		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;

		query = parser.parse( "address.street:place" );
		result = session.createFullTextQuery( query ).list();
		assertEquals( "unable to find property in embedded", 1, result.size() );

		query = parser.parse( "address.ownedBy_name:renting" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property in embedded", 1, result.size() );

		query = parser.parse( "address.id:" + a.getId().toString() );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property by id of embedded", 1, result.size() );

		query = parser.parse( "address.country.name:" + a.getCountry().getName() );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property with 2 levels of embedded", 1, result.size() );

		s.clear();

		tx = s.beginTransaction();
		Address address = (Address) s.get( Address.class, a.getId() );
		address.getOwnedBy().setName( "Buckhead community" );
		tx.commit();

		s.clear();

		session = Search.getFullTextSession( s );

		query = parser.parse( "address.ownedBy_name:buckhead" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "change in embedded not reflected in root index", 1, result.size() );

		s.clear();

		tx = s.beginTransaction();
		s.delete( s.get( Tower.class, tower.getId() ) );
		tx.commit();

		s.close();

	}

	public void testEmbeddedIndexingOneToMany() throws Exception {
		Country country = new Country();
		country.setName( "Germany" );
		List<State> states = new ArrayList<State>();
		State bayern = new State();
		bayern.setName( "Bayern" );
		State hessen = new State();
		hessen.setName( "Hessen" );
		State sachsen = new State();
		sachsen.setName( "Sachsen" );
		states.add( bayern );
		states.add( hessen );
		states.add( sachsen );
		country.setStates( states );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( country );
		tx.commit();

		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;

		query = parser.parse( "states.name:Hessen" );
		result = session.createFullTextQuery( query ).list();
		assertEquals( "unable to find property in embedded", 1, result.size() );
		s.close();
	}

	public void testContainedIn() throws Exception {
		Tower tower = new Tower();
		tower.setName( "JBoss tower" );
		Address a = new Address();
		a.setStreet( "Tower place" );
		a.getTowers().add( tower );
		tower.setAddress( a );
		Person o = new Owner();
		o.setName( "Atlanta Renting corp" );
		a.setOwnedBy( o );
		o.setAddress( a );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( tower );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		Address address = (Address) s.get( Address.class, a.getId() );
		address.setStreet( "Peachtree Road NE" );
		tx.commit();

		s.clear();

		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;

		query = parser.parse( "address.street:peachtree" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "change in embedded not reflected in root index", 1, result.size() );

		s.clear();

		tx = s.beginTransaction();
		address = (Address) s.get( Address.class, a.getId() );
		Tower tower1 = address.getTowers().iterator().next();
		tower1.setAddress( null );
		address.getTowers().remove( tower1 );
		tx.commit();

		s.clear();

		session = Search.getFullTextSession( s );

		query = parser.parse( "address.street:peachtree" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "breaking link fails", 0, result.size() );

		tx = s.beginTransaction();
		s.delete( s.get( Tower.class, tower.getId() ) );
		tx.commit();

		s.close();

	}

	public void testIndexedEmbeddedAndCollections() throws Exception {
		Author a = new Author();
		a.setName( "Voltaire" );
		Author a2 = new Author();
		a2.setName( "Victor Hugo" );
		Author a3 = new Author();
		a3.setName( "Moliere" );
		Author a4 = new Author();
		a4.setName( "Proust" );

		Order o = new Order();
		o.setOrderNumber( "ACVBNM" );

		Order o2 = new Order();
		o2.setOrderNumber( "ZERTYD" );

		Product p1 = new Product();
		p1.setName( "Candide" );
		p1.getAuthors().add( a );
		p1.getAuthors().add( a2 ); // be creative

		Product p2 = new Product();
		p2.setName( "Le malade imaginaire" );
		p2.getAuthors().add( a3 );
		p2.getOrders().put( "Emmanuel", o );
		p2.getOrders().put( "Gavin", o2 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( a );
		s.persist( a2 );
		s.persist( a3 );
		s.persist( a4 );
		s.persist( o );
		s.persist( o2 );
		s.persist( p1 );
		s.persist( p2 );
		tx.commit();

		s.clear();

		FullTextSession session = Search.getFullTextSession( s );
		tx = session.beginTransaction();

		QueryParser parser = new MultiFieldQueryParser(
				TestConstants.getTargetLuceneVersion(),
				new String[] { "name", "authors.name" },
				TestConstants.standardAnalyzer
		);
		Query query;
		List<?> result;

		query = parser.parse( "Hugo" );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertEquals( "collection of embedded ignored", 1, result.size() );

		// update the collection
		Product p = (Product) result.get( 0 );
		p.getAuthors().add( a4 );

		// PhraseQuery
		query = new TermQuery( new Term( "orders.orderNumber", "ZERTYD" ) );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertEquals( "collection of untokenized ignored", 1, result.size() );
		query = new TermQuery( new Term( "orders.orderNumber", "ACVBNM" ) );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertEquals( "collection of untokenized ignored", 1, result.size() );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		session = Search.getFullTextSession( s );
		query = parser.parse( "Proust" );
		result = session.createFullTextQuery( query, Product.class ).list();
		// HSEARCH-56
		assertEquals( "update of collection of embedded ignored", 1, result.size() );

		s.delete( s.get( Product.class, p1.getId() ) );
		s.delete( s.get( Product.class, p2.getId() ) );
		tx.commit();
		s.close();
	}

	/**
	 * Tests that updating an indexed embedded object updates the Lucene index as well.
	 *
	 * @throws Exception in case the test fails
	 */
	public void testEmbeddedObjectUpdate() throws Exception {

		State state = new State();
		state.setName( "Bavaria" );
		StateCandidate candiate = new StateCandidate();
		candiate.setName( "Mueller" );
		candiate.setState( state );
		state.setCandidate( candiate );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( candiate );
		tx.commit();
		s.clear();

		FullTextSession session = Search.getFullTextSession( s );
		tx = session.beginTransaction();

		QueryParser parser = new MultiFieldQueryParser(
				TestConstants.getTargetLuceneVersion(),
				new String[] { "name", "state.name" },
				TestConstants.standardAnalyzer
		);
		Query query;
		List<?> result;

		query = parser.parse( "Bavaria" );
		result = session.createFullTextQuery( query, StateCandidate.class ).list();
		assertEquals( "IndexEmbedded ignored.", 1, result.size() );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		state.setName( "Hessen" );
		s.merge( state );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		session = Search.getFullTextSession( s );
		query = parser.parse( "Hessen" );
		result = session.createFullTextQuery( query, StateCandidate.class ).list();
		assertEquals( "IndexEmbedded ignored.", 1, result.size() );
		tx.commit();
		s.clear();
		s.close();
	}

	public void testEmbeddedToManyInSuperclass() throws ParseException {
		ProductFeature featureA = new ProductFeature();
		featureA.setName( "featureA" );
		ProductFeature featureB = new ProductFeature();
		featureB.setName( "featureB" );

		AbstractProduct book = new Book();
		book.setName( "A Book" );
		featureA.setProduct( book );
		book.getFeatures().add( featureA );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( book );
		tx.commit();

		s.clear();

		FullTextSession session = Search.getFullTextSession( s );
		tx = session.beginTransaction();

		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "name", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;

		query = parser.parse( "features.name:featureA" );
		result = session.createFullTextQuery( query, AbstractProduct.class ).list();
		assertEquals( "Feature A should be indexed", 1, result.size() );

		// Add product features - product should be re-indexed
		book = (AbstractProduct) result.get( 0 );
		book.getFeatures().add( featureB );

		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		tx.commit();
		s.clear();

		query = parser.parse( "features.name:featureB" );
		result = session.createFullTextQuery( query, AbstractProduct.class ).list();
		assertEquals( "Feature B should be indexed now as well", 1, result.size() );

		s.close();
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Tower.class, Address.class, Product.class, Order.class, Author.class, Country.class,
				State.class, StateCandidate.class, NonIndexedEntity.class,
				AbstractProduct.class, Book.class, ProductFeature.class
		};
	}
}
