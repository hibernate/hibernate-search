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

import org.hibernate.search.backend.elasticsearch.search.clause.impl.BooleanQueryClauseBuilder;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.ClauseBuilder;
import org.hibernate.search.engine.search.dsl.BooleanJunctionContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
class BooleanJunctionContextImpl<N>
		implements BooleanJunctionContext<N>, ClauseBuilder<JsonObject> {

	private final QueryTargetContext context;

	private final Supplier<N> nextContextProvider;

	private final BooleanQueryClauseBuilder builder;

	private final OccurContext must;
	private final OccurContext mustNot;
	private final OccurContext should;
	private final OccurContext filter;

	public BooleanJunctionContextImpl(QueryTargetContext context,
			Supplier<N> nextContextProvider) {
		this.context = context;
		this.nextContextProvider = nextContextProvider;
		this.builder = context.getClauseFactory().bool();
		this.must = new OccurContext();
		this.mustNot = new OccurContext();
		this.should = new OccurContext();
		this.filter = new OccurContext();
	}

	@Override
	public BooleanJunctionContext<N> boostedTo(float boost) {
		builder.boost( boost );
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
	public JsonObject build() {
		must.contribute( builder::must );
		mustNot.contribute( builder::mustNot );
		should.contribute( builder::should );
		filter.contribute( builder::filter );
		return builder.build();
	}

	@Override
	public N end() {
		return nextContextProvider.get();
	}

	private class OccurContext extends AbstractClauseContainerContext<BooleanJunctionContext<N>> {

		private final List<ClauseBuilder<JsonObject>> children = new ArrayList<>();

		public OccurContext() {
			super( BooleanJunctionContextImpl.this.context );
		}

		@Override
		protected void add(ClauseBuilder<JsonObject> child) {
			children.add( child );
		}

		public void contribute(Consumer<JsonObject> collector) {
			children.stream().map( ClauseBuilder::build ).forEach( collector );
		}

		@Override
		protected BooleanJunctionContext<N> getNext() {
			return BooleanJunctionContextImpl.this;
		}

	}

}
