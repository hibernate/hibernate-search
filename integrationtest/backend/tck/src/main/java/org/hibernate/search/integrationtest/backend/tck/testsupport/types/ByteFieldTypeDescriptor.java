/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class ByteFieldTypeDescriptor extends StandardFieldTypeDescriptor<Byte> {

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
						(byte) ( Byte.MAX_VALUE - 10 )
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
			protected List<Byte> createSingle() {
				return createUniquelyMatchableValues();
			}
		};
	}

	@Override
	protected List<Byte> createUniquelyMatchableValues() {
		return Arrays.asList(
				Byte.MIN_VALUE, Byte.MAX_VALUE,
				(byte) -42, (byte) -1, (byte) 0, (byte) 1, (byte) 3, (byte) 42
		);
	}

	@Override
	protected List<Byte> createNonMatchingValues() {
		return Arrays.asList(
				Byte.MIN_VALUE, Byte.MAX_VALUE,
				(byte) -99, (byte) 2, (byte) 99, (byte) 100
		);
	}

	@Override
	public Byte valueFromInteger(int integer) {
		return (byte) integer;
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<Byte>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				(byte) 0, (byte) 42
		) );
	}
}
