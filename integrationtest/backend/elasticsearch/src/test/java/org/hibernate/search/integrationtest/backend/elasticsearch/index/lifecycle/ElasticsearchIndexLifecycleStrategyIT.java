/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.lifecycle;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that setting the obsolete index lifecycle strategy property fails.
 */
public class ElasticsearchIndexLifecycleStrategyIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3540")
	public void noCall() {
		Assertions.assertThatThrownBy(
				() -> setupHelper.start()
						.withIndexDefaultsProperty(
								"lifecycle.strategy",
								"update"
						)
						.withIndex(
								INDEX_NAME,
								ctx -> { }
						)
						.setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unable to convert configuration property 'hibernate.search.backends.testedBackend.indexes." + INDEX_NAME
								+ ".lifecycle.strategy' with value 'update'",
						"The lifecycle strategy cannot be set at the index level anymore",
						"Set the schema management strategy via the property 'hibernate.search.schema_management.strategy' instead"
				);
	}

}
