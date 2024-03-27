/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.StubSearchWorkAssert.assertThatSearchWork;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResultTotal;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubSearchWork;

class SearchWorkCall<T> extends Call<SearchWorkCall<?>> {

	private final Set<String> indexNames;
	private final StubSearchWork work;
	private final StubSearchProjectionContext projectionContext;
	private final SearchLoadingContext<?> loadingContext;
	private final StubSearchProjection<T> rootProjection;
	private final StubSearchWorkBehavior<?> behavior;
	private final Deadline deadline;

	SearchWorkCall(Set<String> indexNames,
			StubSearchWork work,
			StubSearchProjectionContext projectionContext,
			SearchLoadingContext<?> loadingContext,
			StubSearchProjection<T> rootProjection,
			Deadline deadline) {
		this.indexNames = indexNames;
		this.work = work;
		this.projectionContext = projectionContext;
		this.loadingContext = loadingContext;
		this.rootProjection = rootProjection;
		this.behavior = null;
		this.deadline = deadline;
	}

	SearchWorkCall(Set<String> indexNames,
			StubSearchWork work,
			StubSearchWorkBehavior<?> behavior) {
		this.indexNames = indexNames;
		this.work = work;
		this.projectionContext = null;
		this.loadingContext = null;
		this.rootProjection = null;
		this.behavior = behavior;
		this.deadline = null;
	}

	public <U> CallBehavior<SearchResult<U>> verify(SearchWorkCall<U> actualCall) {
		assertThat( actualCall.indexNames )
				.as( "Search work did not target the expected indexes: " )
				.isEqualTo( indexNames );
		assertThatSearchWork( actualCall.work )
				.as( "Search work on indexes " + indexNames + " did not match: " )
				.matches( work );

		return () -> new SimpleSearchResult<>(
				SimpleSearchResultTotal.exact( behavior.getTotalHitCount() ),
				getResults(
						actualCall.projectionContext,
						actualCall.loadingContext.createProjectionHitMapper(),
						actualCall.rootProjection,
						behavior.getRawHits(), actualCall.deadline
				),
				Collections.emptyMap(),
				Duration.ZERO, false
		);
	}

	@Override
	protected boolean isSimilarTo(SearchWorkCall<?> other) {
		return Objects.equals( indexNames, other.indexNames );
	}

	static <H> List<H> getResults(StubSearchProjectionContext actualProjectionContext,
			ProjectionHitMapper<?> actualProjectionHitMapper,
			StubSearchProjection<H> actualRootProjection,
			List<?> rawHits, Deadline deadline) {
		List<Object> extractedElements = new ArrayList<>( rawHits.size() );

		for ( Object rawHit : rawHits ) {
			actualProjectionContext.reset();
			extractedElements.add(
					actualRootProjection.extract( actualProjectionHitMapper, Collections.singleton( rawHit ).iterator(),
							actualProjectionContext )
			);
		}

		LoadingResult<?> loadingResult = actualProjectionHitMapper.loadBlocking( deadline );

		List<H> hits = new ArrayList<>( rawHits.size() );

		for ( Object extractedElement : extractedElements ) {
			actualProjectionContext.reset();

			H hit = actualRootProjection.transform( loadingResult, extractedElement, actualProjectionContext );
			if ( actualProjectionContext.hasFailedLoad() ) {
				// skip this hit
				continue;
			}

			hits.add( hit );
		}

		return hits;
	}

	@Override
	protected String summary() {
		return "search work execution on indexes '" + indexNames + "'; work = " + work;
	}
}
