/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.MetricAggregationsValues;

public class OffsetDateTimeFieldTypeDescriptor extends StandardFieldTypeDescriptor<OffsetDateTime> {

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
	public boolean supportsMetricAggregation() {
		return true;
	}

	@Override
	public MetricAggregationsValues<OffsetDateTime> metricAggregationsValues() {
		return new MetricAggregationsValues<OffsetDateTime>() {
			@Override
			protected OffsetDateTime valueOf(int value) {
				return OffsetDateTime.ofInstant( Instant.ofEpochSecond( value * 100_000L ), ZoneOffset.UTC );
			}

			@Override
			public OffsetDateTime avg() {
				return OffsetDateTime.parse( "1970-01-07T08:46:40Z" );
			}

			@Override
			protected double doubleValueOf(double value) {
				return value * 100_000_000;
			}
		};
	}

	@Override
	protected IndexableValues<OffsetDateTime> createIndexableValues() {
		return new IndexableValues<OffsetDateTime>() {
			@Override
			protected List<OffsetDateTime> createSingle() {
				List<OffsetDateTime> values = new ArrayList<>();
				for ( LocalDateTime localDateTime : LocalDateTimeFieldTypeDescriptor.INSTANCE.getIndexableValues()
						.getSingle() ) {
					for ( ZoneOffset offset : createIndexableOffsetList() ) {
						values.add( localDateTime.atOffset( offset ) );
					}
				}
				return values;
			}
		};
	}

	@Override
	protected List<OffsetDateTime> createUniquelyMatchableValues() {
		List<OffsetDateTime> values = new ArrayList<>();
		for ( LocalDateTime localDateTime : LocalDateTimeFieldTypeDescriptor.INSTANCE.getIndexableValues()
				.getSingle() ) {
			for ( ZoneOffset offset : createIndexableOffsetList() ) {
				values.add( localDateTime.atOffset( offset ) );
			}
		}
		// Remove duplicates when it comes to matching timestamps: all dates are converted to UTC when indexed.
		Set<Instant> instants = new HashSet<>();
		List<OffsetDateTime> uniqueTimestampValues = new ArrayList<>();
		for ( OffsetDateTime value : values ) {
			Instant instant = value.toInstant();
			if ( instants.add( instant ) ) {
				uniqueTimestampValues.add( value );
			}
		}
		return uniqueTimestampValues;
	}

	@Override
	protected List<OffsetDateTime> createNonMatchingValues() {
		List<OffsetDateTime> values = new ArrayList<>();
		for ( LocalDateTime localDateTime : LocalDateTimeFieldTypeDescriptor.INSTANCE.getNonMatchingValues() ) {
			for ( ZoneOffset offset : createIndexableOffsetList() ) {
				values.add( localDateTime.atOffset( offset ) );
			}
		}
		return values;
	}

	List<ZoneOffset> createIndexableOffsetList() {
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

	@Override
	public OffsetDateTime valueFromInteger(int integer) {
		return LocalDateTimeFieldTypeDescriptor.INSTANCE.valueFromInteger( integer ).atOffset(
				ZoneOffset.ofHours( 2 ) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<OffsetDateTime>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalDateTime.of( 1970, 1, 1, 0, 0 ).atOffset( ZoneOffset.UTC ),
				LocalDateTime.of( 2018, 3, 1, 12, 14, 52 ).atOffset( ZoneOffset.ofHours( 1 ) )
		) );
	}

	@Override
	public String format(OffsetDateTime value) {
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format( value );
	}

	@Override
	public OffsetDateTime normalize(OffsetDateTime value) {
		return value == null ? null : value.toInstant().atOffset( ZoneOffset.UTC );
	}
}
