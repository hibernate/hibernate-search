/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import java.util.Collection;

import org.hibernate.search.engine.search.common.ValueConvert;

public interface TermsPredicateBuilder extends SearchPredicateBuilder {

	void matchingAny(Collection<?> terms, ValueConvert convert);

	void matchingAll(Collection<?> terms, ValueConvert convert);

}
