/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractKnnPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

public final class DefaultKnnPredicateFieldStep
		extends AbstractKnnPredicateFieldStep<KnnPredicateOptionsStep<?>, KnnPredicateBuilder> {

	public DefaultKnnPredicateFieldStep(SearchPredicateFactory factory, SearchPredicateDslContext<?> dslContext, int k) {
		super( factory, dslContext, k );
	}

	@Override
	protected KnnPredicateBuilder createBuilder(String fieldPath) {
		return dslContext.scope().fieldQueryElement( fieldPath, PredicateTypeKeys.KNN );
	}

	@Override
	protected KnnPredicateOptionsStep<?> thisAsT() {
		return this;
	}
}
