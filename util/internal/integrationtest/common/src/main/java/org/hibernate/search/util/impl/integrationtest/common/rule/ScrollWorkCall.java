/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.Set;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.util.impl.integrationtest.common.assertion.StubSearchWorkAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchScroll;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;

public class ScrollWorkCall<T> extends Call<ScrollWorkCall<?>> {

	private final Set<String> indexNames;
	private final StubSearchWork work;
	private final Integer chunkSize;
	private final StubBackendBehavior behavior;
	private final StubSearchProjectionContext projectionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final StubSearchProjection<T> rootProjection;

	ScrollWorkCall(Set<String> indexNames, StubSearchWork work, Integer chunkSize, StubBackendBehavior behavior,
			StubSearchProjectionContext projectionContext, LoadingContext<?, ?> loadingContext, StubSearchProjection<T> rootProjection) {
		this.indexNames = indexNames;
		this.work = work;
		this.chunkSize = chunkSize;
		this.behavior = behavior;
		this.projectionContext = projectionContext;
		this.loadingContext = loadingContext;
		this.rootProjection = rootProjection;
	}

	ScrollWorkCall(Set<String> indexNames, StubSearchWork work, Integer chunkSize) {
		this.indexNames = indexNames;
		this.work = work;
		this.chunkSize = chunkSize;
		this.behavior = null;
		this.projectionContext = null;
		this.loadingContext = null;
		this.rootProjection = null;
	}

	public <U> CallBehavior<SearchScroll<U>> verify(ScrollWorkCall<U> actualCall) {
		assertThat( actualCall.indexNames )
				.as( "Scroll work did not target the expected indexes: " )
				.isEqualTo( indexNames );
		StubSearchWorkAssert.assertThat( actualCall.work )
				.as( "Scroll work on indexes " + indexNames + " did not match: " )
				.matches( work );
		assertThat( actualCall.chunkSize )
				.as( "Scroll work chunkSize did not match: " )
				.isEqualTo( chunkSize );

		return () -> new StubSearchScroll<>( actualCall.behavior, indexNames, actualCall.projectionContext,
				actualCall.loadingContext, actualCall.rootProjection );
	}

	@Override
	protected boolean isSimilarTo(ScrollWorkCall other) {
		return Objects.equals( indexNames, other.indexNames );
	}
}
