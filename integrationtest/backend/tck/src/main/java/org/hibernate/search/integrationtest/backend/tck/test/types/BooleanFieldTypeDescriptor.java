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

public class BooleanFieldTypeDescriptor extends FieldTypeDescriptor<Boolean> {

	BooleanFieldTypeDescriptor() {
		super( Boolean.class );
	}

	@Override
	public Optional<MatchPredicateExpectations<Boolean>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				true, false
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Boolean>> getRangePredicateExpectations() {
		// Tested separately in BooleanSortAndRangePredicateIT, because we can only use two values
		return Optional.empty();
	}

	@Override
	public Optional<FieldSortExpectations<Boolean>> getFieldSortExpectations() {
		// Tested separately in BooleanSortAndRangePredicateIT, because we can only use two values
		return Optional.empty();
	}

	@Override
	public Optional<FieldProjectionExpectations<Boolean>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				true, false, true
		) );
	}
}
