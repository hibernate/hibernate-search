/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class LocalTimeFieldTypeDescriptor extends FieldTypeDescriptor<LocalTime> {

	public static final LocalTimeFieldTypeDescriptor INSTANCE = new LocalTimeFieldTypeDescriptor();

	private LocalTimeFieldTypeDescriptor() {
		super( LocalTime.class );
	}

	@Override
	protected AscendingUniqueTermValues<LocalTime> createAscendingUniqueTermValues() {
		// Remember: we only get millisecond precision for predicates/sorts/aggregations/etc.
		return new AscendingUniqueTermValues<LocalTime>() {
			@Override
			protected List<LocalTime> createSingle() {
				return Arrays.asList(
						LocalTime.of( 0, 0, 0, 0 ),
						LocalTime.of( 1, 0, 0, 0 ),
						LocalTime.of( 10, 15, 30, 0 ),
						LocalTime.of( 11, 15, 30, 555_000_000 ),
						LocalTime.of( 12, 0, 0, 0 ),
						LocalTime.of( 13, 0, 23, 0 ),
						LocalTime.of( 17, 59, 0, 0 ),
						LocalTime.of( 23, 59, 59, 999_000_000 )
				);
			}

			@Override
			protected List<List<LocalTime>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected LocalTime applyDelta(LocalTime value, int multiplierForDelta) {
				return value.plus( multiplierForDelta, ChronoUnit.MINUTES );
			}
		};
	}

	@Override
	protected IndexableValues<LocalTime> createIndexableValues() {
		return new IndexableValues<LocalTime>() {
			@Override
			protected List<LocalTime> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<LocalTime> createUniquelyMatchableValues() {
		return Arrays.asList(
				LocalTime.of( 0, 0, 0, 0 ),
				LocalTime.of( 10, 15, 30, 0 ),
				LocalTime.of( 11, 15, 30, 555_000_000 ),
				LocalTime.of( 23, 59, 59, 999_000_000 ),
				LocalTime.of( 10, 10, 10, 123_000_000 ),
				LocalTime.of( 11, 10, 12, 123_450_000 ),
				LocalTime.of( 12, 10, 11, 123_456_789 )
		);
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<LocalTime>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				LocalTime.of( 0, 0, 0 ),
				LocalTime.of( 12, 14, 52 )
		) );
	}
}
