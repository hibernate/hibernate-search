/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
