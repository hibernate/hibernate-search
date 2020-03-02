/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.sort;

import org.hibernate.search.engine.search.sort.dsl.SortOrder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.sort.dsl.SortMultiValue;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.search.sort.spi.ScoreSortBuilder;

public class StubSortBuilder implements ScoreSortBuilder<StubSortBuilder>,
		FieldSortBuilder<StubSortBuilder>, DistanceSortBuilder<StubSortBuilder> {

	@Override
	public StubSortBuilder toImplementation() {
		return this;
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

	void simulateBuild() {
		// No-op, just simulates a call on this object
	}

	@Override
	public void multi(SortMultiValue multi) {
		// No-op, just simulates a call on this object
	}
}
