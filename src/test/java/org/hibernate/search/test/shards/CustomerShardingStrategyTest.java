package org.hibernate.search.test.shards;

import junit.framework.TestCase;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import org.hibernate.search.query.FullTextFilterImpl;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.RAMDirectoryProvider;

/**
 * @author Chase Seibert
 */
public class CustomerShardingStrategyTest extends TestCase {

	private CustomerShardingStrategy shardStrategy;

	protected void setUp() throws Exception {
		shardStrategy = new CustomerShardingStrategy();
		
		// initilaize w/ 10 shards
		shardStrategy.initialize( null, new DirectoryProvider[] {
				new RAMDirectoryProvider(), 
				new RAMDirectoryProvider(),
				new RAMDirectoryProvider(),
				new RAMDirectoryProvider(),
				new RAMDirectoryProvider(),
				new RAMDirectoryProvider(),
				new RAMDirectoryProvider(),
				new RAMDirectoryProvider(),
				new RAMDirectoryProvider(),
				new RAMDirectoryProvider() 
		} );
	}

	public void testGetDirectoryProvidersForQuery() {
		
		FullTextFilterImpl filter = new FullTextFilterImpl();
		filter.setName("customer");
		filter.setParameter("customerID", 5);
		
		// customerID == 5 should correspond to just a single shard instance
		DirectoryProvider[] providers = shardStrategy.getDirectoryProvidersForQuery(new FullTextFilterImpl[] { filter });
		assertTrue(providers.length == 1);
		
		// create a dummy document for the same customerID, and make sure the shard it would be
		// indexed on matches the shard returned by getDirectoryProvidersForQuery()
		Document document = new Document();
		document.add(new Field("customerID", "5", Field.Store.NO, Field.Index.UN_TOKENIZED));
		
		assertTrue(providers[0].equals(
			shardStrategy.getDirectoryProviderForAddition(null, null, null, document)
			));
	}
	
}