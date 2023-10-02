/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;

public interface WithParametersPredicateBuilder extends SearchPredicateBuilder {
	void creator(Function<? super NamedValues, ? extends PredicateFinalStep> predicateDefinition);
}
