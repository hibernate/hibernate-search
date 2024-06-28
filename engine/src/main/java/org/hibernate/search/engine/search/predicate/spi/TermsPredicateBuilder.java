/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import java.util.Collection;

import org.hibernate.search.engine.search.common.ValueModel;

public interface TermsPredicateBuilder extends SearchPredicateBuilder {

	void matchingAny(Collection<?> terms, ValueModel valueModel);

	void matchingAll(Collection<?> terms, ValueModel valueModel);

}
