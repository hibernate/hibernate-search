// $Id$
package org.hibernate.search.test.shards;

import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IdHashShardingStrategy;
import org.hibernate.search.store.RAMDirectoryProvider;

import junit.framework.TestCase;

/**
 * @author Sanne Grinovero
 */
public class IdShardingStrategyTest extends TestCase {

	private IdHashShardingStrategy shardStrategy;

	protected void setUp() throws Exception {
		shardStrategy = new IdHashShardingStrategy();
		shardStrategy.initialize( null, new DirectoryProvider[] {
				new RAMDirectoryProvider(), new RAMDirectoryProvider() } );
	}

	public void testHashOverflow() {
		String key = String.valueOf( Integer.MAX_VALUE - 1 );
		// any key will do as long as it's hash is negative
		assertTrue( key.hashCode() < 0 );
		assertAcceptableId( key );
	}

	private void assertAcceptableId(String id) {
		try {
			shardStrategy.getDirectoryProviderForAddition( null, id, id, null );
			shardStrategy.getDirectoryProvidersForDeletion( null, id, id );
		}
		catch ( Exception e ) {
			fail( "Couldn't get directory for id " + id );
		}
	}

}