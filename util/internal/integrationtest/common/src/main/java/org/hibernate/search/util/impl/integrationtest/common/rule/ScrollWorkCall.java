/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.StubSearchWorkAssert.assertThatSearchWork;

import java.util.Objects;
import java.util.Set;

import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchScroll;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchWork;

public class ScrollWorkCall<T> extends Call<ScrollWorkCall<?>> {

	private final Set<String> indexNames;
	private final StubSearchWork work;
	private final int chunkSize;
	private final StubBackendBehavior behavior;
	private final StubSearchProjectionContext projectionContext;
	private final SearchLoadingContext<?> loadingContext;
	private final StubSearchProjection<T> rootProjection;
	private final TimingSource timingSource;

	ScrollWorkCall(Set<String> indexNames, StubSearchWork work, int chunkSize, StubBackendBehavior behavior,
			StubSearchProjectionContext projectionContext, SearchLoadingContext<?> loadingContext,
			StubSearchProjection<T> rootProjection, TimingSource timingSource) {
		this.indexNames = indexNames;
		this.work = work;
		this.chunkSize = chunkSize;
		this.behavior = behavior;
		this.projectionContext = projectionContext;
		this.loadingContext = loadingContext;
		this.rootProjection = rootProjection;
		this.timingSource = timingSource;
	}

	ScrollWorkCall(Set<String> indexNames, StubSearchWork work, int chunkSize) {
		this.indexNames = indexNames;
		this.work = work;
		this.chunkSize = chunkSize;
		this.behavior = null;
		this.projectionContext = null;
		this.loadingContext = null;
		this.rootProjection = null;
		this.timingSource = null;
	}

	@Override
	protected String summary() {
		return "scroll work execution on indexes '" + indexNames + "'; work = " + work;
	}

	public <U> CallBehavior<SearchScroll<U>> verify(ScrollWorkCall<U> actualCall) {
		assertThat( actualCall.indexNames )
				.as( "Scroll work did not target the expected indexes: " )
				.isEqualTo( indexNames );
		assertThatSearchWork( actualCall.work )
				.as( "Scroll work on indexes " + indexNames + " did not match: " )
				.matches( work );
		assertThat( actualCall.chunkSize )
				.as( "Scroll work chunkSize did not match: " )
				.isEqualTo( chunkSize );

		return () -> new StubSearchScroll<>( actualCall.behavior, indexNames, actualCall.work,
				actualCall.projectionContext, actualCall.loadingContext, actualCall.rootProjection,
				actualCall.timingSource );
	}

	@Override
	protected boolean isSimilarTo(ScrollWorkCall<?> other) {
		return Objects.equals( indexNames, other.indexNames );
	}
}
