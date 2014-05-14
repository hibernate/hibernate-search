/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.shards;

import org.apache.lucene.index.IndexReader;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test to retrieve specific IndexReader instances by index name.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DirectorySelectionTest extends SearchTestBase {
	private IndexReaderAccessor indexReaderAccessor;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		FullTextSession fts = indexData();
		indexReaderAccessor = fts.getSearchFactory().getIndexReaderAccessor();
	}

	@Test
	public void testDirectoryProviderForQuery() throws Exception {
		IndexReader indexReader = indexReaderAccessor.open( Product.class );
		try {
			Assert.assertEquals( 2, indexReader.numDocs() );
		}
		finally {
			indexReaderAccessor.close( indexReader );
		}

		indexReader = indexReaderAccessor.open( "Products.0" );
		try {
			Assert.assertEquals( 1, indexReader.numDocs() );
		}
		finally {
			indexReaderAccessor.close( indexReader );
		}

		indexReader = indexReaderAccessor.open( "Products.1" );
		try {
			Assert.assertEquals( 1, indexReader.numDocs() );
		}
		finally {
			indexReaderAccessor.close( indexReader );
		}
	}

	@Test
	public void testOpeningIndexReaderByUnknownNameThrowsException() throws Exception {
		try {
			indexReaderAccessor.open( "Products.1", "hoa?" );
			Assert.fail( "should have failed" );
		}
		catch (SearchException se) {
			Assert.assertEquals( "HSEARCH000107: Index names hoa? is not defined", se.getMessage() );
		}
	}

	@Test
	public void testOpeningIndexReaderUsingEmptyStringArrayThrowsException() throws Exception {
		try {
			indexReaderAccessor.open( new String[]{} );
			Assert.fail( "should have failed" );
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals( "HSEARCH000111: At least one index name must be provided: can't open an IndexReader on nothing", e.getMessage() );
		}
	}

	@Test
	public void testOpeningIndexReaderUsingNullAsNameThrowsException() throws Exception {
		try {
			indexReaderAccessor.open( (String) null );
			Assert.fail( "should have failed" );
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals( "HSEARCH000111: At least one index name must be provided: can't open an IndexReader on nothing", e.getMessage() );
		}
	}

	@Test
	public void testOpeningIndexReaderByUnknownEntityThrowsException() throws Exception {
		try {
			indexReaderAccessor.open( this.getClass() );
			Assert.fail( "should have failed" );
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals(
					"HSEARCH000109: org.hibernate.search.test.shards.DirectorySelectionTest is not an indexed type",
					e.getMessage()
			);
		}
	}

	@Test
	public void testOpeningIndexReaderUsingEmptyClassArrayThrowsException() throws Exception {
		try {
			indexReaderAccessor.open( new Class<?>[]{} );
			Assert.fail( "should have failed" );
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals( "HSEARCH000112: At least one entity type must be provided: can't open an IndexReader on nothing", e.getMessage() );
		}
	}

	@Test
	public void testOpeningIndexReaderUsingNullAsClassThrowsException() throws Exception {
		try {
			indexReaderAccessor.open( (Class<?>) null );
			Assert.fail( "should have failed" );
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals( "HSEARCH000110: 'null' is not a valid indexed type", e.getMessage() );
		}
	}

	private FullTextSession indexData() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Product p1 = new Product();
		p1.setName( "The Definitive ANTLR Reference: Building Domain-Specific Languages" );
		p1.setAvailable( true );
		s.persist( p1 );

		Product p2 = new Product();
		p2.setName( "Recipes for distributed cloud applications using Infinispan" );
		p2.setAvailable( false );
		s.persist( p2 );

		tx.commit();

		s.clear();

		FullTextSession fts = Search.getFullTextSession( s );
		fts.close();
		return fts;
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty(
				"hibernate.search.Products.sharding_strategy",
				ProductsAvailabilityShardingStrategy.class.getCanonicalName()
		);
		cfg.setProperty( "hibernate.search.Products.sharding_strategy.nbr_of_shards", "2" );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Product.class
		};
	}
}
