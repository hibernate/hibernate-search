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

public class ShortFieldTypeDescriptor extends StandardFieldTypeDescriptor<Short> {

	public static final ShortFieldTypeDescriptor INSTANCE = new ShortFieldTypeDescriptor();

	private ShortFieldTypeDescriptor() {
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
	protected IndexableValues<Short> createIndexableValues() {
		return new IndexableValues<Short>() {
			@Override
			protected List<Short> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<Short> createUniquelyMatchableValues() {
		return Arrays.asList(
				Short.MIN_VALUE, Short.MAX_VALUE,
				(short) -25435, (short) -42, (short) -1, (short) 0, (short) 1, (short) 3, (short) 42, (short) 18353
		);
	}

	@Override
	protected List<Short> createNonMatchingValues() {
		return Arrays.asList(
				(short) -99, (short) 2, (short) 99, (short) 10002
		);
	}

	@Override
	public Short valueFromInteger(int integer) {
		return (short) integer;
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Short>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				(short) 0, (short) 67
		) );
	}
}
