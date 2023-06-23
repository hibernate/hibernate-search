/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import java.util.Set;

import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;

public interface SimpleQueryStringPredicateBuilder extends SearchPredicateBuilder {

	FieldState field(String fieldPath);

	void defaultOperator(BooleanOperator operator);

	void simpleQueryString(String simpleQueryString);

	void analyzer(String analyzerName);

	void skipAnalysis();

	void flags(Set<SimpleQueryFlag> flags);

	interface FieldState {

		void boost(float boost);

	}

}
