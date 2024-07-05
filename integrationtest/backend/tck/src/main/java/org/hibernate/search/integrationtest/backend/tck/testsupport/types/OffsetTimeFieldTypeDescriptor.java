/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
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

public class OffsetTimeFieldTypeDescriptor extends StandardFieldTypeDescriptor<OffsetTime> {

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
	protected IndexableValues<OffsetTime> createIndexableValues() {
		return new IndexableValues<OffsetTime>() {
			@Override
			protected List<OffsetTime> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<OffsetTime> createUniquelyMatchableValues() {
		List<OffsetTime> values = new ArrayList<>();
		for ( LocalTime localTime : LocalTimeFieldTypeDescriptor.INSTANCE.getIndexableValues().getSingle() ) {
			for ( ZoneOffset offset : OffsetDateTimeFieldTypeDescriptor.INSTANCE.createIndexableOffsetList() ) {
				values.add( localTime.atOffset( offset ) );
			}
		}
		// Remove duplicates when it comes to matching timestamps: all times are converted to UTC when indexed.
		Set<Instant> instants = new HashSet<>();
		List<OffsetTime> uniqueTimestampValues = new ArrayList<>();
		for ( OffsetTime value : values ) {
			Instant instant = value.atDate( LocalDate.of( 1970, 1, 1 ) ).toInstant();
			if ( instants.add( instant ) ) {
				uniqueTimestampValues.add( value );
			}
		}
		return uniqueTimestampValues;
	}

	@Override
	protected List<OffsetTime> createNonMatchingValues() {
		return Arrays.asList(
				LocalTime.of( 7, 4, 0, 0 ).atOffset( ZoneOffset.ofHours( 10 ) ),
				LocalTime.of( 7, 0, 0, 0 ).atOffset( ZoneOffset.ofHours( 2 ) ),
				LocalTime.of( 7, 15, 30, 0 ).atOffset( ZoneOffset.ofHours( -2 ) ),
				LocalTime.of( 7, 0, 23, 0 ).atOffset( ZoneOffset.ofHours( 0 ) )
		);
	}

	@Override
	public OffsetTime valueFromInteger(int integer) {
		return LocalTimeFieldTypeDescriptor.INSTANCE.valueFromInteger( integer ).atOffset( ZoneOffset.ofHours( 2 ) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<OffsetTime>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalTime.of( 0, 0, 0 ).atOffset( ZoneOffset.UTC ),
				LocalTime.of( 12, 14, 52 ).atOffset( ZoneOffset.ofHours( 1 ) )
		) );
	}
}
