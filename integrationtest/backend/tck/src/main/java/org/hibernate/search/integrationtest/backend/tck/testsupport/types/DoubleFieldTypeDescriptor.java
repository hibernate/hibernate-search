/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class DoubleFieldTypeDescriptor extends FieldTypeDescriptor<Double> {

	DoubleFieldTypeDescriptor() {
		super( Double.class );
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
	public Optional<FieldSortExpectations<Double>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				1.01, 3.2, 5.1,
				-Double.MIN_VALUE, 2.0, 4.00001, Double.MAX_VALUE
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Double>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				-1.001, 3.0, 5.1
		) );
	}
}
