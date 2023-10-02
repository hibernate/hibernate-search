/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class IntegerFieldTypeDescriptor extends StandardFieldTypeDescriptor<Integer> {

	public static final IntegerFieldTypeDescriptor INSTANCE = new IntegerFieldTypeDescriptor();

	private IntegerFieldTypeDescriptor() {
		super( Integer.class );
	}

	@Override
	protected AscendingUniqueTermValues<Integer> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<Integer>() {
			@Override
			protected List<Integer> createSingle() {
				return Arrays.asList(
						Integer.MIN_VALUE,
						-251_484_254,
						0,
						42,
						55,
						2500,
						151_484_254,
						Integer.MAX_VALUE
				);
			}

			@Override
			protected Integer delta(int multiplierForDelta) {
				return 4245 * multiplierForDelta;
			}

			@Override
			protected Integer applyDelta(Integer value, int multiplierForDelta) {
				return value + delta( multiplierForDelta );
			}
		};
	}

	@Override
	protected IndexableValues<Integer> createIndexableValues() {
		return new IndexableValues<Integer>() {
			@Override
			protected List<Integer> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<Integer> createUniquelyMatchableValues() {
		return Arrays.asList(
				Integer.MIN_VALUE, Integer.MAX_VALUE,
				-251_484_254, -42, -1, 0, 1, 3, 42, 151_484_254
		);
	}

	@Override
	protected List<Integer> createNonMatchingValues() {
		return Arrays.asList(
				12_312_312, 7_939_397,
				73_973_922, 9_282_821
		);
	}

	@Override
	public Integer valueFromInteger(int integer) {
		return integer;
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Integer>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				0, 12
		) );
	}
}
