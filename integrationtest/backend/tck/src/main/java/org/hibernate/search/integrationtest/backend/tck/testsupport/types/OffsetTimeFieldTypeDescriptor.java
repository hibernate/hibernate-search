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

public class OffsetTimeFieldTypeDescriptor extends FieldTypeDescriptor<OffsetTime> {

	public static final OffsetTimeFieldTypeDescriptor INSTANCE = new OffsetTimeFieldTypeDescriptor();

	private OffsetTimeFieldTypeDescriptor() {
		super( OffsetTime.class );
	}

	@Override
	public OffsetTime toExpectedDocValue(OffsetTime indexed) {
		return indexed == null ? null : indexed.withOffsetSameInstant( ZoneOffset.UTC );
	}

	@Override
	protected AscendingUniqueTermValues<OffsetTime> createAscendingUniqueTermValues() {
		// Remember: we only get millisecond precision for predicates/sorts/aggregations/etc.
		return new AscendingUniqueTermValues<OffsetTime>() {
			@Override
			protected List<OffsetTime> createSingle() {
				return Arrays.asList(
						LocalTime.of( 3, 4, 0, 0 ).atOffset( ZoneOffset.ofHours( 10 ) ),
						LocalTime.of( 2, 0, 0, 0 ).atOffset( ZoneOffset.ofHours( 2 ) ),
						LocalTime.of( 10, 15, 30, 0 ).atOffset( ZoneOffset.ofHours( -2 ) ),
						LocalTime.of( 13, 0, 23, 0 ).atOffset( ZoneOffset.ofHours( 0 ) ),
						LocalTime.of( 10, 0, 0, 0 ).atOffset( ZoneOffset.ofHours( -4 ) ),
						LocalTime.of( 19, 59, 0, 0 ).atOffset( ZoneOffset.ofHours( 2 ) ),
						LocalTime.of( 23, 59, 59, 999_000_000 ).atOffset( ZoneOffset.ofHours( 0 ) ),
						LocalTime.of( 21, 0, 0, 0 ).atOffset( ZoneOffset.ofHours( -4 ) )
				);
			}

			@Override
			protected List<List<OffsetTime>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected OffsetTime applyDelta(OffsetTime value, int multiplierForDelta) {
				return value.plus( multiplierForDelta, ChronoUnit.HOURS );
			}
		};
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
	public FieldProjectionExpectations<OffsetTime> getFieldProjectionExpectations() {
		return new FieldProjectionExpectations<>(
				LocalTime.of( 10, 0, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
				LocalTime.of( 10, 0, 1, 1 ).atOffset( ZoneOffset.UTC ),
				LocalTime.of( 18, 2, 0, 1 ).atOffset( ZoneOffset.ofHours( -6 ) )
		);
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<OffsetTime>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalTime.of( 0, 0, 0 ).atOffset( ZoneOffset.UTC ),
				LocalTime.of( 12, 14, 52 ).atOffset( ZoneOffset.ofHours( 1 ) )
		) );
	}
}
