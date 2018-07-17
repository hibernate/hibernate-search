/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;
import org.hibernate.search.util.AssertionFailure;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public abstract class AbstractSearchPredicateBuilder
		implements SearchPredicateBuilder<ElasticsearchSearchPredicateBuilder>,
				ElasticsearchSearchPredicateBuilder {

	private static final JsonAccessor<Float> BOOST = JsonAccessor.root().property( "boost" ).asFloat();

	private final JsonObject outerObject = new JsonObject();

	private final JsonObject innerObject = new JsonObject();

	private boolean built;

	@Override
	public void boost(float boost) {
		BOOST.set( getInnerObject(), boost );
	}

	@Override
	public ElasticsearchSearchPredicateBuilder toImplementation() {
		return this;
	}

	@Override
	public final JsonObject build() {
		if ( built ) {
			// we must never call a builder twice. Building may have side-effects.
			throw new AssertionFailure(
					"A predicate builder was called twice. There is a bug in Hibernate Search, please report it."
			);
		}
		built = true;
		return doBuild();
	}

	protected final JsonObject getInnerObject() {
		return innerObject;
	}

	protected final JsonObject getOuterObject() {
		return outerObject;
	}

	protected abstract JsonObject doBuild();
}
