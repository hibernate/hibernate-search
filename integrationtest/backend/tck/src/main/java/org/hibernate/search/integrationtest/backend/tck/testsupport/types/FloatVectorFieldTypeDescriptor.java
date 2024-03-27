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

public class FloatVectorFieldTypeDescriptor extends VectorFieldTypeDescriptor<float[]> {

	public static final FloatVectorFieldTypeDescriptor INSTANCE = new FloatVectorFieldTypeDescriptor();

	private FloatVectorFieldTypeDescriptor() {
		this( 4 );
	}

	private FloatVectorFieldTypeDescriptor(int dimension) {
		super( float[].class, "float_vector", dimension );
	}

	@Override
	public VectorFieldTypeDescriptor<float[]> withDimension(int dimension) {
		return new FloatVectorFieldTypeDescriptor( dimension );
	}

	@Override
	public VectorFieldTypeOptionsStep<?, float[]> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.asFloatVector().dimension( size );
	}

	@Override
	public float[] sampleVector() {
		return arrayOf( size, new Random().nextFloat() );
	}

	@Override
	protected List<float[]> createUniquelyMatchableValues() {
		// need to make sure that we'll get only unique arrays;
		TreeSet<float[]> set = new TreeSet<>( Arrays::compare );
		set.add( arrayOf( size, -42.0f ) );
		set.add( arrayOf( size, -1.0f ) );
		set.add( arrayOf( size, 0.0f ) );
		set.add( arrayOf( size, 1.0f ) );
		set.add( arrayOf( size, 3.0f ) );
		set.add( arrayOf( size, 42.0f ) );

		return new ArrayList<>( set );
	}

	@Override
	protected List<float[]> createNonMatchingValues() {
		return Arrays.asList(
				arrayOf( size, -99.0f ),
				arrayOf( size, 2.0f ),
				arrayOf( size, 99.0f ),
				arrayOf( size, 100.0f ),
				arrayOf( size, 100.0001f ),
				arrayOf( size, 100.000045f )
		);
	}

	@Override
	public List<float[]> unitLengthVectors() {
		return IntStream.range( 0, size )
				.mapToObj( index -> unit( size, index ) )
				.collect( Collectors.toList() );
	}

	@Override
	public float[] valueFromInteger(int integer) {
		return arrayOf( size, integer );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<float[]>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				arrayOf( size, (byte) 0 ), arrayOf( size, (byte) 42 )
		) );
	}

	private static float[] arrayOf(int size, float startingValue) {
		float[] floats = new float[size];
		float sum = 0.0f;
		for ( int i = 0; i < size; i++ ) {
			floats[i] = startingValue + i;
			sum += floats[i] * floats[i];
		}
		sum = (float) Math.sqrt( size );
		for ( int i = 0; i < size; i++ ) {
			floats[i] = floats[i] / sum;
		}
		return floats;
	}

	private static float[] unit(int size, int index) {
		float[] floats = new float[size];
		Arrays.fill( floats, 0.0f );
		floats[index] = 1.0f;
		return floats;
	}
}
