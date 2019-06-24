/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.SpatialWithinPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


class SpatialWithinPredicateFieldStepImpl<B> implements SpatialWithinPredicateFieldStep {

	private final SpatialWithinPredicateFieldMoreStepImpl.CommonState<B> commonState;

	SpatialWithinPredicateFieldStepImpl(SearchPredicateBuilderFactory<?, B> factory) {
		this.commonState = new SpatialWithinPredicateFieldMoreStepImpl.CommonState<>( factory );
	}

	@Override
	public SpatialWithinPredicateFieldMoreStep onFields(String ... absoluteFieldPaths) {
		return new SpatialWithinPredicateFieldMoreStepImpl<>( commonState, Arrays.asList( absoluteFieldPaths ) );
	}
}
