/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;

import org.junit.jupiter.api.Test;

import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
class EmbeddedTest extends SearchTestBase {

	@Test
	void testEmbeddedIndexing() throws Exception {
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
		QueryParser parser = new QueryParser( "noDefaultField", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;

		query = parser.parse( "address.street:place" );
		result = session.createFullTextQuery( query ).list();
		assertThat( result ).as( "unable to find property in embedded" ).hasSize( 1 );

		query = parser.parse( "address.ownedBy_name:renting" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertThat( result ).as( "unable to find property in embedded" ).hasSize( 1 );

		query = LongPoint.newExactQuery( "address.id", a.getId() );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertThat( result ).as( "unable to find property by id of embedded" ).hasSize( 1 );

		query = parser.parse( "address.country.name:" + a.getCountry().getName() );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertThat( result ).as( "unable to find property with 2 levels of embeddedembedded" ).hasSize( 1 );

		s.clear();

		tx = s.beginTransaction();
		Address address = s.get( Address.class, a.getId() );
		address.getOwnedBy().setName( "Buckhead community" );
		tx.commit();

		s.clear();

		session = Search.getFullTextSession( s );

		query = parser.parse( "address.ownedBy_name:buckhead" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertThat( result ).as( "change in embedded not reflected in root index" ).hasSize( 1 );

		s.clear();

		tx = s.beginTransaction();
		s.delete( s.get( Tower.class, tower.getId() ) );
		tx.commit();

		s.close();

	}

	@Test
	void testEmbeddedIndexingOneToMany() throws Exception {
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
		QueryParser parser = new QueryParser( "noDefaultField", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;

		query = parser.parse( "states.name:Hessen" );
		result = session.createFullTextQuery( query ).list();
		assertThat( result ).as( "unable to find property in embedded" ).hasSize( 1 );
		s.close();
	}

	@Test
	void testEmbeddedIndexingElementCollection() throws Exception {
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

		Resident r1 = new Resident();
		r1.setName( "John Doe" );
		a.getResidents().add( r1 );

		Resident r2 = new Resident();
		r2.setName( "Jane Smith" );
		a.getResidents().add( r2 );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( tower );
		tx.commit();

		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( "noDefaultField", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;

		query = parser.parse( "address.inhabitants.name:Smith" );
		result = session.createFullTextQuery( query ).list();
		assertThat( result ).as( "unable to find property in embedded @ElementCollection" ).hasSize( 1 );
		s.close();
	}

	@Test
	void testContainedIn() throws Exception {
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
		Address address = s.get( Address.class, a.getId() );
		address.setStreet( "Peachtree Road NE" );
		tx.commit();

		s.clear();

		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( "noDefaultField", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;

		query = parser.parse( "address.street:peachtree" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertThat( result ).as( "change in embedded not reflected in root index" ).hasSize( 1 );

		s.clear();

		tx = s.beginTransaction();
		address = s.get( Address.class, a.getId() );
		Tower tower1 = address.getTowers().iterator().next();
		tower1.setAddress( null );
		address.getTowers().remove( tower1 );
		tx.commit();

		s.clear();

		session = Search.getFullTextSession( s );

		query = parser.parse( "address.street:peachtree" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertThat( result ).as( "breaking link fails" ).isEmpty();

		tx = s.beginTransaction();
		s.delete( s.get( Tower.class, tower.getId() ) );
		tx.commit();

		s.close();

	}

	@Test
	void testIndexedEmbeddedAndCollections() throws Exception {
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
				new String[] { "name", "authors.name" },
				TestConstants.standardAnalyzer
		);
		Query query;
		List<?> result;

		query = parser.parse( "Hugo" );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertThat( result ).as( "collection of embedded ignored" ).hasSize( 1 );

		// update the collection
		Product p = (Product) result.get( 0 );
		p.getAuthors().add( a4 );

		// PhraseQuery
		query = new TermQuery( new Term( "orders.orderNumber", "ZERTYD" ) );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertThat( result ).as( "collection of untokenized ignored" ).hasSize( 1 );
		query = new TermQuery( new Term( "orders.orderNumber", "ACVBNM" ) );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertThat( result ).as( "collection of untokenized ignored" ).hasSize( 1 );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		session = Search.getFullTextSession( s );
		query = parser.parse( "Proust" );
		result = session.createFullTextQuery( query, Product.class ).list();
		// HSEARCH-56
		assertThat( result ).as( "update of collection of embedded ignored" ).hasSize( 1 );

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
	@Test
	void testEmbeddedObjectUpdate() throws Exception {
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
				new String[] { "name", "state.name" },
				TestConstants.standardAnalyzer
		);
		Query query;
		List<?> result;

		query = parser.parse( "Bavaria" );
		result = session.createFullTextQuery( query, StateCandidate.class ).list();
		assertThat( result ).as( "IndexEmbedded ignored." ).hasSize( 1 );
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
		assertThat( result ).as( "IndexEmbedded ignored." ).hasSize( 1 );
		tx.commit();
		s.clear();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Tower.class,
				Address.class,
				Product.class,
				Order.class,
				Author.class,
				Country.class,
				State.class,
				StateCandidate.class,
				NonIndexedEntity.class
		};
	}
}
