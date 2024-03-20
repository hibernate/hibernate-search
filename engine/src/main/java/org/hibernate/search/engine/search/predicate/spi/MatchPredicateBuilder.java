/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.common.ValueConvert;

public interface MatchPredicateBuilder extends SearchPredicateBuilder {

	void fuzzy(int maxEditDistance, int exactPrefixLength);

	void value(Object value, ValueConvert convert);

	void param(String parameterName, ValueConvert convert);

	void analyzer(String analyzerName);

	void skipAnalysis();
}
