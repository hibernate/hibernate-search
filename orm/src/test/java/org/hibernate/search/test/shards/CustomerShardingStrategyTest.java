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

import static org.junit.Assert.assertTrue;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.hibernate.search.filter.impl.FullTextFilterImpl;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.util.RamIndexManager;
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
