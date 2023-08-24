/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeTrue;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchBootstrapFailureIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy elasticsearchClientSpy = new ElasticsearchClientSpy();

	/**
	 * Check the reported failure when we fail to connect to the Elasticsearch cluster.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3621")
	public void cannotConnect() {
		assumeTrue( "This test only works if the very first request to Elasticsearch"
				+ " is a version check, i.e. if version checks are supported",
				ElasticsearchTckBackendFeatures.supportsVersionCheck() );

		assertThatThrownBy(
				() -> setupHelper.start()
						.withBackendProperty(
								ElasticsearchBackendSettings.URIS,
								// We just need a closed port, hopefully this one will generally be closed
								"http://localhost:9199"
						)
						.withIndex( StubMappedIndex.withoutFields() )
						.setup(),
				"Closed port"
		)
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.defaultBackendContext()
						.failure(
								"Unable to detect the Elasticsearch version running on the cluster",
								"Elasticsearch request failed",
								"Connection refused"
						)
				);
	}

}
