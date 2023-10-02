/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.WildcardPredicateOptionsStep;
import org.hibernate.search.query.dsl.TermTermination;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsWildcardQueryBuilder
		extends AbstractConnectedMultiFieldsQueryBuilder<TermTermination, WildcardPredicateOptionsStep<?>>
		implements TermTermination {

	private final Object value;

	public ConnectedMultiFieldsWildcardQueryBuilder(QueryBuildingContext queryContext, QueryCustomizer queryCustomizer,
			FieldsContext fieldsContext, Object value) {
		super( queryContext, queryCustomizer, fieldsContext );
		this.value = value;
	}

	@Override
	protected WildcardPredicateOptionsStep<?> createPredicate(SearchPredicateFactory factory, FieldContext fieldContext) {
		return fieldContext.applyBoost( factory.wildcard().field( fieldContext.getField() ) )
				.matching( value.toString() );
	}
}
