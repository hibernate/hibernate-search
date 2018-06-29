/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.util.AssertionFailure;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public abstract class AbstractSearchPredicateBuilder
		implements SearchPredicateBuilder<Void, ElasticsearchSearchPredicateCollector> {

	private static final JsonAccessor<Float> BOOST = JsonAccessor.root().property( "boost" ).asFloat();

	private final JsonObject outerObject = new JsonObject();

	private final JsonObject innerObject = new JsonObject();

	private boolean contributed;

	@Override
	public void boost(float boost) {
		BOOST.set( getInnerObject(), boost );
	}

	protected JsonObject getInnerObject() {
		return innerObject;
	}

	protected JsonObject getOuterObject() {
		return outerObject;
	}

	protected JsonObject getQueryFromContributor(SearchPredicateContributor<Void, ? super ElasticsearchSearchPredicateCollector> queryContributor) {
		ElasticsearchSearchPredicateQueryBuilder queryBuilder = new ElasticsearchSearchPredicateQueryBuilder();
		queryContributor.contribute( null, queryBuilder );
		return queryBuilder.build();
	}

	@Override
	public final void contribute(Void context, ElasticsearchSearchPredicateCollector collector) {
		if ( contributed ) {
			// we must never call a contribution twice. Contributions may have side-effects.
			throw new AssertionFailure(
					"A predicate contributor was called twice. There is a bug in Hibernate Search, please report it."
			);
		}
		contributed = true;

		doContribute( context, collector );
	}

	protected abstract void doContribute(Void context, ElasticsearchSearchPredicateCollector collector);
}
