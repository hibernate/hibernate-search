/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class OffsetTimeFieldTypeDescriptor extends FieldTypeDescriptor<OffsetTime> {

	OffsetTimeFieldTypeDescriptor() {
		super( OffsetTime.class );
	}

	@Override
	public Optional<IndexingExpectations<OffsetTime>> getIndexingExpectations() {
		List<OffsetTime> values = new ArrayList<>();
		LocalTimeFieldTypeDescriptor.getValuesForIndexingExpectations().forEach( localTime -> {
			OffsetDateTimeFieldTypeDescriptor.getOffsetsForIndexingExpectations().forEach( offset -> {
				values.add( localTime.atOffset( offset ) );
			} );
		} );
		return Optional.of( new IndexingExpectations<>( values ) );
	}

	@Override
	public Optional<MatchPredicateExpectations<OffsetTime>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				LocalTime.of( 11, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalTime.of( 7, 0, 3, 1029 ).atOffset( ZoneOffset.ofHours( -6 ) )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<OffsetTime>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				// Indexed
				LocalTime.of( 11, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalTime.of( 18, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalTime.of( 18, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				// Values around what is indexed
				LocalTime.of( 12, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalTime.of( 18, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( -3 ) )
		) );
	}

	@Override
	public ExistsPredicateExpectations<OffsetTime> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				LocalTime.of( 0, 0, 0 ).atOffset( ZoneOffset.UTC ),
				LocalTime.of( 12, 14, 52 ).atOffset( ZoneOffset.ofHours( 1 ) )
		);
	}

	@Override
	public Optional<FieldSortExpectations<OffsetTime>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				// Indexed
				LocalTime.of( 11, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalTime.of( 18, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalTime.of( 18, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( -6 ) ),
				// Values around what is indexed
				LocalTime.of( 11, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 9 ) ),
				LocalTime.of( 12, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalTime.of( 18, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( -3 ) ),
				LocalTime.of( 21, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( -6 ) )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<OffsetTime>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				LocalTime.of( 10, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalTime.of( 10, 0, 1, 1 ).atOffset( ZoneOffset.UTC ),
				LocalTime.of( 18, 2, 0, 1 ).atOffset( ZoneOffset.ofHours( -6 ) )
		) );
	}
}
