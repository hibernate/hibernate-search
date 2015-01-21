/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.query.fieldcache;

import java.math.BigDecimal;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.engine.Country;
import org.hibernate.search.test.engine.Location;
import org.hibernate.search.test.engine.PinPoint;
import org.hibernate.search.testsupport.readerprovider.FieldSelectorLeakingReaderProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class CachedNumericIdTest extends SearchTestBase {

	private static final int NUM_LOCATIONS = 50;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		prepareData();
	}

	@Test
	public void testLocationLoading() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		QueryBuilder queryBuilder = getSearchFactory().buildQueryBuilder().forEntity( Location.class ).get();
		Query query = queryBuilder.all().createQuery();
		FieldSelectorLeakingReaderProvider.resetFieldSelector();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( query, Location.class );
		fullTextQuery.setSort( new Sort( new SortField( "description", SortField.Type.STRING ) ) ); // to avoid loading in document order -- too easy
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
					.valueOf( 100 - i ), String.valueOf( i ) + "42", null, italy, BigDecimal.ONE, (short) 5, (byte) 10 ) );
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
	@After
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
		cfg.setProperty( "hibernate.search.default." + Environment.READER_STRATEGY, FieldSelectorLeakingReaderProvider.class.getName() );
	}

}
