/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.shards;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.indexmanager.RamIndexManagerFactory;
import org.hibernate.search.testsupport.junit.ElasticsearchSupportInProgress;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Emmanuel Bernard
 */
public class ShardsTest extends SearchTestBase {

	@Rule
	public RamIndexManagerFactory managerFactory = new RamIndexManagerFactory();

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.directory_provider", "filesystem" );
		Path sub = getBaseIndexDir();
		cfg.put( "hibernate.search.default.indexBase", sub.toAbsolutePath().toString() );
		cfg.put( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
		//is the default when multiple shards are set up
		//cfg.setProperty( "hibernate.search.Animal.sharding_strategy", IdHashShardingStrategy.class );
		cfg.put( "hibernate.search.Animal.sharding_strategy.nbr_of_shards", "2" );
		cfg.put( "hibernate.search.Animal.0.indexName", "Animal00" );
	}

	@Test
	public void testIdShardingStrategy() {
		IndexManager[] dps = managerFactory.createArray( 2 );
		IdHashShardingStrategy shardingStrategy = new IdHashShardingStrategy();
		shardingStrategy.initialize( null, dps );
		assertTrue( dps[1] == shardingStrategy.getIndexManagerForAddition( Animal.class, 1, "1", null ) );
		assertTrue( dps[0] == shardingStrategy.getIndexManagerForAddition( Animal.class, 2, "2", null ) );
	}

	@Test
	// there is a mismatch of the index name as handled by IndexManagerHolder and the ES-IM: Animal.0 vs. Animal00
	@Category(ElasticsearchSupportInProgress.class)
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
		a = s.get( Animal.class, 1 );
		a.setName( "Mouse" );
		Furniture fur = new Furniture();
		fur.setColor( "dark blue" );
		s.persist( fur );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		FullTextSession fts = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( "id", TestConstants.stopAnalyzer );

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
	// there is a mismatch of the index name as handled by IndexManagerHolder and the ES-IM: Animal.0 vs. Animal00
	@Category(ElasticsearchSupportInProgress.class)
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

		assertEquals( 1, getNumberOfDocumentsInIndex( "Animal00" ) );
		assertEquals( 1, getNumberOfDocumentsInIndex( "Animal.1" ) );

		tx = s.beginTransaction();
		a = s.get( Animal.class, 1 );
		a.setName( "Mouse" );
		tx.commit();

		s.clear();

		assertEquals( 1, getNumberOfDocumentsInIndex( "Animal.1" ) );
		assertEquals( 1, getNumberOfDocumentsInIndexByQuery( "Animal.1", "name", "mouse" ) );

		tx = s.beginTransaction();
		FullTextSession fts = Search.getFullTextSession( s );
		QueryParser parser = new QueryParser( "id", TestConstants.stopAnalyzer );

		List<?> results = fts.createFullTextQuery( parser.parse( "name:mouse OR name:bear" ) ).list();
		assertEquals( "Either double insert, single update, or query fails with shards", 2, results.size() );
		for ( Object o : results ) {
			s.delete( o );
		}
		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Animal.class,
				Furniture.class
		};
	}
}
