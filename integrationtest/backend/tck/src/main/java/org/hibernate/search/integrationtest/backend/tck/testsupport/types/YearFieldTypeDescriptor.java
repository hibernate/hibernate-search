/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class YearFieldTypeDescriptor extends StandardFieldTypeDescriptor<Year> {

	public static final YearFieldTypeDescriptor INSTANCE = new YearFieldTypeDescriptor();

	private YearFieldTypeDescriptor() {
		super( Year.class );
	}

	@Override
	protected AscendingUniqueTermValues<Year> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<Year>() {
			@Override
			protected List<Year> createSingle() {
				return Arrays.asList(
						Year.of( -25435 ),
						Year.of( 0 ),
						Year.of( 42 ),
						Year.of( 1989 ),
						Year.of( 1999 ),
						Year.of( 2000 ),
						Year.of( 2019 ),
						Year.of( 2050 )
				);
			}

			@Override
			protected List<List<Year>> createMultiResultingInSingleAfterSum() {
				return valuesThatWontBeUsed();
			}

			@Override
			protected Year applyDelta(Year value, int multiplierForDelta) {
				return value.plus( multiplierForDelta, ChronoUnit.YEARS );
			}
		};
	}

	@Override
	protected IndexableValues<Year> createIndexableValues() {
		return new IndexableValues<Year>() {
			@Override
			protected List<Year> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<Year> createUniquelyMatchableValues() {
		return Arrays.asList(
				Year.of( -25435 ), Year.of( -42 ), Year.of( -1 ),
				Year.of( 0 ),
				Year.of( 1 ), Year.of( 3 ), Year.of( 42 ), Year.of( 18353 ),
				Year.of( 1989 ),
				Year.of( 1999 ),
				Year.of( 2000 ),
				Year.of( 2019 ),
				Year.of( 2050 ),
				/*
				 * Minimum and maximum years that can be represented as number of millisecond since the epoch in a long.
				 * The minimum and maximum dates that can be represented are slightly before/after,
				 * but there's no point telling users these years are supported if not all months are supported.
				 */
				Year.of( -292_275_054 ),
				Year.of( 292_278_993 )
		);
	}

	@Override
	protected List<Year> createNonMatchingValues() {
		return Arrays.asList(
				Year.of( 2720 ), Year.of( 2621 ), Year.of( 2522 ), Year.of( 2423 )
		);
	}

	@Override
	public Year valueFromInteger(int integer) {
		return Year.of( 2000 + integer );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Year>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				Year.of( 1970 ), Year.of( 4302 )
		) );
	}
}
