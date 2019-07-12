/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.util.impl.integrationtest.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchBootstrapIT {

	private static final String BACKEND_NAME = "BackendName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy elasticsearchClientSpy = new ElasticsearchClientSpy();

	/**
	 * Check that we boot successfully when the Elasticsearch model dialect is determined
	 * from an explicitly configured Elasticsearch version,
	 * and that the Elasticsearch client starts in the second phase of bootstrap in that case.
	 */
	@Test
	public void explicitModelDialect() {
		SearchSetupHelper.PartialSetup partialSetup = setupHelper.start( BACKEND_NAME )
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSettings.VERSION, ElasticsearchTestDialect.getClusterVersion()
				)
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSpiSettings.CLIENT_FACTORY,
						elasticsearchClientSpy.getFactory()
				)
				.withIndexDefaultsProperty(
						BACKEND_NAME, ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						ElasticsearchIndexLifecycleStrategyName.NONE
				)
				.withIndex(
						"EmptyIndexName",
						ctx -> { }
				)
				.setupFirstPhaseOnly();

		// We do not expect the client to be created in the first phase
		assertThat( elasticsearchClientSpy.getCreatedClientCount() ).isEqualTo( 0 );
		elasticsearchClientSpy.verifyExpectationsMet();

		// In the *second* phase, however, we expect the client to be created and used to check the version
		elasticsearchClientSpy.expectNext(
				ElasticsearchRequest.get().build(), ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		partialSetup.doSecondPhase();
		elasticsearchClientSpy.verifyExpectationsMet();
	}

}
