/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

public interface NestedPredicateBuilder extends SearchPredicateBuilder {

	void nested(SearchPredicate nestedPredicate);

}
