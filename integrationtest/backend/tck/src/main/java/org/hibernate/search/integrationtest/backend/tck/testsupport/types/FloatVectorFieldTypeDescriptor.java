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

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;

public class FloatVectorFieldTypeDescriptor extends VectorFieldTypeDescriptor<float[]> {

	public static final FloatVectorFieldTypeDescriptor INSTANCE = new FloatVectorFieldTypeDescriptor();

	public static final int size = 4;

	private FloatVectorFieldTypeDescriptor() {
		super( float[].class, "float_vector" );
	}

	@Override
	public VectorFieldTypeOptionsStep<?, float[]> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.asFloatVector().dimension( size );
	}

	@Override
	public int vectorSize() {
		return size;
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
