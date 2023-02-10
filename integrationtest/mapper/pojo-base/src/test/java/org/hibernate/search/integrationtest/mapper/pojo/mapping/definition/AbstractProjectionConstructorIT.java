/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;

import org.junit.Rule;

public abstract class AbstractProjectionConstructorIT {

	public static final String INDEX_NAME = "index_name";

	@Rule
	public BackendMock backendMock = new BackendMock();

	protected final <P> void testSuccessfulRootProjection(SearchMapping mapping, Class<?> indexedType, Class<P> projectionType,
			List<?> rawProjectionResults, List<P> expectedProjectionResults) {
		try ( SearchSession session = mapping.createSession() ) {
			backendMock.expectSearchProjection(
					INDEX_NAME,
					StubSearchWorkBehavior.of(
							rawProjectionResults.size(),
							rawProjectionResults
					)
			);

			assertThat( session.search( indexedType )
					.select( projectionType )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveComparison()
					.isEqualTo( expectedProjectionResults );
		}
		backendMock.verifyExpectationsMet();
	}
}
