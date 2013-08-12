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
package org.hibernate.search.test.shards;

import java.io.File;
import java.util.List;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.store.FSDirectory;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.test.util.RamIndexManager;

/**
 * @author Emmanuel Bernard
 */
public class ShardsTest extends SearchTestCase {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		File sub = getBaseIndexDir();
		cfg.setProperty( "hibernate.search.default.indexBase", sub.getAbsolutePath() );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		//is the default when multiple shards are set up
		//cfg.setProperty( "hibernate.search.Animal.sharding_strategy", IdHashShardingStrategy.class );
		cfg.setProperty( "hibernate.search.Animal.sharding_strategy.nbr_of_shards", "2" );
		cfg.setProperty( "hibernate.search.Animal.0.indexName", "Animal00" );
	}

	public void testIdShardingStrategy() {
		IndexManager[] dps = new IndexManager[] { RamIndexManager.makeRamDirectory(), RamIndexManager.makeRamDirectory() };
		IdHashShardingStrategy shardingStrategy = new IdHashShardingStrategy();
		shardingStrategy.initialize( null, dps );
		assertTrue( dps[1] == shardingStrategy.getIndexManagerForAddition( Animal.class, 1, "1", null ) );
		assertTrue( dps[0] == shardingStrategy.getIndexManagerForAddition( Animal.class, 2, "2", null ) );
		dps[0].destroy();
		dps[1].destroy();
	}

	public void testBehavior() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Animal a = new Animal();
		a.setId( 1 );
		a.setName( "Elephant" );
		s.persist( a );
		a = new Animal();
		a.setId( 2 );
		a.setName( "Bear" );
		s.persist( a );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		a = (Animal) s.get( Animal.class, 1 );
		a.setName( "Mouse" );
		Furniture fur = new Furniture();
		fur.setColor( "dark blue" );
		s.persist( fur );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		FullTextSession fts = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.stopAnalyzer );

		List results = fts.createFullTextQuery( parser.parse( "name:mouse OR name:bear" ) ).list();
		assertEquals( "Either double insert, single update, or query fails with shards", 2, results.size() );

		results = fts.createFullTextQuery( parser.parse( "name:mouse OR name:bear OR color:blue" ) ).list();
		assertEquals( "Mixing shared and non sharded properties fails", 3, results.size() );
		results = fts.createFullTextQuery( parser.parse( "name:mouse OR name:bear OR color:blue" ) ).list();
		assertEquals( "Mixing shared and non sharded properties fails with indexreader reuse", 3, results.size() );
		for ( Object o : results ) {
			s.delete( o );
		}
		tx.commit();
		s.close();
	}

	public void testInternalSharding() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Animal a = new Animal();
		a.setId( 1 );
		a.setName( "Elephant" );
		s.persist( a );
		a = new Animal();
		a.setId( 2 );
		a.setName( "Bear" );
		s.persist( a );
		tx.commit();

		s.clear();

		FSDirectory animal00Directory = FSDirectory.open( new File( getBaseIndexDir(), "Animal00" ) );
		try {
			IndexReader reader = IndexReader.open( animal00Directory, true );
			try {
				int num = reader.numDocs();
				assertEquals( 1, num );
			}
			finally {
				reader.close();
			}
		}
		finally {
			animal00Directory.close();
		}

		FSDirectory animal01Directory = FSDirectory.open( new File( getBaseIndexDir(), "Animal.1" ) );
		try {
			IndexReader reader = IndexReader.open( animal01Directory );
			try {
				int num = reader.numDocs();
				assertEquals( 1, num );
			}
			finally {
				reader.close();
			}
		}
		finally {
			animal01Directory.close();
		}

		tx = s.beginTransaction();
		a = (Animal) s.get( Animal.class, 1 );
		a.setName( "Mouse" );
		tx.commit();

		s.clear();

		animal01Directory = FSDirectory.open( new File( getBaseIndexDir(), "Animal.1" ) );
		try {
			IndexReader reader = IndexReader.open( animal01Directory );
			try {
				int num = reader.numDocs();
				assertEquals( 1, num );
				TermDocs docs = reader.termDocs( new Term( "name", "mouse" ) );
				assertTrue( docs.next() );
				org.apache.lucene.document.Document doc = reader.document( docs.doc() );
				assertFalse( docs.next() );
			}
			finally {
				reader.close();
			}
		}
		finally {
			animal01Directory.close();
		}

		tx = s.beginTransaction();
		FullTextSession fts = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.stopAnalyzer );

		List results = fts.createFullTextQuery( parser.parse( "name:mouse OR name:bear" ) ).list();
		assertEquals( "Either double insert, single update, or query fails with shards", 2, results.size() );
		for ( Object o : results ) {
			s.delete( o );
		}
		tx.commit();
		s.close();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Animal.class,
				Furniture.class
		};
	}
}
