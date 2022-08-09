/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.testsupport.util;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.configuration.AnalysisBuiltinOverrideITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.lucene.testsupport.configuration.AnalysisCustomITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.query.SlowQuery;

public class LuceneTckBackendHelper implements TckBackendHelper {

	private final LuceneTckBackendFeatures features = new LuceneTckBackendFeatures();

	@Override
	public TckBackendFeatures getBackendFeatures() {
		return features;
	}

	@Override
	public TckBackendSetupStrategy<?> createDefaultBackendSetupStrategy() {
		return new LuceneTckBackendSetupStrategy();
	}

	@Override
	public TckBackendSetupStrategy<?> createMultiTenancyBackendSetupStrategy() {
		return new LuceneTckBackendSetupStrategy()
				.setProperty( "multi_tenancy.strategy", "discriminator" );
	}

	@Override
	public TckBackendSetupStrategy<?> createAnalysisCustomBackendSetupStrategy() {
		return new LuceneTckBackendSetupStrategy()
				.expectCustomBeans()
				.setProperty( "analysis.configurer", AnalysisCustomITAnalysisConfigurer.class.getName() );
	}

	@Override
	public TckBackendSetupStrategy<?> createAnalysisNotConfiguredBackendSetupStrategy() {
		return new LuceneTckBackendSetupStrategy()
				.setProperty( "analysis.configurer", null );
	}

	@Override
	public TckBackendSetupStrategy<?> createAnalysisBuiltinOverridesBackendSetupStrategy() {
		return new LuceneTckBackendSetupStrategy()
				.expectCustomBeans()
				.setProperty( "analysis.configurer", AnalysisBuiltinOverrideITAnalysisConfigurer.class.getName() );
	}

	@Override
	public TckBackendSetupStrategy<?> createNoShardingBackendSetupStrategy() {
		// Sharding is disabled by default
		return createDefaultBackendSetupStrategy();
	}

	@Override
	public TckBackendSetupStrategy<?> createHashBasedShardingBackendSetupStrategy(int shardCount) {
		return new LuceneTckBackendSetupStrategy()
				.setProperty( "sharding.strategy", "hash" )
				.setProperty( "sharding.number_of_shards", String.valueOf( shardCount ) );
	}

	@Override
	public TckBackendSetupStrategy<?> createRarePeriodicRefreshBackendSetupStrategy() {
		return new LuceneTckBackendSetupStrategy()
				.setProperty( "io.refresh_interval", String.valueOf( 1_000_000 ) );
	}

	@Override
	public PredicateFinalStep createSlowPredicate(SearchPredicateFactory f) {
		return f.extension( LuceneExtension.get() )
				.fromLuceneQuery( new SlowQuery( 100 ) );
	}

	@Override
	public <R, E, LOS> SearchQueryDslExtension<? extends SearchQuerySelectStep<?, R, E, LOS, ?, ?>, R, E, LOS> queryDslExtension() {
		return LuceneExtension.get();
	}
}
