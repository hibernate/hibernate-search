/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class FloatFieldTypeDescriptor extends FieldTypeDescriptor<Float> {

	public static final FloatFieldTypeDescriptor INSTANCE = new FloatFieldTypeDescriptor();

	private FloatFieldTypeDescriptor() {
		super( Float.class );
	}

	@Override
	protected AscendingUniqueTermValues<Float> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<Float>() {
			@Override
			protected List<Float> createSingle() {
				return Arrays.asList(
						-251_484_254.849f,
						-42.42f,
						0.0f,
						22.0f,
						55f,
						2500.5100000045f,
						1584514514.000000184f,
						Float.MAX_VALUE
				);
			}

			@Override
			protected Float delta(int multiplierForDelta) {
				return 52.0f * multiplierForDelta;
			}

			@Override
			protected Float applyDelta(Float value, int multiplierForDelta) {
				return value + delta( multiplierForDelta );
			}
		};
	}

	@Override
	protected IndexableValues<Float> createIndexableValues() {
		return new IndexableValues<Float>() {
			@Override
			protected List<Float> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<Float> createUniquelyMatchableValues() {
		return Arrays.asList(
				Float.MIN_VALUE, Float.MAX_VALUE,
				-Float.MIN_VALUE, -Float.MAX_VALUE,
				// Elasticsearch doesn't support these: it fails when parsing them
				//Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN,
				0.0f,
				-0.0f, // Negative 0 is a different float
				42.42f,
				1584514514.000000184f,
				-1.001f, 3.0f, 5.1f
		);
	}

	@Override
	protected List<Float> createNonMatchingValues() {
		return Arrays.asList(
				123.12312312f, 0.7939397f, 739739.22121f, 92828212.12f
		);
	}

	@Override
	public Float valueFromInteger(int integer) {
		return (float) integer;
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Float>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				0.0f, 67.0f
		) );
	}
}
