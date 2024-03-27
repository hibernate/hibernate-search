/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.common.ValueConvert;

public interface MatchIdPredicateBuilder extends SearchPredicateBuilder {

	void value(Object value, ValueConvert valueConvert);

}
