/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.dsl.ExtendedSearchPredicateFactory;

public interface NamedPredicateBuilder extends SearchPredicateBuilder {

	void factory(ExtendedSearchPredicateFactory<?, ?> factory);

	void param(String name, Object value);

}
