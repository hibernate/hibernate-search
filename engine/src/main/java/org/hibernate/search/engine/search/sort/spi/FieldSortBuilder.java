/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.spi;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;

public interface FieldSortBuilder extends SearchSortBuilder {

	void order(SortOrder order);

	void missingFirst();

	void missingLast();

	void missingHighest();

	void missingLowest();

	void missingAs(Object value, ValueConvert convert);

	void mode(SortMode mode);

	void filter(SearchPredicate filter);

}
