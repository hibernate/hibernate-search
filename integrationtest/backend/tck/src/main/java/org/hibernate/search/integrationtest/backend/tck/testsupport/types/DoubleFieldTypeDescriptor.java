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

public class DoubleFieldTypeDescriptor extends StandardFieldTypeDescriptor<Double> {

	public static final DoubleFieldTypeDescriptor INSTANCE = new DoubleFieldTypeDescriptor();

	private DoubleFieldTypeDescriptor() {
		super( Double.class );
	}

	@Override
	protected AscendingUniqueTermValues<Double> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<Double>() {
			@Override
			protected List<Double> createSingle() {
				return Arrays.asList(
						-251_484_254.849,
						-42.42,
						0.0,
						22.0,
						55.0,
						2500.5100000045,
						1584514514.000000184,
						Double.MAX_VALUE
				);
			}

			@Override
			protected Double delta(int multiplierForDelta) {
				return 52.0 * multiplierForDelta;
			}

			@Override
			protected Double applyDelta(Double value, int multiplierForDelta) {
				return value + delta( multiplierForDelta );
			}
		};
	}

	@Override
	protected IndexableValues<Double> createIndexableValues() {
		return new IndexableValues<Double>() {
			@Override
			protected List<Double> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<Double> createUniquelyMatchableValues() {
		return Arrays.asList(
				Double.MIN_VALUE, Double.MAX_VALUE,
				-Double.MIN_VALUE, -Double.MAX_VALUE,
				// Elasticsearch doesn't support these: it fails when parsing them
				//Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN,
				0.0,
				-0.0, // Negative 0 is a different double
				42.42,
				1584514514.000000184,
				-1.001, 3.0, 5.1
		);
	}

	@Override
	protected List<Double> createNonMatchingValues() {
		return Arrays.asList(
				123.12312312d, 0.7939397d, 739739.22121d, 92828212.12d
		);
	}

	@Override
	public Double valueFromInteger(int integer) {
		return (double) integer;
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Double>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				0.0, 42.1
		) );
	}
}
