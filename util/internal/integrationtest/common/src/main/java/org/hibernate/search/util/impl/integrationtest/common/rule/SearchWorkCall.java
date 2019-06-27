/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;
import org.hibernate.search.util.impl.integrationtest.common.assertion.StubSearchWorkAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;

class SearchWorkCall<T> extends Call<SearchWorkCall<?>> {

	private final Set<String> indexNames;
	private final StubSearchWork work;
	private final FromDocumentFieldValueConvertContext convertContext;
	private final LoadingContext<?, ?> loadingContext;
	private final StubSearchProjection<T> rootProjection;
	private final StubSearchWorkBehavior<?> behavior;

	SearchWorkCall(Set<String> indexNames,
			StubSearchWork work,
			FromDocumentFieldValueConvertContext convertContext,
			LoadingContext<?, ?> loadingContext,
			StubSearchProjection<T> rootProjection) {
		this.indexNames = indexNames;
		this.work = work;
		this.convertContext = convertContext;
		this.loadingContext = loadingContext;
		this.rootProjection = rootProjection;
		this.behavior = null;
	}

	SearchWorkCall(Set<String> indexNames,
			StubSearchWork work,
			StubSearchWorkBehavior<?> behavior) {
		this.indexNames = indexNames;
		this.work = work;
		this.convertContext = null;
		this.loadingContext = null;
		this.rootProjection = null;
		this.behavior = behavior;
	}

	public <U> CallBehavior<SearchResult<U>> verify(SearchWorkCall<U> actualCall) {
		assertThat( actualCall.indexNames )
				.as( "Search work did not target the expected indexes: " )
				.isEqualTo( indexNames );
		StubSearchWorkAssert.assertThat( actualCall.work )
				.as( "Search work on indexes " + indexNames + " did not match: " )
				.matches( work );

		long totalHitCount = behavior.getTotalHitCount();

		return () -> new SimpleSearchResult<>(
				totalHitCount,
				getResults(
						actualCall.convertContext,
						actualCall.loadingContext.getProjectionHitMapper(),
						actualCall.rootProjection,
						behavior.getRawHits()
				)
		);
	}

	@Override
	protected boolean isSimilarTo(SearchWorkCall<?> other) {
		return Objects.equals( indexNames, other.indexNames );
	}

	private static <U> List<U> getResults(FromDocumentFieldValueConvertContext actualConvertContext,
			ProjectionHitMapper<?, ?> actualProjectionHitMapper,
			StubSearchProjection<U> actualRootProjection,
			List<?> rawHits) {
		List<Object> extractedElements = new ArrayList<>( rawHits.size() );

		for ( Object rawHit : rawHits ) {
			extractedElements.add(
					actualRootProjection.extract( actualProjectionHitMapper, rawHit, actualConvertContext ) );
		}

		LoadingResult<?> loadingResult = actualProjectionHitMapper.loadBlocking();

		List<U> results = new ArrayList<>( rawHits.size() );

		for ( Object extractedElement : extractedElements ) {
			results.add( actualRootProjection.transform( loadingResult, extractedElement ) );
		}

		return results;
	}

	@Override
	public String toString() {
		return "search work execution on indexes '" + indexNames + "'; work = " + work;
	}

}
