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
package org.hibernate.search.test.embedded.nested;

import java.util.List;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
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
public class NestedEmbeddedTest extends SearchTestCase {

	/**
	 * HSEARCH-391
	 *
	 * @throws Exception in case the tests fails
	 */
	public void testNestedEmbeddedIndexing() throws Exception {
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
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "attributes.values.value", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;


		query = parser.parse( "foo" );
		result = session.createFullTextQuery( query ).list();
		assertEquals( "unable to find property in attribute value", 1, result.size() );


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
		assertEquals( "change in embedded not reflected in root index", 0, result.size() );

		query = parser.parse( "bar" );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertEquals( "change in embedded not reflected in root index", 1, result.size() );

		tx.commit();
		s.close();
	}


	/**
	 * HSEARCH-391
	 *
	 * @throws Exception in case the tests fails
	 */
	public void testNestedEmbeddedIndexingWithContainedInOnCollection() throws Exception {
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
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "placesVisited.address.city", TestConstants.standardAnalyzer );
		Query query;
		List<?> result;


		query = parser.parse( "London" );
		result = session.createFullTextQuery( query ).list();
		assertEquals( "unable to find nested indexed value", 1, result.size() );


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
		assertEquals( "change in embedded not reflected in root index", 0, result.size() );

		query = parser.parse( "Paris" );
		result = session.createFullTextQuery( query, Person.class ).list();
		assertEquals( "change in embedded not reflected in root index", 1, result.size() );

		tx.commit();
		session.close();
		//s.close();
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Product.class, Attribute.class, AttributeValue.class, Person.class, Place.class, Address.class
		};
	}
}
