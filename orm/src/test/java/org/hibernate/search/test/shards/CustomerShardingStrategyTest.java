/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.shards;

import static org.junit.Assert.assertTrue;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.filter.impl.FullTextFilterImpl;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.testsupport.indexmanager.RamIndexManager;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Chase Seibert
 */
public class CustomerShardingStrategyTest {

	private CustomerShardingStrategy shardStrategy;

	@Before
	public void setUp() throws Exception {
		shardStrategy = new CustomerShardingStrategy();

		// initialize w/ 10 shards
		shardStrategy.initialize( null, new IndexManager[] {
				RamIndexManager.makeRamDirectory(),
				RamIndexManager.makeRamDirectory(),
				RamIndexManager.makeRamDirectory(),
				RamIndexManager.makeRamDirectory(),
				RamIndexManager.makeRamDirectory(),
				RamIndexManager.makeRamDirectory(),
				RamIndexManager.makeRamDirectory(),
				RamIndexManager.makeRamDirectory(),
				RamIndexManager.makeRamDirectory(),
				RamIndexManager.makeRamDirectory()
		} );
	}

	@Test
	public void testGetDirectoryProvidersForQuery() {

		FullTextFilterImpl filter = new FullTextFilterImpl();
		filter.setName( "customer" );
		filter.setParameter( "customerID", 5 );

		// customerID == 5 should correspond to just a single shard instance
		IndexManager[] providers = shardStrategy.getIndexManagersForQuery( new FullTextFilterImpl[] { filter } );
		assertTrue( providers.length == 1 );

		// create a dummy document for the same customerID, and make sure the shard it would be
		// indexed on matches the shard returned by getDirectoryProvidersForQuery()
		Document document = new Document();
		document.add( new Field( "customerID", "5", Field.Store.NO, Field.Index.NOT_ANALYZED ) );

		assertTrue( providers[0].equals(
				shardStrategy.getIndexManagerForAddition( null, null, null, document )
			));
	}

}
