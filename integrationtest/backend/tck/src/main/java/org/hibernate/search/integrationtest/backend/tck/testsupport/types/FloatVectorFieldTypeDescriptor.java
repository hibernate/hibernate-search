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
import java.util.Random;

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
		return fieldContext.asFloatVector( size );
	}

	@Override
	public float[] sampleVector() {
		return arrayOf( size, new Random().nextFloat() );
	}

	@Override
	protected List<float[]> createUniquelyMatchableValues() {
		return Arrays.asList(
				arrayOf( size, -42.0f ),
				arrayOf( size, -1.0f ),
				arrayOf( size, 0.0f ),
				arrayOf( size, 1.0f ),
				arrayOf( size, 3.0f ),
				arrayOf( size, 42.0f )
		);
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
	public float[] valueFromInteger(int integer) {
		return arrayOf( size, integer );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<float[]>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				arrayOf( size, (byte) 0 ), arrayOf( size, (byte) 42 )
		) );
	}

	private static float[] arrayOf(int size, float value) {
		float[] floats = new float[size];
		Arrays.fill( floats, value );
		return floats;
	}
}
