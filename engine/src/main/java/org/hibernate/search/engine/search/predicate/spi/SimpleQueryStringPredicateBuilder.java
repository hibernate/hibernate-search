/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import java.util.EnumSet;
import org.hibernate.search.engine.search.common.BooleanOperator;

public interface SimpleQueryStringPredicateBuilder<B> extends SearchPredicateBuilder<B> {

	FieldState field(String absoluteFieldPath);

	void defaultOperator(BooleanOperator operator);

	void simpleQueryString(String simpleQueryString);

	void analyzer(String analyzerName);

	void skipAnalysis();

	void enable(SimpleQueryFlag operation);

	void enable(EnumSet<SimpleQueryFlag> operations);

	void disable(SimpleQueryFlag operation);

	void disable(EnumSet<SimpleQueryFlag> operations);

	interface FieldState<B> {

		void boost(float boost);

	}

}
