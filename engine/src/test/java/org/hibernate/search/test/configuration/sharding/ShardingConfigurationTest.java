/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.sharding;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.apache.lucene.document.Document;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.impl.MutableSearchFactory;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;
import org.hibernate.search.store.impl.FSDirectoryProvider;
import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.store.impl.NotShardedStrategy;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.BytemanHelper;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.TestForIssue;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This tests verifies the different sharding configuration options
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-472")
@RunWith(BMUnitRunner.class)
public class ShardingConfigurationTest {

	@Test
	public void testNoShardingIsUsedPerDefault() {
		MutableSearchFactory searchFactory = getSearchFactory( Collections.<String, String>emptyMap() );

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBinding( Foo.class );

		assertEquals(
				"No sharding should be configured. Number of shards and sharding strategy are not set",
				NotShardedStrategy.class,
				entityIndexBinding.getSelectionStrategy().getClass()
		);
	}

	@Test
	public void testSettingNumberOfShardsOnlySelectsIdHashSharding() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put( "hibernate.search.default.sharding_strategy.nbr_of_shards", "2" );

		MutableSearchFactory searchFactory = getSearchFactory( shardingProperties );

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBinding( Foo.class );

		assertEquals(
				"IdHashShardingStrategy should be selected due to number of shards being set",
				IdHashShardingStrategy.class,
				entityIndexBinding.getSelectionStrategy().getClass()
		);
	}

	@Test
	public void testSettingNegativeNumberOfShardsThrowsException() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put( "hibernate.search.default.sharding_strategy.nbr_of_shards", "-1" );

		try {
			getSearchFactory( shardingProperties );
			fail( "Factory creation should have failed" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message - " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000191" ) );
		}
	}

	@Test
	public void testSettingZeroNumberOfShardsThrowsException() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put( "hibernate.search.default.sharding_strategy.nbr_of_shards", "0" );

		try {
			getSearchFactory( shardingProperties );
			fail( "Factory creation should have failed" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message - " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000191" ) );
		}
	}

	@Test
	public void testSettingStringValueAsNumberOfShardsThrowsException() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put( "hibernate.search.default.sharding_strategy.nbr_of_shards", "snafu" );

		try {
			getSearchFactory( shardingProperties );
			fail( "Factory creation should have failed" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message - " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000192" ) );
		}
	}

	@Test
	@BMRule(targetClass = "org.hibernate.search.util.logging.impl.Log_$logger",
			targetMethod = "idHashShardingWithSingleShard",
			helper = "org.hibernate.search.testsupport.BytemanHelper",
			action = "countInvocation()",
			name = "testSettingIdHashShardingStrategyWithoutNumberOfShards")
	public void testSettingIdHashShardingStrategyWithoutNumberOfShards() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put( "hibernate.search.default.sharding_strategy", IdHashShardingStrategy.class.getName() );

		EntityIndexBinding entityIndexBinding = getSearchFactory( shardingProperties ).getIndexBinding( Foo.class );

		// 1 is assumed for legacy reasons. IMO not setting the number of shards should throw an exception
		assertTrue(
				"Without specifying number of shards, 1 should be assumed",
				entityIndexBinding.getSelectionStrategy().getIndexManagersForAllShards().length == 1
		);

		Assert.assertEquals( "Wrong invocation count", 1, BytemanHelper.getAndResetInvocationCount() );
	}

	@Test
	public void testSettingCustomIndexShardingStrategy() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put(
				"hibernate.search.default.sharding_strategy",
				DummyIndexShardingStrategy.class.getName()
		);
		shardingProperties.put( "hibernate.search.default.sharding_strategy.nbr_of_shards", "2" );

		MutableSearchFactory searchFactory = getSearchFactory( shardingProperties );

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBinding( Foo.class );

		assertEquals(
				"Explicitly set sharding strategy ignored",
				DummyIndexShardingStrategy.class,
				entityIndexBinding.getSelectionStrategy().getClass()
		);

		assertTrue(
				"Number of shards is explicitly set, but ignored",
				entityIndexBinding.getSelectionStrategy().getIndexManagersForAllShards().length == 2
		);
	}

	@Test
	public void testSettingCustomShardIdentifierProvider() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put(
				"hibernate.search.default.sharding_strategy",
				DummyShardIdentifierProvider.class.getName()
		);

		MutableSearchFactory searchFactory = getSearchFactory( shardingProperties );

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBinding( Foo.class );

		assertEquals(
				"Explicitly set shard id provider ignored",
				DummyShardIdentifierProvider.class,
				entityIndexBinding.getShardIdentifierProvider().getClass()
		);
	}

	@Test
	public void testSettingUnknownStrategyClassThrowsException() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put( "hibernate.search.default.sharding_strategy", "snafu" );

		try {
			getSearchFactory( shardingProperties );
			fail( "Factory creation should have failed" );
		}
		catch (SearchException e) {
			assertTrue( "Unexpected error message - " + e.getMessage(), e.getMessage().startsWith( "HSEARCH000194" ) );
		}
	}

	@Test
	public void testSettingCustomShardIdentifierProviderWithExplicitIndexName() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put(
				"hibernate.search.foo.sharding_strategy",
				DummyShardIdentifierProvider.class.getName()
		);

		MutableSearchFactory searchFactory = getSearchFactory( shardingProperties );

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBinding( Foo.class );

		assertEquals(
				"Explicitly set shard id provider ignored",
				DummyShardIdentifierProvider.class,
				entityIndexBinding.getShardIdentifierProvider().getClass()
		);
	}

	@Test
	public void testConfiguringDynamicallyCreatedShardViaConfiguration() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put(
				"hibernate.search.foo.sharding_strategy",
				DummyShardIdentifierProvider.class.getName()
		);

		shardingProperties.put( "hibernate.search.foo.snafu.directory_provider", "filesystem" );
		File indexDir = new File( TestConstants.getIndexDirectory( ShardingConfigurationTest.class ) );
		shardingProperties.put( "hibernate.search.foo.snafu.indexBase", indexDir.getAbsolutePath() );

		MutableSearchFactory searchFactory = getSearchFactory( shardingProperties );

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBinding( Foo.class );
		IndexManager indexManagers[] = entityIndexBinding.getIndexManagers();

		assertTrue( "There should be two index managers", indexManagers.length == 1 );
		assertTrue( "Unexpected index manager type", indexManagers[0] instanceof DirectoryBasedIndexManager );

		DirectoryBasedIndexManager directoryBasedIndexManager = (DirectoryBasedIndexManager) indexManagers[0];
		assertTrue(
				"Unexpected directory provider type: " + directoryBasedIndexManager.getDirectoryProvider().getClass(),
				directoryBasedIndexManager.getDirectoryProvider() instanceof FSDirectoryProvider
		);
	}

	private MutableSearchFactory getSearchFactory(Map<String, String> shardingProperties) {
		SearchConfigurationForTest configuration = new SearchConfigurationForTest();
		for ( Map.Entry<String, String> entry : shardingProperties.entrySet() ) {
			configuration.addProperty( entry.getKey(), entry.getValue() );
		}
		configuration.addClass( Foo.class );

		return (MutableSearchFactory) new SearchIntegratorBuilder().configuration(
				configuration
		).buildSearchIntegrator();
	}

	@Indexed(index = "foo")
	public static final class Foo {
		@DocumentId
		long id;
	}

	public static class DummyIndexShardingStrategy implements IndexShardingStrategy {

		private IndexManager[] indexManagers;

		@Override
		public void initialize(Properties properties, IndexManager[] indexManagers) {
			this.indexManagers = indexManagers;
		}

		@Override
		public IndexManager[] getIndexManagersForAllShards() {
			return indexManagers;
		}

		@Override
		public IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
			return null;
		}

		@Override
		public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
			return null;
		}

		@Override
		public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters) {
			return null;
		}
	}

	public static class DummyShardIdentifierProvider implements ShardIdentifierProvider {
		private Set<String> shards = new HashSet<String>();

		@Override
		public void initialize(Properties properties, BuildContext buildContext) {
			shards.add( "snafu" );
		}

		@Override
		public String getShardIdentifier(Class<?> entityType, Serializable id, String idAsString, Document document) {
			return null;
		}

		@Override
		public Set<String> getShardIdentifiersForQuery(FullTextFilterImplementor[] fullTextFilters) {
			return shards;
		}

		@Override
		public Set<String> getAllShardIdentifiers() {
			return shards;
		}
	}
}
