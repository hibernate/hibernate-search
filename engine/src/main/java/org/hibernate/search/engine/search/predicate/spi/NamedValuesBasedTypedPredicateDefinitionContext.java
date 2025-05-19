/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.common.spi.MapNamedValues;
import org.hibernate.search.engine.search.predicate.definition.TypedPredicateDefinitionContext;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class NamedValuesBasedTypedPredicateDefinitionContext<SR> implements TypedPredicateDefinitionContext<SR> {

	private final TypedSearchPredicateFactory<SR> factory;
	private final NamedValues parameters;

	public NamedValuesBasedTypedPredicateDefinitionContext(TypedSearchPredicateFactory<SR> factory, Map<String, Object> params,
			Function<String, SearchException> namedValueMissing) {
		this.factory = factory;
		this.parameters = MapNamedValues.fromMap( params, namedValueMissing );
	}

	@Override
	public TypedSearchPredicateFactory<SR> predicate() {
		return factory;
	}

	@Override
	public NamedValues params() {
		return parameters;
	}

}
