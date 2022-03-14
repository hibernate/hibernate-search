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

public class LongFieldTypeDescriptor extends FieldTypeDescriptor<Long> {

	public static final LongFieldTypeDescriptor INSTANCE = new LongFieldTypeDescriptor();

	private LongFieldTypeDescriptor() {
		super( Long.class );
	}

	@Override
	protected AscendingUniqueTermValues<Long> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<Long>() {
			@Override
			protected List<Long> createSingle() {
				return Arrays.asList(
						Long.MIN_VALUE,
						-251_484_254L,
						0L,
						42L,
						55L,
						2500L,
						151_484_254L,
						Long.MAX_VALUE
				);
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
		return Arrays.asList(
				Long.MIN_VALUE, Long.MAX_VALUE,
				(long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE,
				-251_484_254L, -42L, -1L, 0L, 1L, 3L, 42L, 151_484_254L
		);
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
