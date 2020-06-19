/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.ExistsPredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.ExistsPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;


final class ExistsPredicateFieldStepImpl
		extends AbstractPredicateFinalStep
		implements ExistsPredicateFieldStep<ExistsPredicateOptionsStep<?>>,
				ExistsPredicateOptionsStep<ExistsPredicateOptionsStep<?>> {

	private ExistsPredicateBuilder builder;

	ExistsPredicateFieldStepImpl(SearchPredicateBuilderFactory<?> builderFactory) {
		super( builderFactory );
	}

	@Override
	public ExistsPredicateOptionsStep<?> field(String absoluteFieldPath) {
		this.builder = builderFactory.exists( absoluteFieldPath );
		return this;
	}

	@Override
	public ExistsPredicateOptionsStep<?> boost(float boost) {
		this.builder.boost( boost );
		return this;
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}
}
