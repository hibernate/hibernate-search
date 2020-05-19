/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.index;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class IndexManagerIT {

	private static final String BACKEND_NAME = "MyBackend";

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final StubMappedIndex index = StubMappedIndex.withoutFields();

	private static Backend backendApi;
	private static IndexManager indexApi;

	@BeforeClass
	public static void setup() {
		SearchIntegration integration = setupHelper.start( BACKEND_NAME ).withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
		backendApi = integration.backend( BACKEND_NAME );
		indexApi = index.toApi();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void backend() {
		assertThat( indexApi.backend() )
				.isNotNull()
				.isSameAs( backendApi );
	}
}
