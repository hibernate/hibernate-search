/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateOptionsCollector;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;

public final class BooleanPredicateClausesStepImpl<SR>
		extends
		AbstractBooleanPredicateClausesStep<SR, BooleanPredicateClausesStepImpl<SR>, BooleanPredicateOptionsCollector<SR, ?>>
		implements BooleanPredicateClausesStep<SR, BooleanPredicateClausesStepImpl<SR>> {

	public BooleanPredicateClausesStepImpl(SearchPredicateDslContext<?> dslContext,
			SearchPredicateFactory<SR> factory) {
		super( dslContext, factory );
	}

	@Override
	protected BooleanPredicateClausesStepImpl<SR> self() {
		return this;
	}

}
