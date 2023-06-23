/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.sharding;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * A basic test for explicit sharding with explicit routing keys.
 */
@RunWith(Parameterized.class)
public abstract class AbstractSettingsPerShardIT {

	@Parameterized.Parameters(name = "{0} - {2}")
	public static Object[][] params() {
		Set<String> hashShardIds = CollectionHelper.asImmutableSet( "0", "1", "2", "3" );
		Set<String> explicitShardIds = CollectionHelper.asImmutableSet( "first", "second", "other", "yetanother" );

		return new Object[][] {
				{
						"hash",
						new SearchSetupHelper( helper -> helper.createHashBasedShardingBackendSetupStrategy( 4 ) ),
						new ArrayList<>( hashShardIds ) },
				{
						"explicit",
						new SearchSetupHelper(
								ignored -> ShardingExplicitIT.explicitShardingBackendSetupStrategy( explicitShardIds ) ),
						new ArrayList<>( explicitShardIds ) }
		};
	}

	protected final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.ofAdvanced( IndexBinding::new );

	public final String strategy;

	public final SearchSetupHelper setupHelper;

	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public final RuleChain rules;

	public final List<String> shardIds;

	protected AbstractSettingsPerShardIT(String strategy, SearchSetupHelper setupHelper, List<String> shardIds) {
		this.strategy = strategy;
		this.setupHelper = setupHelper;
		this.shardIds = shardIds;
		this.rules = RuleChain.outerRule( temporaryFolder ).around( setupHelper );
	}

	protected final String routingKey(int i) {
		if ( "explicit".equals( strategy ) ) {
			return shardIds.get( i % 4 );
		}
		else {
			return String.valueOf( i );
		}
	}

	protected static class IndexBinding {
		public IndexBinding(IndexedEntityBindingContext ctx) {
			ctx.explicitRouting();
		}
	}

}
