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

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class DoubleFieldTypeDescriptor extends FieldTypeDescriptor<Double> {

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
			protected List<Double> create() {
				return Arrays.asList(
						Double.MIN_VALUE, Double.MAX_VALUE,
						- Double.MIN_VALUE, - Double.MAX_VALUE,
						// Elasticsearch doesn't support these: it fails when parsing them
						//Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN,
						0.0,
						-0.0, // Negative 0 is a different double
						Math.nextDown( 0.0 ),
						Math.nextUp( 0.0 ),
						42.42,
						1584514514.000000184,
						-1.001, 3.0, 5.1
				);
			}
		};
	}

	@Override
	public Optional<MatchPredicateExpectations<Double>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				42.1, 67.0
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Double>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				3.0, 13.2, 25.79,
				10.0, 19.101
		) );
	}

	@Override
	public ExistsPredicateExpectations<Double> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				0.0, 42.1
		);
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Double>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				0.0, 42.1
		) );
	}
}
