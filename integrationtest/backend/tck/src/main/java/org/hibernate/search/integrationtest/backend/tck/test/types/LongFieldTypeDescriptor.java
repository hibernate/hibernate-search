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

public class LongFieldTypeDescriptor extends FieldTypeDescriptor<Long> {

	LongFieldTypeDescriptor() {
		super( Long.class );
	}

	@Override
	public Optional<MatchPredicateExpectations<Long>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				42L, 67L
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Long>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				3L, 13L, 25L,
				10L, 19L
		) );
	}

	@Override
	public Optional<FieldSortExpectations<Long>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				1L, 3L, 5L,
				Long.MIN_VALUE, 2L, 4L, Long.MAX_VALUE
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Long>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				1L, 3L, 5L
		) );
	}
}
