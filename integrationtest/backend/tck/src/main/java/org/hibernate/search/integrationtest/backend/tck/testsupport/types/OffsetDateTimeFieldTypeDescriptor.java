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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class OffsetDateTimeFieldTypeDescriptor extends FieldTypeDescriptor<OffsetDateTime> {

	static List<ZoneOffset> getOffsetsForIndexingExpectations() {
		return Arrays.asList(
				ZoneOffset.UTC,
				ZoneOffset.ofHoursMinutes( -8, 0 ),
				ZoneOffset.ofHoursMinutes( -2, -30 ),
				ZoneOffset.ofHoursMinutes( -2, 0 ),
				ZoneOffset.ofHoursMinutes( 2, 0 ),
				ZoneOffset.ofHoursMinutes( 2, 30 ),
				ZoneOffset.ofHoursMinutes( 10, 0 ),
				ZoneOffset.ofHoursMinutesSeconds( 10, 0, 24 )
		);
	}

	OffsetDateTimeFieldTypeDescriptor() {
		super( OffsetDateTime.class );
	}

	@Override
	public Optional<IndexingExpectations<OffsetDateTime>> getIndexingExpectations() {
		List<OffsetDateTime> values = new ArrayList<>();
		LocalDateTimeFieldTypeDescriptor.getValuesForIndexingExpectations().forEach( localDateTime -> {
			getOffsetsForIndexingExpectations().forEach( offset -> {
				values.add( localDateTime.atOffset( offset ) );
			} );
		} );
		return Optional.of( new IndexingExpectations<>( values ) );
	}

	@Override
	public Optional<MatchPredicateExpectations<OffsetDateTime>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				LocalDateTime.of( 1980, 10, 11, 0, 15 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 1984, 10, 7, 15, 37, 37 ).atOffset( ZoneOffset.ofHours( -6 ) )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<OffsetDateTime>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				LocalDateTime.of( 2018, 2, 1, 0, 30 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 3, 18, 22, 57, 59 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 4, 1, 14, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				// Values around what is indexed
				LocalDateTime.of( 2018, 2, 15, 21, 10, 10 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 3, 18, 22, 57, 59 ).atOffset( ZoneOffset.ofHours( -6 ) )
		) );
	}

	@Override
	public ExistsPredicateExpectations<OffsetDateTime> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				LocalDateTime.of( 1970, 1, 1, 0, 0 ).atOffset( ZoneOffset.UTC ),
				LocalDateTime.of( 2018, 3, 1, 12, 14, 52 ).atOffset( ZoneOffset.ofHours( 1 ) )
		);
	}

	@Override
	public Optional<FieldSortExpectations<OffsetDateTime>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				// Indexed
				LocalDateTime.of( 2018, 2, 1, 13, 0 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				LocalDateTime.of( 2018, 3, 21, 0, 0 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				LocalDateTime.of( 2018, 4, 10, 23, 30 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				// Values around what is indexed
				LocalDateTime.of( 2018, 2, 1, 12, 0 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				LocalDateTime.of( 2018, 2, 15, 0, 0 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				LocalDateTime.of( 2018, 3, 21, 0, 0 ).atOffset( ZoneOffset.ofHours( -9 ) ),
				LocalDateTime.of( 2018, 5, 3, 0, 3 ).atOffset( ZoneOffset.ofHours( -6 ) )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<OffsetDateTime>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				LocalDateTime.of( 2018, 2, 1, 16, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 3, 1, 23, 59, 59 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atOffset( ZoneOffset.UTC )
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<OffsetDateTime>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalDateTime.of( 1970, 1, 1, 0, 0 ).atOffset( ZoneOffset.UTC ),
				LocalDateTime.of( 2018, 3, 1, 12, 14, 52 ).atOffset( ZoneOffset.ofHours( 1 ) )
		) );
	}
}
