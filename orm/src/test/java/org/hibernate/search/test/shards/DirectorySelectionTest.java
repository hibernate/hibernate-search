/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
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
package org.hibernate.search.test.shards;

import junit.framework.Assert;
import org.apache.lucene.index.IndexReader;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.test.SearchTestCase;

/**
 * Test to retrieve specific IndexReader instances by index name.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DirectorySelectionTest extends SearchTestCase {
	private IndexReaderAccessor indexReaderAccessor;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		FullTextSession fts = indexData();
		indexReaderAccessor = fts.getSearchFactory().getIndexReaderAccessor();
	}

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

	public void testOpeningIndexReaderByUnknownNameThrowsException() throws Exception {
		try {
			indexReaderAccessor.open( "Products.1", "hoa?" );
			Assert.fail( "should have failed" );
		}
		catch (SearchException se) {
			Assert.assertEquals( "HSEARCH000107: Index names hoa? is not defined", se.getMessage() );
		}
	}

	public void testOpeningIndexReaderUsingEmptyStringArrayThrowsException() throws Exception {
		try {
			indexReaderAccessor.open( new String[]{} );
			Assert.fail( "should have failed" );
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals( "HSEARCH000111: At least one index name must be provided: can't open an IndexReader on nothing", e.getMessage() );
		}
	}

	public void testOpeningIndexReaderUsingNullAsNameThrowsException() throws Exception {
		try {
			indexReaderAccessor.open( (String) null );
			Assert.fail( "should have failed" );
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals( "HSEARCH000111: At least one index name must be provided: can't open an IndexReader on nothing", e.getMessage() );
		}
	}

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

	public void testOpeningIndexReaderUsingEmptyClassArrayThrowsException() throws Exception {
		try {
			indexReaderAccessor.open( new Class<?>[]{} );
			Assert.fail( "should have failed" );
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals( "HSEARCH000112: At least one entity type must be provided: can't open an IndexReader on nothing", e.getMessage() );
		}
	}

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
