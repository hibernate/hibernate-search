/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class ZonedDateTimeFieldTypeDescriptor extends StandardFieldTypeDescriptor<ZonedDateTime> {

	public static final ZonedDateTimeFieldTypeDescriptor INSTANCE = new ZonedDateTimeFieldTypeDescriptor();

	private ZonedDateTimeFieldTypeDescriptor() {
		super( ZonedDateTime.class );
	}

	@Override
	public ZonedDateTime toExpectedDocValue(ZonedDateTime indexed) {
		if ( indexed == null ) {
			return null;
		}

		ZonedDateTime indexedAtUTC = indexed.withZoneSameInstant( ZoneOffset.UTC );

		return indexedAtUTC;
	}

	@Override
	protected AscendingUniqueTermValues<ZonedDateTime> createAscendingUniqueTermValues() {
		// Remember: we only get millisecond precision for predicates/sorts/aggregations/etc.
		return new AscendingUniqueTermValues<ZonedDateTime>() {
			@Override
			protected List<ZonedDateTime> createSingle() {
				return Arrays.asList(
						LocalDateTime.of( 2018, 1, 1, 12, 58, 30, 0 ).atZone( ZoneId.of( "Africa/Cairo" /* UTC+2 */ ) ),
						LocalDateTime.of( 2018, 2, 1, 8, 15, 30, 0 ).atZone( ZoneOffset.ofHours( -2 ) ),
						LocalDateTime.of( 2018, 2, 1, 2, 15, 30, 0 ).atZone( ZoneId.of( "Pacific/Honolulu" /* UTC-10 */ ) ),
						LocalDateTime.of( 2018, 2, 15, 20, 15, 30, 0 ).atZone( ZoneId.of( "Asia/Vladivostok" /* UTC+10 */ ) ),
						LocalDateTime.of( 2018, 3, 1, 8, 15, 30, 0 ).atZone( ZoneId.of( "UTC" ) ),
						LocalDateTime.of( 2018, 3, 1, 12, 15, 32, 0 ).atZone( ZoneOffset.ofHours( 4 ) ),
						LocalDateTime.of( 2018, 3, 15, 9, 15, 30, 0 ).atZone( ZoneId.of( "UTC" ) ),
						LocalDateTime.of( 2018, 3, 15, 11, 15, 30, 0 ).atZone( ZoneId.of( "Europe/Paris" /* UTC+1 */ ) ),
						LocalDateTime.of( 2018, 4, 1, 10, 15, 30, 0 ).atZone( ZoneId.of( "UTC" ) )
				);
			}

			@Override
			protected List<List<ZonedDateTime>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected ZonedDateTime applyDelta(ZonedDateTime value, int multiplierForDelta) {
				return value.plus( multiplierForDelta, ChronoUnit.DAYS );
			}
		};
	}

	@Override
	protected IndexableValues<ZonedDateTime> createIndexableValues() {
		return new IndexableValues<ZonedDateTime>() {
			@Override
			protected List<ZonedDateTime> createSingle() {
				List<ZonedDateTime> values = new ArrayList<>();
				for ( LocalDateTime localDateTime : LocalDateTimeFieldTypeDescriptor.INSTANCE.getIndexableValues()
						.getSingle() ) {
					for ( ZoneId zoneId : createIndexableZoneIdList() ) {
						values.add( localDateTime.atZone( zoneId ) );
					}
				}
				// HSEARCH-3557: Two date/times that could be ambiguous due to a daylight saving time switch
				Collections.addAll(
						values,
						LocalDateTime.parse( "2011-10-30T02:50:00.00" ).atZone( ZoneId.of( "CET" ) )
								.withEarlierOffsetAtOverlap(),
						LocalDateTime.parse( "2011-10-30T02:50:00.00" ).atZone( ZoneId.of( "CET" ) )
								.withLaterOffsetAtOverlap()
				);
				return values;
			}
		};
	}

	@Override
	protected List<ZonedDateTime> createUniquelyMatchableValues() {
		List<ZonedDateTime> values = new ArrayList<>();
		for ( LocalDateTime localDateTime : LocalDateTimeFieldTypeDescriptor.INSTANCE.getIndexableValues().getSingle() ) {
			for ( ZoneId zoneId : createIndexableZoneIdList() ) {
				values.add( localDateTime.atZone( zoneId ) );
			}
		}
		// Remove duplicates when it comes to matching timestamps: all dates are converted to UTC when indexed.
		Set<Instant> instants = new HashSet<>();
		List<ZonedDateTime> uniqueTimestampValues = new ArrayList<>();
		for ( ZonedDateTime value : values ) {
			Instant instant = value.toInstant();
			if ( instants.add( instant ) ) {
				uniqueTimestampValues.add( value );
			}
		}
		return uniqueTimestampValues;
	}

	@Override
	protected List<ZonedDateTime> createNonMatchingValues() {
		List<ZonedDateTime> values = new ArrayList<>();
		for ( LocalDateTime localDateTime : LocalDateTimeFieldTypeDescriptor.INSTANCE.getNonMatchingValues() ) {
			for ( ZoneId zoneId : createIndexableZoneIdList() ) {
				values.add( localDateTime.atZone( zoneId ) );
			}
		}
		return values;
	}

	private List<ZoneId> createIndexableZoneIdList() {
		return Arrays.asList(
				ZoneId.of( "UTC" ),
				ZoneId.of( "Europe/Paris" ),
				ZoneId.of( "Europe/London" ),
				ZoneId.of( "Europe/Berlin" ),
				ZoneId.of( "Europe/Warsaw" ),
				ZoneId.of( "America/Los_Angeles" ),
				ZoneId.of( "America/Argentina/Buenos_Aires" ),
				ZoneId.of( "America/Indiana/Winamac" ),
				ZoneId.of( "US/Pacific" ),
				// HSEARCH-3548: also test ZoneOffsets used as ZoneIds
				ZoneOffset.UTC, // Strangely, this is not the same as ZoneId.of( "UTC" )
				ZoneOffset.ofHoursMinutes( -2, 0 ),
				ZoneOffset.ofHoursMinutes( 2, 30 ),
				ZoneOffset.ofHoursMinutesSeconds( 10, 0, 24 )
		);
	}

	@Override
	public ZonedDateTime valueFromInteger(int integer) {
		return LocalDateTimeFieldTypeDescriptor.INSTANCE.valueFromInteger( integer )
				.atZone( ZoneId.of( "Europe/Paris" ) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<ZonedDateTime>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalDateTime.of( 1970, 1, 1, 0, 0 ).atZone( ZoneId.of( "UTC" ) ),
				LocalDateTime.of( 2018, 3, 1, 12, 14, 52 ).atZone( ZoneId.of( "Europe/Paris" ) )
		) );
	}
}
