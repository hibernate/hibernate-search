/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalTime;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class LocalTimeFieldTypeDescriptor extends FieldTypeDescriptor<LocalTime> {

	public static final int ONE_MILLION = 1000000;

	LocalTimeFieldTypeDescriptor() {
		super( LocalTime.class );
	}

	@Override
	public Optional<MatchPredicateExpectations<LocalTime>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				LocalTime.of( 10, 10, 10, 123 * ONE_MILLION ),
				LocalTime.of( 10, 10, 10, 122 * ONE_MILLION )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<LocalTime>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				LocalTime.of( 10, 10, 10, 0 ),
				LocalTime.of( 11, 10, 10, 0 ),
				LocalTime.of( 12, 10, 10, 0 ),
				// Values around what is indexed
				LocalTime.of( 10, 40, 10, 0 ),
				LocalTime.of( 11, 40, 10, 0 )
		) );
	}

	@Override
	public Optional<FieldSortExpectations<LocalTime>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				// Indexed
				LocalTime.of( 10, 10, 10, 0 ),
				LocalTime.of( 11, 10, 10, 0 ),
				LocalTime.of( 12, 10, 10, 0 ),
				// Values around what is indexed
				LocalTime.of( 9, 40, 10, 0 ),
				LocalTime.of( 10, 40, 10, 0 ),
				LocalTime.of( 11, 40, 10, 0 ),
				LocalTime.of( 12, 40, 10, 0 )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<LocalTime>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				LocalTime.of( 10, 10, 10, 123000000 ),
				LocalTime.of( 11, 10, 10, 123450000 ),
				LocalTime.of( 12, 10, 10, 123456789 )
		) );
	}
}
