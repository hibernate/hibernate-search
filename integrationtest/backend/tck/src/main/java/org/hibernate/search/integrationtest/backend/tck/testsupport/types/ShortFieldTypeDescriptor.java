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
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;

public class ShortFieldTypeDescriptor extends FieldTypeDescriptor<Short> {

	ShortFieldTypeDescriptor() {
		super( Short.class );
	}

	@Override
	protected AscendingUniqueTermValues<Short> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<Short>() {
			@Override
			protected List<Short> createSingle() {
				return Arrays.asList(
						(short) ( Short.MIN_VALUE + 50 ),
						(short) -25435,
						(short) 0,
						(short) 42,
						(short) 55,
						(short) 2500,
						(short) 18353,
						(short) ( Short.MAX_VALUE - 50 )
				);
			}

			@Override
			protected Short delta(int multiplierForDelta) {
				return toShortExact( 10 * multiplierForDelta );
			}

			@Override
			protected Short applyDelta(Short value, int multiplierForDelta) {
				return toShortExact( value + delta( multiplierForDelta ) );
			}

			private short toShortExact(int value) {
				if ( value < Short.MIN_VALUE || Short.MAX_VALUE < value ) {
					throw new IllegalStateException( "Test dataset contains an out-of-bound value for short: " + value );
				}
				return (short) value;
			}
		};
	}

	@Override
	public Optional<IndexingExpectations<Short>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>(
				Short.MIN_VALUE, Short.MAX_VALUE,
				(short) -25435, (short) -42, (short) -1, (short) 0, (short) 1, (short) 3, (short) 42, (short) 18353
		) );
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
	public Optional<FieldProjectionExpectations<Short>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				(short) 1, (short) 3, (short) 5
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Short>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				(short) 0, (short) 67
		) );
	}
}
