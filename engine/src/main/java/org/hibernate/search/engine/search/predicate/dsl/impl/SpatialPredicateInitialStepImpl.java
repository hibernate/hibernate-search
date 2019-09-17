/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.SpatialPredicateInitialStep;
import org.hibernate.search.engine.search.predicate.dsl.SpatialWithinPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;

class SpatialPredicateInitialStepImpl<B> implements SpatialPredicateInitialStep {

	private final SearchPredicateBuilderFactory<?, B> factory;

	SpatialPredicateInitialStepImpl(SearchPredicateBuilderFactory<?, B> factory) {
		this.factory = factory;
	}

	@Override
	public SpatialWithinPredicateFieldStep<?> within() {
		return new SpatialWithinPredicateFieldStepImpl<>( factory );
	}
}
