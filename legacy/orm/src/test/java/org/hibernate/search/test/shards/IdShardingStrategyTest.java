/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.shards;

import static org.junit.Assert.assertTrue;

import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.testsupport.indexmanager.RamIndexManagerFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Sanne Grinovero
 */
public class IdShardingStrategyTest {

	@Rule
	public RamIndexManagerFactory managerFactory = new RamIndexManagerFactory();

	private IdHashShardingStrategy shardStrategy;

	@Before
	public void setUp() throws Exception {
		shardStrategy = new IdHashShardingStrategy();
		shardStrategy.initialize( null, managerFactory.createArray( 2 ) );
	}

	@Test
	public void testHashOverflow() {
		String key = String.valueOf( Integer.MAX_VALUE - 1 );
		// any key will do as long as it's hash is negative
		assertTrue( key.hashCode() < 0 );
		assertAcceptableId( key );
	}

	private void assertAcceptableId(String id) {
		shardStrategy.getIndexManagerForAddition( null, id, id, null );
		shardStrategy.getIndexManagersForDeletion( null, id, id );
	}

}
