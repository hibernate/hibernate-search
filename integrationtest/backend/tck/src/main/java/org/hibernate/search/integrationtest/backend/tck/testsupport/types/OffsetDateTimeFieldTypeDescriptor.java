/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class OffsetDateTimeFieldTypeDescriptor extends FieldTypeDescriptor<OffsetDateTime> {

	OffsetDateTimeFieldTypeDescriptor() {
		super( OffsetDateTime.class );
	}

	@Override
	public Optional<MatchPredicateExpectations<OffsetDateTime>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				LocalDateTime.of( 1980, 10, 11, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 1984, 10, 7, 0, 0 ).atOffset( ZoneOffset.ofHours( -6 ) )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<OffsetDateTime>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				LocalDateTime.of( 2018, 2, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 4, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				// Values around what is indexed
				LocalDateTime.of( 2018, 2, 15, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( -6 ) )
		) );
	}

	@Override
	public Optional<FieldSortExpectations<OffsetDateTime>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				// Indexed
				LocalDateTime.of( 2018, 2, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				LocalDateTime.of( 2018, 4, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				// Values around what is indexed
				LocalDateTime.of( 2018, 1, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				LocalDateTime.of( 2018, 2, 15, 0, 0 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( -9 ) ),
				LocalDateTime.of( 2018, 5, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( -6 ) )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<OffsetDateTime>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				LocalDateTime.of( 2018, 2, 1, 0, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atOffset( ZoneOffset.UTC )
		) );
	}
}
