/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.bootstrap;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientFactoryImpl;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientFactory;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;

import org.junit.Rule;
import org.junit.Test;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

public class ElasticsearchBootstrapIT extends EasyMockSupport {

	private static final String BACKEND_NAME = "BackendName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	/**
	 * Check that we boot successfully when the Elasticsearch dialect is configured explicitly,
	 * and that the Elasticsearch client starts in the second phase of bootstrap in that case.
	 */
	@Test
	public void explicitDialect() {
		ElasticsearchClientFactory clientFactoryMock = createMock( ElasticsearchClientFactory.class );

		resetAll();
		// Do not expect any call to the client factory
		replayAll();
		SearchSetupHelper.PartialSetup partialSetup = setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSettings.DIALECT, dialect.getName()
				)
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSpiSettings.CLIENT_FACTORY, clientFactoryMock
				)
				.withIndex(
						"EmptyIndexName",
						ctx -> { },
						indexManager -> { }
				)
				.setupFirstPhaseOnly();
		verifyAll();

		resetAll();
		// NOW, we expect a call to the client factory
		EasyMock.expect( clientFactoryMock.create( EasyMock.anyObject(), EasyMock.anyObject() ) )
				.andDelegateTo( new ElasticsearchClientFactoryImpl() );
		replayAll();
		partialSetup.doSecondPhase();
		verifyAll();
	}

}
