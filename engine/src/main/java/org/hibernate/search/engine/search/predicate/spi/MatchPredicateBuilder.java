/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.common.ValueModel;

public interface MatchPredicateBuilder extends SearchPredicateBuilder, MinimumShouldMatchBuilder {

	void fuzzy(int maxEditDistance, int exactPrefixLength);

	void value(Object value, ValueModel valueModel);

	void analyzer(String analyzerName);

	void skipAnalysis();
}
