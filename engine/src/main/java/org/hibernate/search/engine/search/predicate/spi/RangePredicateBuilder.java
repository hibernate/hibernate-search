/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.util.common.data.Range;

public interface RangePredicateBuilder extends SearchPredicateBuilder {

	void range(Range<?> range, ValueConvert convertLowerBound, ValueConvert convertUpperBound);

	void param(String parameterName, ValueConvert lowerBoundConvert, ValueConvert upperBoundConvert);

	void parameterized(Range<String> range, ValueConvert lowerBoundConvert, ValueConvert upperBoundConvert);
}
