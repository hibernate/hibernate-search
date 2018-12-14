/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.test.types;

import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.test.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.test.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.test.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.test.types.expectations.RangePredicateExpectations;

public class IntegerFieldTypeDescriptor extends FieldTypeDescriptor<Integer> {

	IntegerFieldTypeDescriptor() {
		super( Integer.class );
	}

	@Override
	public Optional<MatchPredicateExpectations<Integer>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				42, 67
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Integer>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				3, 13, 25,
				10, 19
		) );
	}

	@Override
	public Optional<FieldSortExpectations<Integer>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				1, 3, 5,
				Integer.MIN_VALUE, 2, 4, Integer.MAX_VALUE
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Integer>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				1, 3, 5
		) );
	}
}
