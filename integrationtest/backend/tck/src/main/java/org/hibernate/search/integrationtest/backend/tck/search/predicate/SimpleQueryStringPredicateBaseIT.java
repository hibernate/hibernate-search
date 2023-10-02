/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryStringPredicateFieldStep;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class SimpleQueryStringPredicateBaseIT extends AbstractBaseQueryStringPredicateBaseIT<SimpleQueryStringPredicateFieldStep<?>> {
	//CHECKSTYLE:ON
	@Override
	SimpleQueryStringPredicateFieldStep<?> predicate(SearchPredicateFactory f) {
		return f.simpleQueryString();
	}

	@Override
	protected String predicateTrait() {
		return "predicate:simple-query-string";
	}
}
