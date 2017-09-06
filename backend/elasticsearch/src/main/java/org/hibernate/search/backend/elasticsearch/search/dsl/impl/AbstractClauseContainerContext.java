/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.impl;

import org.hibernate.search.backend.elasticsearch.search.clause.impl.ClauseBuilder;
import org.hibernate.search.engine.search.dsl.BooleanJunctionContext;
import org.hibernate.search.engine.search.dsl.MatchClauseContext;
import org.hibernate.search.engine.search.dsl.QueryClauseContainerContext;
import org.hibernate.search.engine.search.dsl.RangeClauseContext;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
abstract class AbstractClauseContainerContext<N> implements QueryClauseContainerContext<N> {

	private final QueryTargetContext targetContext;

	public AbstractClauseContainerContext(QueryTargetContext targetContext) {
		this.targetContext = targetContext;
	}

	protected AbstractClauseContainerContext(AbstractClauseContainerContext<?> other) {
		this( other.targetContext );
	}

	@Override
	public BooleanJunctionContext<N> bool() {
		BooleanJunctionContextImpl<N> child = new BooleanJunctionContextImpl<>( targetContext, this::getNext );
		add( child );
		return child;
	}

	@Override
	public MatchClauseContext<N> match() {
		MatchClauseContextImpl<N> child = new MatchClauseContextImpl<>( targetContext, this::getNext );
		add( child );
		return child;
	}

	@Override
	public RangeClauseContext<N> range() {
		RangeClauseContextImpl<N> child = new RangeClauseContextImpl<>( targetContext, this::getNext );
		add( child );
		return child;
	}

	protected abstract void add(ClauseBuilder<JsonObject> child);

	protected abstract N getNext();

}
