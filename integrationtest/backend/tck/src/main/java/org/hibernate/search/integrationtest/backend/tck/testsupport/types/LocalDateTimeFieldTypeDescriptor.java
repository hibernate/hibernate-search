/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.FormatUtils;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.MetricAggregationsValues;

public class LocalDateTimeFieldTypeDescriptor extends StandardFieldTypeDescriptor<LocalDateTime> {

	public static final LocalDateTimeFieldTypeDescriptor INSTANCE = new LocalDateTimeFieldTypeDescriptor();

	private LocalDateTimeFieldTypeDescriptor() {
		super( LocalDateTime.class );
	}

	@Override
	protected AscendingUniqueTermValues<LocalDateTime> createAscendingUniqueTermValues() {
		// Remember: we only get millisecond precision for predicates/sorts/aggregations/etc.
		return new AscendingUniqueTermValues<LocalDateTime>() {
			@Override
			protected List<LocalDateTime> createSingle() {
				return Arrays.asList(
						LocalDateTime.of( -52, 10, 11, 10, 15, 30, 0 ),
						LocalDateTime.of( 1600, 2, 28, 13, 0, 23, 0 ),
						LocalDateTime.of( 1900, 1, 1, 0, 0, 0, 0 ),
						LocalDateTime.of( 1970, 1, 1, 0, 0, 0, 0 ),
						LocalDateTime.of( 1980, 1, 1, 0, 0, 0, 0 ),
						LocalDateTime.of( 1980, 12, 31, 12, 0, 0, 0 ),
						LocalDateTime.of( 2004, 2, 29, 1, 0, 0, 0 ),
						LocalDateTime.of( 2017, 7, 7, 11, 15, 30, 555_000_000 )
				);
			}

			@Override
			protected List<List<LocalDateTime>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected LocalDateTime applyDelta(LocalDateTime value, int multiplierForDelta) {
				return value.plus( multiplierForDelta, ChronoUnit.YEARS );
			}
		};
	}

	@Override
	public boolean supportsMetricAggregation() {
		return true;
	}

	@Override
	public MetricAggregationsValues<LocalDateTime> metricAggregationsValues() {
		return new MetricAggregationsValues<LocalDateTime>() {
			@Override
			protected LocalDateTime valueOf(int value) {
				return LocalDateTime.ofEpochSecond( value, 0, ZoneOffset.UTC );
			}

			@Override
			public LocalDateTime avg() {
				return LocalDateTime.parse( "1970-01-01T00:00:05.500" );
			}
		};
	}

	@Override
	protected IndexableValues<LocalDateTime> createIndexableValues() {
		return new IndexableValues<LocalDateTime>() {
			@Override
			protected List<LocalDateTime> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<LocalDateTime> createUniquelyMatchableValues() {
		return Arrays.asList(
				LocalDateTime.of( 1970, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 1980, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 1985, 5, 13, 10, 15, 30, 0 ),
				LocalDateTime.of( 2017, 7, 7, 11, 15, 30, 555_000_000 ),
				LocalDateTime.of( 1980, 10, 5, 12, 0, 0, 0 ),
				LocalDateTime.of( 1980, 12, 31, 23, 59, 59, 999_000_000 ),
				LocalDateTime.of( 2004, 2, 29, 1, 0, 0, 0 ),
				LocalDateTime.of( 1900, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 1600, 2, 28, 13, 0, 23, 0 ),
				LocalDateTime.of( -52, 10, 11, 10, 15, 30, 0 ),
				LocalDateTime.of( 22500, 10, 11, 17, 44, 0, 0 ),

				LocalDateTime.of( 2018, 2, 1, 14, 0, 15, 1 ),
				LocalDateTime.of( 2018, 3, 1, 0, 59 ),
				LocalDateTime.of( 2018, 4, 1, 23, 0 ),

				/*
				 * Minimum and maximum years that can be represented as number of millisecond since the epoch in a long.
				 * The minimum and maximum dates that can be represented are slightly before/after,
				 * but there's no point telling users these years are supported if not all months are supported.
				 */
				LocalDateTime.of( -292_275_054, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 292_278_993, 12, 31, 23, 59, 59, 999_000_000 )
		);
	}

	@Override
	protected List<LocalDateTime> createNonMatchingValues() {
		return Arrays.asList(
				LocalDateTime.of( 2099, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 2200, 1, 1, 0, 0, 0, 0 ),
				LocalDateTime.of( 728, 5, 13, 10, 15, 30, 0 ),
				LocalDateTime.of( -28, 7, 7, 11, 15, 30, 555_000_000 )
		);
	}

	@Override
	public LocalDateTime valueFromInteger(int integer) {
		return LocalDateTime.of( 2017, 11, 1, 0, 0, 0 )
				.plus( integer, ChronoUnit.SECONDS );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<LocalDateTime>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalDateTime.of( 1970, 1, 1, 0, 0, 0 ),
				LocalDateTime.of( 1984, 10, 7, 12, 14, 52 )
		) );
	}

	@Override
	public String format(LocalDateTime value) {
		return FormatUtils.format( value );
	}
}
