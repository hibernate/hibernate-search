/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.sort.impl;

import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;
import org.hibernate.search.engine.search.sort.spi.WithParametersSortBuilder;

class ElasticsearchWithParametersSort extends AbstractElasticsearchSort {

	private final ElasticsearchSearchIndexScope<?> scope;
	private final Function<? super NamedValues, ? extends SortFinalStep> sortCreator;

	ElasticsearchWithParametersSort(Builder builder) {
		super( builder );
		scope = builder.scope;
		sortCreator = builder.sortCreator;
	}

	@Override
	public void toJsonSorts(ElasticsearchSearchSortCollector collector) {
		SearchSort sort = sortCreator.apply( collector.getRootPredicateContext().queryParameters() ).toSort();

		ElasticsearchSearchSort.from( scope, sort ).toJsonSorts( collector );
	}

	static class Builder extends AbstractBuilder implements WithParametersSortBuilder {
		private Function<? super NamedValues, ? extends SortFinalStep> sortCreator;

		Builder(ElasticsearchSearchIndexScope<?> scope) {
			super( scope );
		}

		@Override
		public void creator(Function<? super NamedValues, ? extends SortFinalStep> sortCreator) {
			this.sortCreator = sortCreator;
		}

		@Override
		public SearchSort build() {
			return new ElasticsearchWithParametersSort( this );
		}
	}
}
