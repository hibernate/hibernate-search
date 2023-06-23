/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import java.util.Arrays;

import org.hibernate.search.engine.search.predicate.dsl.SpatialWithinPredicateFieldMoreStep;
import org.hibernate.search.engine.search.predicate.dsl.SpatialWithinPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

class SpatialWithinPredicateFieldStepImpl
		implements SpatialWithinPredicateFieldStep<SpatialWithinPredicateFieldMoreStep<?, ?>> {

	private final SpatialWithinPredicateFieldMoreStepImpl.CommonState commonState;

	SpatialWithinPredicateFieldStepImpl(SearchPredicateDslContext<?> dslContext) {
		this.commonState = new SpatialWithinPredicateFieldMoreStepImpl.CommonState( dslContext );
	}

	@Override
	public SpatialWithinPredicateFieldMoreStep<?, ?> fields(String... fieldPaths) {
		return new SpatialWithinPredicateFieldMoreStepImpl( commonState, Arrays.asList( fieldPaths ) );
	}
}
