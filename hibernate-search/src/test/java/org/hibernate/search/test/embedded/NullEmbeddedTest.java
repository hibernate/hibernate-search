/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.search.test.embedded;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.embedded.nested.Attribute;
import org.hibernate.search.test.embedded.nested.AttributeValue;
import org.hibernate.search.test.embedded.nested.Product;

/**
 * @author Davide D'Alto
 */
public class NullEmbeddedTest extends SearchTestCase {

	public void testNullIndexing() throws Exception {
		State piemonte = new State();
		piemonte.setName( "Piemonte" );
		State lazio = new State();
		lazio.setName( "Lazio" );
		Country italy = new Country();
		italy.setName( "Italia" );
		italy.setStates( Arrays.asList( lazio, piemonte ) );

		Country france = new Country();
		france.setName( "France" );
		france.setStates( null );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( italy );
		s.persist( france );
		tx.commit();

		FullTextSession session = Search.getFullTextSession( s );
		QueryBuilder queryBuilder = session.getSearchFactory().buildQueryBuilder().forEntity( Country.class ).get();
		Query query = queryBuilder.keyword().onField( "states" ).ignoreAnalyzer().matching( null ).createQuery();
		@SuppressWarnings("unchecked")
		List<Country>result = session.createFullTextQuery( query ).list();
		assertEquals( "Wrong number of results found", 1, result.size() );
		assertEquals( "Wrong result returned", france, result.get( 0 ) );

		s.clear();

		tx = s.beginTransaction();
		s.delete( s.get( Country.class, italy.getId() ) );
		s.delete( s.get( Country.class, france.getId() ) );
		tx.commit();

		s.close();
	}

	public void testNullNestedEmbeddedIndexing() throws Exception {
		Product product = new Product();
		Attribute attribute = new Attribute( product, null );
		product.setAttribute( attribute );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( product );
		tx.commit();

		FullTextSession session = Search.getFullTextSession( s );
		QueryBuilder queryBuilder = session
				.getSearchFactory()
				.buildQueryBuilder()
				.forEntity( Product.class )
				.get();
		Query query = queryBuilder
				.keyword()
				.onField( "attributes.values" )
				.ignoreAnalyzer()
				.matching( null )
				.createQuery();
		@SuppressWarnings("unchecked")
		List<Product> result = session.createFullTextQuery( query ).list();
		assertEquals( "unable to find property in attribute value", 1, result.size() );
		assertEquals( "Wrong result retrieved", product, result.get( 0 ) );

		s.clear();
		s.close();
	}

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Tower.class, Address.class, Product.class, Order.class, Author.class, Country.class,
				State.class, StateCandidate.class, NonIndexedEntity.class, AbstractProduct.class, Book.class,
				ProductFeature.class, Attribute.class, AttributeValue.class };
	}
}
