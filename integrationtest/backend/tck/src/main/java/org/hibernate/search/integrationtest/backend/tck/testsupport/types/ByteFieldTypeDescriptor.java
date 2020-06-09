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

public class ByteFieldTypeDescriptor extends FieldTypeDescriptor<Byte> {

	public static final ByteFieldTypeDescriptor INSTANCE = new ByteFieldTypeDescriptor();

	private ByteFieldTypeDescriptor() {
		super( Byte.class );
	}

	@Override
	protected AscendingUniqueTermValues<Byte> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<Byte>() {
			@Override
			protected List<Byte> createSingle() {
				return Arrays.asList(
						(byte) ( Byte.MIN_VALUE + 2 ),
						(byte) -58,
						(byte) 0,
						(byte) 42,
						(byte) 55,
						(byte) 70,
						(byte) 101,
						(byte) (Byte.MAX_VALUE - 10 )
				);
			}

			@Override
			protected Byte delta(int multiplierForDelta) {
				return toByteExact( multiplierForDelta );
			}

			@Override
			protected Byte applyDelta(Byte value, int multiplierForDelta) {
				return toByteExact( value + delta( multiplierForDelta ) );
			}

			private byte toByteExact(int value) {
				if ( value < Byte.MIN_VALUE || Byte.MAX_VALUE < value ) {
					throw new IllegalStateException( "Test dataset contains an out-of-bound value for byte: " + value );
				}
				return (byte) value;
			}
		};
	}

	@Override
	protected IndexableValues<Byte> createIndexableValues() {
		return new IndexableValues<Byte>() {
			@Override
			protected List<Byte> create() {
				return Arrays.asList(
						Byte.MIN_VALUE, Byte.MAX_VALUE,
						(byte) -42, (byte) -1, (byte) 0, (byte) 1, (byte) 3, (byte) 42
				);
			}
		};
	}

	@Override
	public Optional<MatchPredicateExpectations<Byte>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				(byte) 42, (byte) 67
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<Byte>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				(byte) 3, (byte) 13, (byte) 25,
				(byte) 10, (byte) 19
		) );
	}

	@Override
	public ExistsPredicateExpectations<Byte> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				(byte) 0, (byte) 42
		);
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Byte>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				(byte) 0, (byte) 42
		) );
	}
}
