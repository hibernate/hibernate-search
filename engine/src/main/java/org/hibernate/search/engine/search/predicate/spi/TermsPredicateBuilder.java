/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import java.util.Collection;

import org.hibernate.search.engine.search.common.spi.InputValueConvert;

public interface TermsPredicateBuilder extends SearchPredicateBuilder {

	void matchingAny(Collection<?> terms, InputValueConvert convert);

	void matchingAll(Collection<?> terms, InputValueConvert convert);

}
