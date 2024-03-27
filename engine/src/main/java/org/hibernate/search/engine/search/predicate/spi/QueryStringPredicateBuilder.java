/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.common.RewriteMethod;

public interface QueryStringPredicateBuilder extends CommonQueryStringPredicateBuilder {

	void allowLeadingWildcard(boolean allowLeadingWildcard);

	void enablePositionIncrements(boolean enablePositionIncrements);

	void phraseSlop(Integer phraseSlop);

	void rewriteMethod(RewriteMethod rewriteMethod, Integer n);
}
