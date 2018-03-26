/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.assertion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.integrationtest.util.common.NormalizationUtils;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.SearchResult;

import org.assertj.core.api.Assertions;

public class ProjectionsSearchResultAssert<T extends List<?>>
		extends AbstractSearchResultAssert<ProjectionsSearchResultAssert<T>, T> {

	public static <T extends List<?>> ProjectionsSearchResultAssert<T> assertThat(SearchQuery<T> searchQuery) {
		return assertThat( searchQuery.execute() ).fromQuery( searchQuery );
	}

	public static <T extends List<?>> ProjectionsSearchResultAssert<T> assertThat(SearchResult<T> actual) {
		return new ProjectionsSearchResultAssert<>( actual );
	}

	private ProjectionsSearchResultAssert(SearchResult<T> actual) {
		super( actual );
	}

	public ProjectionsSearchResultAssert<T> hasProjectionsHitsExactOrder(Consumer<ProjectionsHitsBuilder> expectation) {
		ProjectionsHitsBuilder context = new ProjectionsHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualHits() )
				.as( "Hits of " + queryDescription )
				.containsExactly( context.getExpectedHits() );
		return this;
	}

	public ProjectionsSearchResultAssert<T> hasProjectionsHitsAnyOrder(Consumer<ProjectionsHitsBuilder> expectation) {
		ProjectionsHitsBuilder context = new ProjectionsHitsBuilder();
		expectation.accept( context );
		Assertions.assertThat( getNormalizedActualHits() )
				.as( "Hits of " + queryDescription )
				.containsOnly( context.getExpectedHits() );
		return this;
	}

	private List<List<?>> getNormalizedActualHits() {
		return actual.getHits().stream()
				.map( NormalizationUtils::normalizeProjection )
				.collect( Collectors.toList() );
	}

	public class ProjectionsHitsBuilder {
		private final List<List<?>> expectedHits = new ArrayList<>();

		private ProjectionsHitsBuilder() {
		}

		public ProjectionsHitsBuilder projection(Object ... projectionItems) {
			expectedHits.add( NormalizationUtils.normalizeProjection( Arrays.asList( projectionItems ) ) );
			return this;
		}

		@SuppressWarnings("rawtypes")
		private List[] getExpectedHits() {
			return expectedHits.toArray( new List[expectedHits.size()] );
		}
	}

}
