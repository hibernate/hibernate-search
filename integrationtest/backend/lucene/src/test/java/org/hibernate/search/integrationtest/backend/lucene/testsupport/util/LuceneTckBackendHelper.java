/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.configuration.AnalysisCustomITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;

public class LuceneTckBackendHelper implements TckBackendHelper {

	private final LuceneTckBackendFeatures features = new LuceneTckBackendFeatures();

	@Override
	public TckBackendFeatures getBackendFeatures() {
		return features;
	}

	@Override
	public TckBackendSetupStrategy createDefaultBackendSetupStrategy() {
		return new LuceneTckBackendSetupStrategy();
	}

	@Override
	public TckBackendSetupStrategy createMultiTenancyBackendSetupStrategy() {
		return new LuceneTckBackendSetupStrategy()
				.setProperty( "multi_tenancy.strategy", "discriminator" );
	}

	@Override
	public TckBackendSetupStrategy createAnalysisCustomBackendSetupStrategy() {
		return new LuceneTckBackendSetupStrategy()
				.setProperty( "analysis.configurer", AnalysisCustomITAnalysisConfigurer.class.getName() );
	}

	@Override
	public TckBackendSetupStrategy createNoShardingBackendSetupStrategy() {
		// Sharding is disabled by default
		return createDefaultBackendSetupStrategy();
	}

	@Override
	public TckBackendSetupStrategy createHashBasedShardingBackendSetupStrategy(int shardCount) {
		return new LuceneTckBackendSetupStrategy()
				.setProperty( BackendSettings.INDEX_DEFAULTS + ".sharding.strategy", "hash" )
				.setProperty( BackendSettings.INDEX_DEFAULTS + ".sharding.number_of_shards",
						String.valueOf( shardCount ) );
	}

	@Override
	public TckBackendSetupStrategy createPeriodicRefreshBackendSetupStrategy(int refreshIntervalMs) {
		return new LuceneTckBackendSetupStrategy()
				.setProperty(
						BackendSettings.INDEX_DEFAULTS + ".io.refresh_interval",
						String.valueOf( refreshIntervalMs )
				);
	}
}
