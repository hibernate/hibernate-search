/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilder;

import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;


/**
 * @author Guillaume Smet
 */
abstract class AbstractLuceneSearchPredicateBuilder implements SearchPredicateBuilder<LuceneSearchPredicateBuilder>,
		LuceneSearchPredicateBuilder {

	private Float boost;

	@Override
	public void boost(float boost) {
		this.boost = boost;
	}

	@Override
	public LuceneSearchPredicateBuilder toImplementation() {
		return this;
	}

	@Override
	public final Query build(LuceneSearchPredicateContext context) {
		if ( boost != null ) {
			return new BoostQuery( doBuild( context ), boost );
		}
		else {
			return doBuild( context );
		}
	}

	protected abstract Query doBuild(LuceneSearchPredicateContext context);
}
