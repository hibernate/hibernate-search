/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.test.query.fieldcache;

import java.math.BigDecimal;
import java.util.List;

import junit.framework.Assert;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.engine.Location;
import org.hibernate.search.test.engine.PinPoint;
import org.hibernate.search.test.engine.Country;
import org.hibernate.search.test.util.FieldSelectorLeakingReaderProvider;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class CachedNumericIdTest extends SearchTestCase {

	private static final int NUM_LOCATIONS = 50;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		prepareData();
	}

	public void testLocationLoading() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		QueryBuilder queryBuilder = getSearchFactory().buildQueryBuilder().forEntity( Location.class ).get();
		Query query = queryBuilder.all().createQuery();
		FieldSelectorLeakingReaderProvider.resetFieldSelector();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Location.class );
		fullTextQuery.setSort( new Sort( new SortField( "description", SortField.STRING ) ) ); // to avoid loading in document order -- too easy
		List<Location> locations = fullTextQuery.list();
		FieldSelectorLeakingReaderProvider.assertFieldSelectorDisabled();
		Assert.assertEquals( NUM_LOCATIONS, locations.size() );
		for ( Location location : locations ) {
			int id = location.getId();
			Assert.assertEquals( String.valueOf( id ) + "42", location.getDescription() );
		}
		tx.commit();
		session.close();
	}

	private void prepareData() {
		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction transaction = fullTextSession.beginTransaction();
		Country italy = new Country( "Italy", 39d );
		for ( int i = 0; i < NUM_LOCATIONS; i++ ) {
			session.persist( new Location( i, Long.valueOf( i ), 7 * i, Double.valueOf( 9 * i ), Integer
					.valueOf( 100 - i ), String.valueOf( i ) + "42", null, italy, BigDecimal.ONE ) );
		}
		transaction.commit();
		session.close();
	}

	private void cleanData() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		session.createQuery( "delete Location" ).executeUpdate();
		tx.commit();
		session.close();
	}

	@Override
	public void tearDown() throws Exception {
		cleanData();
		super.tearDown();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PinPoint.class, Location.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		// force multiple segments to also verify the docId transformations
		cfg.setProperty( "hibernate.search.default.indexwriter.transaction.max_merge_docs" , "10" );
		cfg.setProperty( "hibernate.search.default." + Environment.READER_STRATEGY, org.hibernate.search.test.util.FieldSelectorLeakingReaderProvider.class.getName() );
	}

}
