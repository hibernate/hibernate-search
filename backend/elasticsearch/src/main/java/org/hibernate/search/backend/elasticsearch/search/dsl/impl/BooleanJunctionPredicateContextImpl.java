/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class BooleanJunctionPredicateContextImpl<N>
		implements BooleanJunctionPredicateContext<N>, ElasticsearchSearchPredicateContributor {

	private final SearchTargetContext context;

	private final Supplier<N> nextContextProvider;

	private final BooleanJunctionPredicateBuilder builder;

	private final OccurContext must;
	private final OccurContext mustNot;
	private final OccurContext should;
	private final OccurContext filter;

	public BooleanJunctionPredicateContextImpl(SearchTargetContext context,
			Supplier<N> nextContextProvider) {
		this.context = context;
		this.nextContextProvider = nextContextProvider;
		this.builder = context.getSearchPredicateFactory().bool();
		this.must = new OccurContext();
		this.mustNot = new OccurContext();
		this.should = new OccurContext();
		this.filter = new OccurContext();
	}

	@Override
	public BooleanJunctionPredicateContext<N> boostedTo(float boost) {
		builder.boost( boost );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> must(SearchPredicate searchPredicate) {
		must().add( ElasticsearchSearchPredicate.cast( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> mustNot(SearchPredicate searchPredicate) {
		mustNot().add( ElasticsearchSearchPredicate.cast( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> should(SearchPredicate searchPredicate) {
		should().add( ElasticsearchSearchPredicate.cast( searchPredicate ) );
		return this;
	}

	@Override
	public BooleanJunctionPredicateContext<N> filter(SearchPredicate searchPredicate) {
		filter().add( ElasticsearchSearchPredicate.cast( searchPredicate ) );
		return this;
	}

	@Override
	public OccurContext must() {
		return must;
	}

	@Override
	public OccurContext mustNot() {
		return mustNot;
	}

	@Override
	public OccurContext should() {
		return should;
	}

	@Override
	public OccurContext filter() {
		return filter;
	}

	@Override
	public void contribute(Consumer<JsonObject> collector) {
		must.contribute( builder::must );
		mustNot.contribute( builder::mustNot );
		should.contribute( builder::should );
		filter.contribute( builder::filter );
		JsonObject booleanPredicate = builder.build();
		collector.accept( booleanPredicate );
	}

	@Override
	public N end() {
		return nextContextProvider.get();
	}

	private class OccurContext extends AbstractSearchPredicateContainerContext<BooleanJunctionPredicateContext<N>> {

		private final List<ElasticsearchSearchPredicateContributor> children = new ArrayList<>();

		public OccurContext() {
			super( BooleanJunctionPredicateContextImpl.this.context );
		}

		@Override
		protected void add(ElasticsearchSearchPredicateContributor child) {
			children.add( child );
		}

		public void contribute(Consumer<JsonObject> collector) {
			children.forEach( c -> c.contribute( collector ) );
		}

		@Override
		protected BooleanJunctionPredicateContext<N> getNext() {
			return BooleanJunctionPredicateContextImpl.this;
		}

	}

}
