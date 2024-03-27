/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

public interface NamedPredicateBuilder extends SearchPredicateBuilder {

	void factory(SearchPredicateFactory factory);

	void param(String name, Object value);

}
