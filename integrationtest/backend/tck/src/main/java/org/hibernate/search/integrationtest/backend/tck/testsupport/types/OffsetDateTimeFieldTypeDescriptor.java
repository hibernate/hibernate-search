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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;

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

	public static final OffsetDateTimeFieldTypeDescriptor INSTANCE = new OffsetDateTimeFieldTypeDescriptor();

	private OffsetDateTimeFieldTypeDescriptor() {
		super( OffsetDateTime.class );
	}

	@Override
	public OffsetDateTime toExpectedDocValue(OffsetDateTime indexed) {
		return indexed == null ? null : indexed.withOffsetSameInstant( ZoneOffset.UTC );
	}

	@Override
	protected AscendingUniqueTermValues<OffsetDateTime> createAscendingUniqueTermValues() {
		// Remember: we only get millisecond precision for predicates/sorts/aggregations/etc.
		return new AscendingUniqueTermValues<OffsetDateTime>() {
			@Override
			protected List<OffsetDateTime> createSingle() {
				return Arrays.asList(
						LocalDateTime.of( 2018, 1, 1, 12, 58, 30, 0 ).atOffset( ZoneOffset.ofHours( 2 ) ),
						LocalDateTime.of( 2018, 2, 1, 8, 15, 30, 0 ).atOffset( ZoneOffset.ofHours( -2 ) ),
						LocalDateTime.of( 2018, 2, 1, 2, 15, 30, 0 ).atOffset( ZoneOffset.ofHours( -10 ) ),
						LocalDateTime.of( 2018, 2, 15, 20, 15, 30, 0 ).atOffset( ZoneOffset.ofHours( 10 ) ),
						LocalDateTime.of( 2018, 3, 1, 8, 15, 30, 0 ).atOffset( ZoneOffset.ofHours( 0 ) ),
						LocalDateTime.of( 2018, 3, 1, 12, 15, 32, 0 ).atOffset( ZoneOffset.ofHours( 4 ) ),
						LocalDateTime.of( 2018, 3, 15, 9, 15, 30, 0 ).atOffset( ZoneOffset.ofHours( 0 ) ),
						LocalDateTime.of( 2018, 3, 15, 11, 15, 30, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
						LocalDateTime.of( 2018, 4, 1, 10, 15, 30, 0 ).atOffset( ZoneOffset.ofHours( 0 ) )
				);
			}

			@Override
			protected List<List<OffsetDateTime>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected OffsetDateTime applyDelta(OffsetDateTime value, int multiplierForDelta) {
				return value.plus( multiplierForDelta, ChronoUnit.DAYS );
			}
		};
	}

	@Override
	public IndexingExpectations<OffsetDateTime> getIndexingExpectations() {
		List<OffsetDateTime> values = new ArrayList<>();
		LocalDateTimeFieldTypeDescriptor.getValuesForIndexingExpectations().forEach( localDateTime -> {
			getOffsetsForIndexingExpectations().forEach( offset -> {
				values.add( localDateTime.atOffset( offset ) );
			} );
		} );
		return new IndexingExpectations<>( values );
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
	public FieldProjectionExpectations<OffsetDateTime> getFieldProjectionExpectations() {
		return new FieldProjectionExpectations<>(
				LocalDateTime.of( 2018, 2, 1, 16, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 3, 1, 23, 59, 59 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalDateTime.of( 2018, 3, 1, 0, 0 ).atOffset( ZoneOffset.UTC )
		);
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<OffsetDateTime>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalDateTime.of( 1970, 1, 1, 0, 0 ).atOffset( ZoneOffset.UTC ),
				LocalDateTime.of( 2018, 3, 1, 12, 14, 52 ).atOffset( ZoneOffset.ofHours( 1 ) )
		) );
	}
}
