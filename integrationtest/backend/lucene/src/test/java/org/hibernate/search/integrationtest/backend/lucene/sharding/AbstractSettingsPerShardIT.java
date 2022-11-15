/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.sharding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;

/**
 * A basic test for explicit sharding with explicit routing keys.
 */
public abstract class AbstractSettingsPerShardIT {

	public static List<? extends Arguments> params() {
		Set<String> hashShardIds = CollectionHelper.asImmutableSet( "0", "1", "2", "3" );
		Set<String> explicitShardIds = CollectionHelper.asImmutableSet( "first", "second", "other", "yetanother" );

		return Arrays.asList(
				Arguments.of( "hash",
						(Function<TckBackendHelper,
								TckBackendSetupStrategy<
										?>>) ( helper -> helper.createHashBasedShardingBackendSetupStrategy( 4 ) ),
						new ArrayList<>( hashShardIds )
				),
				Arguments.of(
						"explicit",
						(Function<TckBackendHelper,
								TckBackendSetupStrategy<
										?>>) ( ignored -> ShardingExplicitIT.explicitShardingBackendSetupStrategy(
												explicitShardIds ) ),
						new ArrayList<>( explicitShardIds )
				)
		);
	}

	protected final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.ofAdvanced( IndexBinding::new );

	public String strategy;

	@RegisterExtension
	public SearchSetupHelper setupHelper = SearchSetupHelper.create();

	public List<String> shardIds;

	protected void init(String strategy, Function<TckBackendHelper, TckBackendSetupStrategy<?>> setupStrategyFunction,
			List<String> shardIds) {
		this.strategy = strategy;
		this.setupHelper.with( setupStrategyFunction );
		this.shardIds = shardIds;
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
