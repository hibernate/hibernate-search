/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

public interface PrefixPredicateBuilder extends SearchPredicateBuilder {

	void prefix(String prefix);

}
