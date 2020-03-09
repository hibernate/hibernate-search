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
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class IntegerFieldTypeDescriptor extends FieldTypeDescriptor<Integer> {

	IntegerFieldTypeDescriptor() {
		super( Integer.class );
	}

	@Override
	public List<Integer> getAscendingUniqueTermValues() {
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
	public Optional<IndexingExpectations<Integer>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>(
				Integer.MIN_VALUE, Integer.MAX_VALUE,
				-251_484_254, -42, -1, 0, 1, 3, 42, 151_484_254
		) );
	}

	@Override
	public Optional<MatchPredicateExpectations<Integer>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				42, 67
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Integer>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				3, 13, 25,
				10, 19
		) );
	}

	@Override
	public ExistsPredicateExpectations<Integer> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				0, 12
		);
	}

	@Override
	public Optional<FieldProjectionExpectations<Integer>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				1, 3, 5
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Integer>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				0, 12
		) );
	}
}
