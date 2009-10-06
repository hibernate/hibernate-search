/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
