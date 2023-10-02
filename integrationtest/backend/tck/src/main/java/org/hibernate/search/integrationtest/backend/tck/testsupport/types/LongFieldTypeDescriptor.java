/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;

public class LongFieldTypeDescriptor extends StandardFieldTypeDescriptor<Long> {

	public static final LongFieldTypeDescriptor INSTANCE = new LongFieldTypeDescriptor();

	private LongFieldTypeDescriptor() {
		super( Long.class );
	}

	@Override
	protected AscendingUniqueTermValues<Long> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<Long>() {
			@Override
			protected List<Long> createSingle() {
				List<Long> list = new ArrayList<>();
				if ( TckConfiguration.get().getBackendFeatures().supportsExtremeLongValues() ) {
					list.add( Long.MIN_VALUE );
				}
				Collections.addAll(
						list,
						-251_484_254L,
						-988L,
						-45L,
						0L,
						42L,
						55L,
						2500L,
						151_484_254L
				);
				if ( TckConfiguration.get().getBackendFeatures().supportsExtremeLongValues() ) {
					list.add( Long.MAX_VALUE );
				}
				return list;
			}

			@Override
			protected Long delta(int multiplierForDelta) {
				return 4245L * multiplierForDelta;
			}

			@Override
			protected Long applyDelta(Long value, int multiplierForDelta) {
				return value + delta( multiplierForDelta );
			}
		};
	}

	@Override
	protected IndexableValues<Long> createIndexableValues() {
		return new IndexableValues<Long>() {
			@Override
			protected List<Long> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<Long> createUniquelyMatchableValues() {
		List<Long> list = new ArrayList<>( Arrays.asList(
				-251_484_254L, -42L, -1L, 0L, 1L, 3L, 42L, 151_484_254L
		) );
		if ( TckConfiguration.get().getBackendFeatures().supportsExtremeLongValues() ) {
			Collections.addAll(
					list,
					Long.MIN_VALUE, Long.MAX_VALUE
			);
		}
		return list;
	}

	@Override
	protected List<Long> createNonMatchingValues() {
		return Arrays.asList(
				12_312_312_312L, 7_939_397L, 73_973_922_121L, 9_282_821_212L
		);
	}

	@Override
	public Long valueFromInteger(int integer) {
		return (long) integer;
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Long>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				0L, 67L
		) );
	}
}
