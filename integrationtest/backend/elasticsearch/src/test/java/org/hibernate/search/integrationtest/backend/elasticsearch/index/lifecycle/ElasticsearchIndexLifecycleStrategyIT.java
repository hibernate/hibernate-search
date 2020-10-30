/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.lifecycle;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that setting the obsolete index lifecycle strategy property fails.
 */
public class ElasticsearchIndexLifecycleStrategyIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3540")
	public void noCall() {
		StubMappedIndex index = StubMappedIndex.withoutFields();
		assertThatThrownBy(
				() -> setupHelper.start()
						.withBackendProperty(
								"lifecycle.strategy",
								"update"
						)
						.withIndex( index )
						.setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid value for configuration property 'hibernate.search.backend.lifecycle.strategy': 'update'",
						"The lifecycle strategy cannot be set at the index level anymore",
						"Set the schema management strategy via the property 'hibernate.search.schema_management.strategy' instead"
				);
	}

}
