/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate.impl;

import org.hibernate.search.engine.search.dsl.predicate.ExistsPredicateFieldStep;
import org.hibernate.search.engine.search.dsl.predicate.ExistsPredicateOptionsStep;
import org.hibernate.search.engine.search.dsl.predicate.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


final class ExistsPredicateFieldStepImpl<B>
		extends AbstractPredicateFinalStep<B>
		implements ExistsPredicateFieldStep, ExistsPredicateOptionsStep {

	private ExistsPredicateBuilder<B> builder;

	ExistsPredicateFieldStepImpl(SearchPredicateBuilderFactory<?, B> factory) {
		super( factory );
	}

	@Override
	public ExistsPredicateOptionsStep onField(String absoluteFieldPath) {
		this.builder = factory.exists( absoluteFieldPath );
		return this;
	}

	@Override
	public ExistsPredicateOptionsStep boostedTo(float boost) {
		this.builder.boost( boost );
		return this;
	}

	@Override
	protected B toImplementation() {
		return builder.toImplementation();
	}
}
