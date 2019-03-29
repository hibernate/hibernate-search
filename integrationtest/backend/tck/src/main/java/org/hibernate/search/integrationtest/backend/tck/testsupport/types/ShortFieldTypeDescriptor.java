/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class ShortFieldTypeDescriptor extends FieldTypeDescriptor<Short> {

	ShortFieldTypeDescriptor() {
		super( Short.class );
	}

	@Override
	public Optional<MatchPredicateExpectations<Short>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				(short) 42, (short) 67
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Short>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				(short) 3, (short) 13, (short) 25,
				(short) 10, (short) 19
		) );
	}

	@Override
	public ExistsPredicateExpectations<Short> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				(short) 0, (short) 67
		);
	}

	@Override
	public Optional<FieldSortExpectations<Short>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				(short) 1, (short) 3, (short) 5,
				Short.MIN_VALUE, (short) 2, (short) 4, Short.MAX_VALUE
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<Short>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				(short) 1, (short) 3, (short) 5
		) );
	}
}
