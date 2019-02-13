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

public class FloatFieldTypeDescriptor extends FieldTypeDescriptor<Float> {

	FloatFieldTypeDescriptor() {
		super( Float.class );
	}

	@Override
	public Optional<MatchPredicateExpectations<Float>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				42.1f, 67.0f
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Float>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				3.0f, 13.2f, 25.79f,
				10.0f, 19.101f
		) );
	}

	@Override
	public Optional<FieldSortExpectations<Float>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				1.01f, 3.2f, 5.1f,
				-Float.MIN_VALUE, 2.0f, 4.00001f, Float.MAX_VALUE
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Float>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				-1.001f, 3.0f, 5.1f
		) );
	}
}
