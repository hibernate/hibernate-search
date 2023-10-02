/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.embedded.nested;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;

import org.junit.jupiter.api.Test;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
class NestedEmbeddedTest extends SearchTestBase {

	/**
	 * HSEARCH-391
	 *
	 * @throws Exception in case the tests fails
	 */
	@Test
	void testNestedEmbeddedIndexing() throws Exception {
		Product product = new Product();
		Attribute attribute = new Attribute( product );
		product.setAttribute( attribute );
		AttributeValue value = new AttributeValue( attribute, "foo" );
		attribute.setValue( value );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( product );
		tx.commit();

		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( "attributes.values.value", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;


		query = parser.parse( "foo" );
		result = session.createFullTextQuery( query ).list();
		assertThat( result ).as( "unable to find property in attribute value" ).hasSize( 1 );


		s.clear();
		tx = s.beginTransaction();

		product = (Product) s.get( Product.class, product.getId() );
		product.getAttributes().get( 0 ).getValues().get( 0 ).setValue( "bar" );
		tx.commit();

		s.clear();

		session = Search.getFullTextSession( s );
		tx = s.beginTransaction();

		query = parser.parse( "foo" );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertThat( result ).as( "change in embedded not reflected in root index" ).isEmpty();

		query = parser.parse( "bar" );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertThat( result ).as( "change in embedded not reflected in root index" ).hasSize( 1 );

		tx.commit();
		s.close();
	}


	/**
	 * HSEARCH-391
	 *
	 * @throws Exception in case the tests fails
	 */
	@Test
	void testNestedEmbeddedIndexingWithContainedInOnCollection() throws Exception {
		Person john = new Person( "John Doe" );
		Place eiffelTower = new Place( "Eiffel Tower" );
		Address addressEiffel = new Address( "Avenue Gustave Eiffel", "London" );
		addressEiffel.addPlace( eiffelTower );
		eiffelTower.setAddress( addressEiffel );
		john.addPlaceVisited( eiffelTower );
		eiffelTower.visitedBy( john );


		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( john );
		tx.commit();

		FullTextSession session = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( "placesVisited.address.city", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;


		query = parser.parse( "London" );
		result = session.createFullTextQuery( query ).list();
		assertThat( result ).as( "unable to find nested indexed value" ).hasSize( 1 );


		s.clear();
		tx = s.beginTransaction();

		john = (Person) s.get( Person.class, john.getId() );
		john.getPlacesVisited().get( 0 ).getAddress().setCity( "Paris" );
		tx.commit();

		s.clear();

		john = (Person) s.get( Person.class, john.getId() );

		session = Search.getFullTextSession( s );
		tx = s.beginTransaction();

		query = parser.parse( "London" );
		result = session.createFullTextQuery( query, Person.class ).list();
		assertThat( result ).as( "change in embedded not reflected in root index" ).isEmpty();

		query = parser.parse( "Paris" );
		result = session.createFullTextQuery( query, Person.class ).list();
		assertThat( result ).as( "change in embedded not reflected in root index" ).hasSize( 1 );

		tx.commit();
		session.close();
		//s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Product.class,
				Attribute.class,
				AttributeValue.class,
				Person.class,
				Place.class,
				Address.class
		};
	}
}
