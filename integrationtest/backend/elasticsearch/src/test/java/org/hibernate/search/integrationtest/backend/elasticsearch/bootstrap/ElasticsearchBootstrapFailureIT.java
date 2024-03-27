/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchBootstrapFailureIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public ElasticsearchClientSpy elasticsearchClientSpy = ElasticsearchClientSpy.create();

	/**
	 * Check the reported failure when we fail to connect to the Elasticsearch cluster.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-3621")
	void cannotConnect() {
		assumeTrue(
				ElasticsearchTckBackendFeatures.supportsVersionCheck(),
				"This test only works if the very first request to Elasticsearch"
						+ " is a version check, i.e. if version checks are supported"
		);

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
