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
import org.hibernate.search.indexes.ReaderAccessor;
import org.hibernate.search.test.SearchTestCase;

/**
 * Test to retrieve specific IndexReader instances by index name.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DirectorySelectionTest extends SearchTestCase {

	public void testDirectoryProviderForQuery() throws Exception {

		Session s = openSession( );
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
		ReaderAccessor indexReaders = fts.getSearchFactory().getIndexReaders();
		fts.close();

		IndexReader indexReader = indexReaders.openIndexReader( Product.class );
		try {
			Assert.assertEquals( 2, indexReader.numDocs() );
		}
		finally {
			indexReaders.closeIndexReader( indexReader );
		}

		indexReader = indexReaders.openIndexReader( "Products.0" );
		try {
			Assert.assertEquals( 1, indexReader.numDocs() );
		}
		finally {
			indexReaders.closeIndexReader( indexReader );
		}

		indexReader = indexReaders.openIndexReader( "Products.1" );
		try {
			Assert.assertEquals( 1, indexReader.numDocs() );
		}
		finally {
			indexReaders.closeIndexReader( indexReader );
		}

		try {
			indexReader = indexReaders.openIndexReader( "Products.1", "hoa?" );
			Assert.fail( "should have failed" );
		}
		catch (SearchException se) {
			Assert.assertEquals( "HSEARCH000107: Index names hoa? is not defined", se.getMessage() );
		}
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.Products.sharding_strategy", ProductsAvailabilityShardingStrategy.class.getCanonicalName() );
		cfg.setProperty( "hibernate.search.Products.sharding_strategy.nbr_of_shards", "2" );
	}

	@SuppressWarnings("unchecked")
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Product.class
		};
	}

}
