/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;

import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractProjectionConstructorIT {

	public static final String INDEX_NAME = "index_name";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	protected final ProjectionFinalStep<?> dummyProjectionForEnclosingClassInstance(SearchProjectionFactory<?, ?> f) {
		return f.constant( null );
	}

	protected final <P> void testSuccessfulRootProjectionExecutionOnly(SearchMapping mapping, Class<?> indexedType,
			Class<P> projectionType,
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

	protected final <P> void testSuccessfulRootProjection(SearchMapping mapping, Class<?> indexedType, Class<P> projectionType,
			List<?> rawProjectionResults,
			Function<SearchProjectionFactory<?, ?>, ProjectionFinalStep<?>> expectedProjection,
			List<P> expectedProjectionResults) {
		try ( SearchSession session = createSession( mapping ) ) {
			backendMock.expectSearchProjection(
					INDEX_NAME,
					b -> b.projection( expectedProjection.apply( mapping.scope( indexedType ).projection() ) ),
					StubSearchWorkBehavior.of(
							rawProjectionResults.size(),
							rawProjectionResults
					)
			);

			assertThat( session.search( indexedType )
					.select( projectionType )
					.where( f -> f.matchAll() )
					.fetchAllHits() )
					.usingRecursiveFieldByFieldElementComparator()
					.containsExactlyElementsOf( expectedProjectionResults );
		}
		backendMock.verifyExpectationsMet();
	}

	protected SearchSession createSession(SearchMapping mapping) {
		return mapping.createSession();
	}
}
