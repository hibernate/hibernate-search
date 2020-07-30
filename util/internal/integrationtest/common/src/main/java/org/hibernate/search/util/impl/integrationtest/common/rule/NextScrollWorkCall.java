/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchScrollResult;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;

public class NextScrollWorkCall<T> extends Call<NextScrollWorkCall<?>> {

	private final Set<String> indexNames;
	private final StubSearchProjectionContext projectionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final StubSearchProjection<T> rootProjection;
	private final StubSearchWorkBehavior<?> behavior;

	NextScrollWorkCall(Set<String> indexNames,
			StubSearchProjectionContext projectionContext,
			LoadingContext<?, ?> loadingContext,
			StubSearchProjection<T> rootProjection) {
		this.indexNames = indexNames;
		this.projectionContext = projectionContext;
		this.loadingContext = loadingContext;
		this.rootProjection = rootProjection;
		this.behavior = null;
	}

	NextScrollWorkCall(Set<String> indexNames,
			StubSearchWorkBehavior<?> behavior) {
		this.indexNames = indexNames;
		this.projectionContext = null;
		this.loadingContext = null;
		this.rootProjection = null;
		this.behavior = behavior;
	}

	public <U> CallBehavior<SearchScrollResult<U>> verify(NextScrollWorkCall<U> actualCall) {
		assertThat( actualCall.indexNames )
				.as( "NextScroll work did not target the expected indexes: " )
				.isEqualTo( indexNames );

		return () -> new SimpleSearchScrollResult( behavior.getTotalHitCount() > 0, SearchWorkCall.getResults(
				actualCall.projectionContext,
				actualCall.loadingContext.createProjectionHitMapper(),
				actualCall.rootProjection,
				behavior.getRawHits()
		), Duration.ZERO, false );
	}

	@Override
	protected boolean isSimilarTo(NextScrollWorkCall other) {
		return Objects.equals( indexNames, other.indexNames );
	}
}
