/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import java.util.Set;

import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;

public interface SimpleQueryStringPredicateBuilder extends CommonQueryStringPredicateBuilder {

	void flags(Set<SimpleQueryFlag> flags);

}
