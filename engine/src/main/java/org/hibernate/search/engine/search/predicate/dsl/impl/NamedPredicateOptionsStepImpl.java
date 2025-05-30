/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl.impl;

import org.hibernate.search.engine.search.common.spi.SearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.ExtendedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.NamedPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.AbstractPredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateDslContext;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

public class NamedPredicateOptionsStepImpl
		extends AbstractPredicateFinalStep
		implements NamedPredicateOptionsStep {

	private final NamedPredicateBuilder builder;

	public NamedPredicateOptionsStepImpl(ExtendedSearchPredicateFactory<?, ?> predicateFactory,
			SearchPredicateDslContext<?> dslContext, String fieldPath, String predicateName) {
		super( dslContext );
		SearchIndexScope<?> scope = dslContext.scope();
		SearchQueryElementTypeKey<NamedPredicateBuilder> key = PredicateTypeKeys.named( predicateName );
		this.builder = fieldPath == null
				? scope.rootQueryElement( key )
				: scope.fieldQueryElement( fieldPath, key );
		builder.factory( fieldPath == null ? predicateFactory : predicateFactory.withRoot( fieldPath ) );
	}

	@Override
	public NamedPredicateOptionsStep param(String name, Object value) {
		builder.param( name, value );
		return this;
	}

	@Override
	protected SearchPredicate build() {
		return builder.build();
	}
}
