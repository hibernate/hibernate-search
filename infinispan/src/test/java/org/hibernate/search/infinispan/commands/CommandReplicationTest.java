/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.infinispan.commands;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.Assert;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Similarity;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.ReaderProvider;
import org.hibernate.search.infinispan.CacheManagerServiceProvider;
import org.hibernate.search.infinispan.impl.indexmanager.IndexUpdateCommand;
import org.hibernate.search.infinispan.impl.routing.CacheManagerMuxer;
import org.hibernate.search.spi.WorkerBuildContext;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.test.TestingUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Setup a cluster of two nodes and verify all components are wired
 * up to send a message using our custom commands.
 * In this case we send the command with sync=true to verify against
 * exceptions but we don't need the sync in practice.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class CommandReplicationTest {

	static volatile byte[] received = new byte[0];

	private static final String SOME_CACHE_NAME = "SomeCacheName";
	private static final String SOME_INDEX_NAME = "SomeIndexName";

	private static CacheManagerServiceProvider node1;
	private static CacheManagerServiceProvider node2;

	@Test
	public void verifyClusterFormed() {
		Address coordinator1 = node1.getService().getCoordinator();
		Address coordinator2 = node2.getService().getCoordinator();
		Assert.assertNotNull( coordinator1 );
		Assert.assertEquals( coordinator1, coordinator2 );
	}

	@Test
	public void verifyCommandSend() {
		final byte[] buffer = new byte[] { 1, 2, 36 };
		IndexUpdateCommand command = new IndexUpdateCommand( SOME_CACHE_NAME );
		command.setMessage( buffer );
		command.setIndexName( SOME_INDEX_NAME );
		Collection<Address> recipients = Collections.singleton( node2.getService().getAddress() );

		CacheManagerMuxer muxerNode2 = extractComponent( node2, CacheManagerMuxer.class, SOME_CACHE_NAME );
		Assert.assertNotNull( muxerNode2 );

		muxerNode2.activateIndexManager( SOME_INDEX_NAME, new MockIndexManager() );

		RpcManager rpcManager1 = node1.getService().getCache().getAdvancedCache().getRpcManager();
		rpcManager1.invokeRemotely( recipients, command, true );

		Assert.assertTrue( Arrays.equals( buffer, received ) );
	}

	@BeforeClass
	public static void prepareCluster() {
		node1 = createInfinispanNode();
		node2 = createInfinispanNode();
		TestingUtil.blockUntilViewsReceived( 5000L, node1.getService(), node2.getService() );
	}

	private static <T> T extractComponent(CacheManagerServiceProvider node, Class<T> componentType, String cacheName) {
		EmbeddedCacheManager cacheManager = node.getService();
		ComponentRegistry cr = cacheManager.getCache( cacheName ).getAdvancedCache().getComponentRegistry();
		return cr.getComponent( componentType );
	}

	@AfterClass
	public static void killCluster() {
		if ( node1 != null ) {
			node1.stop();
		}
		if ( node2 != null ) {
			node2.stop();
		}
	}

	private static CacheManagerServiceProvider createInfinispanNode() {
		CacheManagerServiceProvider dp = new CacheManagerServiceProvider();
		Properties cfg = new Properties();
		cfg.setProperty(
				CacheManagerServiceProvider.INFINISPAN_CONFIGURATION_RESOURCENAME,
				"testing-hibernatesearch-infinispan.xml" );
		//Start the CacheManager
		dp.start( cfg, null );
		//Make sure the Cache we're using is initialised
		dp.getService().getCache( SOME_CACHE_NAME );
		return dp;
	}

	/**
	 * Fake IndexManager which is going to store the received data
	 * as a byte buffer (instead of deserializing it) so we can assert
	 * the RPC infrastructure works fine in isolation from the Indexing
	 * engine.
	 */
	private static class MockIndexManager implements IndexManager {

		@Override
		public String getIndexName() {
			return null;
		}

		@Override
		public ReaderProvider getReaderProvider() {
			return null;
		}

		@Override
		public void performOperations(List<LuceneWork> queue, IndexingMonitor monitor) {
		}

		@Override
		public void performStreamOperation(LuceneWork singleOperation, IndexingMonitor monitor, boolean forceAsync) {
		}

		@Override
		public void initialize(String indexName, Properties properties, WorkerBuildContext context) {
		}

		@Override
		public void destroy() {
		}

		@Override
		public Set<Class<?>> getContainedTypes() {
			return null;
		}

		@Override
		public Similarity getSimilarity() {
			return null;
		}

		@Override
		public void setSimilarity(Similarity newSimilarity) {
		}

		@Override
		public Analyzer getAnalyzer(String name) {
			return null;
		}

		@Override
		public void setSearchFactory(SearchFactoryImplementor boundSearchFactory) {
		}

		@Override
		public void addContainedEntity(Class<?> entity) {
		}

		@Override
		public void optimize() {
		}

		@Override
		public LuceneWorkSerializer getSerializer() {
			return new LuceneWorkSerializer() {

				@Override
				public byte[] toSerializedModel(List<LuceneWork> works) {
					return null;
				}

				@Override
				public List<LuceneWork> toLuceneWorks(byte[] data) {
					CommandReplicationTest.received = data;
					return null;
				}

				@Override
				public String describeSerializer() {
					return "mock serializer used in tests";
				}

			};
		}

	}

}
