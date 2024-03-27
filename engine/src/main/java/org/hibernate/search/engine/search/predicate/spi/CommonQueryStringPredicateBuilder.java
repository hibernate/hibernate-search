/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.common.BooleanOperator;

public interface CommonQueryStringPredicateBuilder extends MinimumShouldMatchBuilder, SearchPredicateBuilder {

	FieldState field(String fieldPath);

	void defaultOperator(BooleanOperator operator);

	void queryString(String queryString);

	void analyzer(String analyzerName);

	void skipAnalysis();

	interface FieldState {
		void boost(float boost);
	}
}
