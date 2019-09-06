/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.util.impl.integrationtest.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for aspects of the NONE lifecycle strategy
 * that are not addressed in other ITs.
 */
public class ElasticsearchSchemaNoneStrategyIT {

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy elasticsearchClientSpy = new ElasticsearchClientSpy();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	/**
	 * Just check that the NONE strategy works correctly and does not involve any call to the Elasticsearch cluster
	 * (except the one to get the Elasticsearch version, but that one is not caused by the lifecycle strategy).
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3540")
	public void noCall() {
		elasticsearchClientSpy.expectNext(
				ElasticsearchRequest.get().build(), ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		setupHelper.start( BACKEND_NAME )
				.withBackendProperty(
						BACKEND_NAME,
						ElasticsearchBackendSpiSettings.CLIENT_FACTORY,
						elasticsearchClientSpy.getFactory()
				)
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						IndexLifecycleStrategyName.NONE.getExternalRepresentation()
				)
				.withIndex(
						INDEX_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "field", f -> f.asString() ).toReference();
						},
						indexManager -> { }
				)
				.setup();

		elasticsearchClientSpy.verifyExpectationsMet();
	}

}
