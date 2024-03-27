/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;

public class ByteVectorFieldTypeDescriptor extends VectorFieldTypeDescriptor<byte[]> {

	public static final ByteVectorFieldTypeDescriptor INSTANCE = new ByteVectorFieldTypeDescriptor();

	private ByteVectorFieldTypeDescriptor() {
		this( 4 );
	}

	private ByteVectorFieldTypeDescriptor(int dimension) {
		super( byte[].class, "byte_vector", dimension );
	}

	@Override
	public VectorFieldTypeDescriptor<byte[]> withDimension(int dimension) {
		return new ByteVectorFieldTypeDescriptor( dimension );
	}

	@Override
	public VectorFieldTypeOptionsStep<?, byte[]> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.asByteVector().dimension( size );
	}

	@Override
	public int vectorSize() {
		return size;
	}

	@Override
	public byte[] sampleVector() {
		return arrayOf( size, new Random().nextInt( Byte.MAX_VALUE - 1 ) + 1 );
	}

	@Override
	protected List<byte[]> createUniquelyMatchableValues() {
		// need to make sure that we'll get only unique arrays;
		TreeSet<byte[]> set = new TreeSet<>( Arrays::compare );
		set.add( arrayOf( size, (byte) 0 ) );
		set.add( arrayOf( size, (byte) 1 ) );
		set.add( arrayOf( size, (byte) 2 ) );
		set.add( arrayOf( size, (byte) 3 ) );
		return new ArrayList<>( set );
	}

	@Override
	protected List<byte[]> createNonMatchingValues() {
		return Arrays.asList(
				arrayOf( size, Byte.MIN_VALUE ),
				arrayOf( size, Byte.MAX_VALUE ),
				arrayOf( size, (byte) -99 ),
				arrayOf( size, (byte) 2 ),
				arrayOf( size, (byte) 99 ),
				arrayOf( size, (byte) 100 )
		);
	}

	@Override
	public List<byte[]> unitLengthVectors() {
		return IntStream.range( 0, size )
				.mapToObj( index -> unit( size, index ) )
				.collect( Collectors.toList() );
	}

	@Override
	public byte[] valueFromInteger(int integer) {
		return arrayOf( size, integer );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<byte[]>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				arrayOf( size, (byte) 0 ), arrayOf( size, (byte) 42 )
		) );
	}

	private static byte[] arrayOf(int size, int value) {
		if ( value < Byte.MIN_VALUE || Byte.MAX_VALUE < value ) {
			throw new IllegalStateException( "Test dataset contains an out-of-bound value for byte: " + value );
		}
		return arrayOf( size, (byte) value );
	}

	private static byte[] arrayOf(int size, byte value) {
		byte[] bytes = new byte[size];
		for ( int i = 0; i < size; i++ ) {
			bytes[i] = (byte) ( ( i + value ) % size );
		}

		return bytes;
	}

	private static byte[] unit(int size, int index) {
		byte[] bytes = new byte[size];
		Arrays.fill( bytes, (byte) 0 );
		bytes[index] = (byte) 1;
		return bytes;
	}
}
