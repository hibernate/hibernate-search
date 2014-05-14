/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.shards;

import java.io.File;
import java.util.List;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.FSDirectory;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.indexmanager.RamIndexManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class ShardsTest extends SearchTestBase {

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

	@Test
	public void testIdShardingStrategy() {
		IndexManager[] dps = new IndexManager[] { RamIndexManager.makeRamDirectory(), RamIndexManager.makeRamDirectory() };
		IdHashShardingStrategy shardingStrategy = new IdHashShardingStrategy();
		shardingStrategy.initialize( null, dps );
		assertTrue( dps[1] == shardingStrategy.getIndexManagerForAddition( Animal.class, 1, "1", null ) );
		assertTrue( dps[0] == shardingStrategy.getIndexManagerForAddition( Animal.class, 2, "2", null ) );
		dps[0].destroy();
		dps[1].destroy();
	}

	@Test
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

	@Test
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
			IndexReader reader = DirectoryReader.open( animal00Directory );
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
			IndexReader reader = DirectoryReader.open( animal01Directory );
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
			IndexReader reader = DirectoryReader.open( animal01Directory );
			try {
				int num = reader.numDocs();
				assertEquals( 1, num );
				int numberOfMice = reader.docFreq( new Term( "name", "mouse" ) );
				assertEquals( 1, numberOfMice );
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
