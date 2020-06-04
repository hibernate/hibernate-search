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

public class IntegerFieldTypeDescriptor extends FieldTypeDescriptor<Integer> {

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
				return Arrays.asList(
						Integer.MIN_VALUE, Integer.MAX_VALUE,
						-251_484_254, -42, -1, 0, 1, 3, 42, 151_484_254
				);
			}
		};
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
	public Optional<IndexNullAsMatchPredicateExpectactions<Integer>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				0, 12
		) );
	}
}
