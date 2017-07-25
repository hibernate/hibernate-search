/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.sharding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.engine.impl.MutableSearchFactory;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.impl.IndexShardingStrategyIndexManagerSelector;
import org.hibernate.search.indexes.impl.NotShardedIndexManagerSelector;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.search.store.ShardIdentifierProvider;
import org.hibernate.search.store.impl.FSDirectoryProvider;
import org.hibernate.search.store.impl.IdHashShardingStrategy;
import org.hibernate.search.test.util.impl.ExpectedLog4jLog;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * This tests verifies the different sharding configuration options
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-472")
public class ShardingConfigurationTest {

	@Rule
	public ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void testNoShardingIsUsedPerDefault() {
		MutableSearchFactory searchFactory = getSearchFactory( Collections.<String, String>emptyMap() );

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBindings().get( Foo.class );

		IndexManagerSelector selector = entityIndexBinding.getIndexManagerSelector();

		assertEquals(
				"No sharding should be configured. Number of shards and sharding strategy are not set",
				NotShardedIndexManagerSelector.class,
				selector.getClass()
		);
		assertEquals(
				"There should be exactly one shard",
				1,
				selector.all().size()
		);
	}

	@Test
	public void testSettingNumberOfShardsOnlySelectsIdHashSharding() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put( "hibernate.search.default.sharding_strategy.nbr_of_shards", "2" );

		MutableSearchFactory searchFactory = getSearchFactory( shardingProperties );

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBindings().get( Foo.class );

		IndexManagerSelector selector = entityIndexBinding.getIndexManagerSelector();

		assertEquals(
				"IndexShardingStrategyIndexManagerSelector should be used due to number of shards being set",
				IndexShardingStrategyIndexManagerSelector.class,
				selector.getClass()
		);
		assertEquals(
				"There should be exactly two shards",
				2,
				selector.all().size()
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
	public void testSettingIdHashShardingStrategyWithoutNumberOfShards() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put( "hibernate.search.default.sharding_strategy", IdHashShardingStrategy.class.getName() );

		logged.expectMessage( "HSEARCH000193", "IdHashShardingStrategy" );

		EntityIndexBinding entityIndexBinding = getSearchFactory( shardingProperties ).getIndexBindings().get( Foo.class );

		// 1 is assumed for legacy reasons. IMO not setting the number of shards should throw an exception
		assertTrue(
				"Without specifying number of shards, 1 should be assumed",
				entityIndexBinding.getIndexManagerSelector().all().size() == 1
		);
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

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBindings().get( Foo.class );

		IndexManagerSelector selector = entityIndexBinding.getIndexManagerSelector();

		assertEquals(
				"Explicitly set sharding strategy ignored",
				IndexShardingStrategyIndexManagerSelector.class,
				selector.getClass()
		);

		assertEquals(
				"Number of shards is explicitly set, but ignored",
				2,
				entityIndexBinding.getIndexManagerSelector().all().size()
		);

		assertEquals(
				"Explicitly set sharding strategy ignored",
				0,
				entityIndexBinding.getIndexManagerSelector().forExisting( new PojoIndexedTypeIdentifier( Foo.class ), null, null ).size()
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

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBindings().get( Foo.class );

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

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBindings().get( Foo.class );

		assertEquals(
				"Explicitly set shard id provider ignored",
				DummyShardIdentifierProvider.class,
				entityIndexBinding.getShardIdentifierProvider().getClass()
		);
	}

	@Test
	@Category(SkipOnElasticsearch.class) // This test is specific to Lucene
	public void testConfiguringDynamicallyCreatedShardViaConfiguration() {
		Map<String, String> shardingProperties = new HashMap<String, String>();
		shardingProperties.put(
				"hibernate.search.foo.sharding_strategy",
				DummyShardIdentifierProvider.class.getName()
		);

		shardingProperties.put( "hibernate.search.foo.snafu.directory_provider", "filesystem" );
		Path indexDir = TestConstants.getIndexDirectory( getTargetDir() );
		shardingProperties.put( "hibernate.search.foo.snafu.indexBase", indexDir.toAbsolutePath().toString() );

		MutableSearchFactory searchFactory = getSearchFactory( shardingProperties );

		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBindings().get( Foo.class );
		Set<IndexManager> indexManagers = entityIndexBinding.getIndexManagerSelector().all();

		assertTrue( "There should be two index managers", indexManagers.size() == 1 );
		assertTrue( "Unexpected index manager type", indexManagers.iterator().next() instanceof DirectoryBasedIndexManager );

		DirectoryBasedIndexManager directoryBasedIndexManager = (DirectoryBasedIndexManager) indexManagers.iterator().next();
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

		return (MutableSearchFactory) integratorResource.create( configuration );
	}

	private static Path getTargetDir() {
		URI classesDirUri;
		try {
			classesDirUri = ShardingConfigurationTest.class.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI();
		}
		catch (URISyntaxException e) {
			throw new RuntimeException( e );
		}

		return Paths.get( classesDirUri ).getParent();
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
		private final Set<String> shards = new HashSet<String>();

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
		public Set<String> getShardIdentifiersForDeletion(Class<?> entity, Serializable id, String idInString) {
			return null;
		}

		@Override
		public Set<String> getAllShardIdentifiers() {
			return shards;
		}
	}
}
