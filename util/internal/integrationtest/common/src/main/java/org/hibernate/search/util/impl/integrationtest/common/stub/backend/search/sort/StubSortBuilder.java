/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort;

import org.hibernate.search.engine.search.common.SortMode;
import org.hibernate.search.engine.search.sort.SearchSort;
import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.sort.spi.CompositeSortBuilder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

public class StubSortBuilder implements ScoreSortBuilder,
		FieldSortBuilder, DistanceSortBuilder, CompositeSortBuilder {

	@Override
	public SearchSort build() {
		return new StubSearchSort();
	}

	@Override
	public void missingFirst() {
		// No-op
	}

	@Override
	public void missingLast() {
		// No-op
	}

	@Override
	public void missingAs(Object value, ValueConvert convert) {
		// No-op
	}

	@Override
	public void order(SortOrder order) {
		// No-op
	}

	@Override
	public void mode(SortMode mode) {
		// No-op
	}

	@Override
	public void filter(SearchPredicate clauseBuilder) {
		// No-op, just simulates a call on this object
	}

	@Override
	public void add(SearchSort sort) {
		// No-op
	}
}
