/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.spi;

import java.util.Map;
import org.hibernate.search.engine.search.common.MultiValue;

import org.hibernate.search.util.common.data.Range;

public interface RangeAggregationBuilder<K> extends SearchAggregationBuilder<Map<Range<K>, Long>> {

	void range(Range<? extends K> range);

	void multi(MultiValue multi);
}
