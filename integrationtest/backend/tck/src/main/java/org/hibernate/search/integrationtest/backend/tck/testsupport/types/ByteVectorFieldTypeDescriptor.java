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

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;

public class ByteVectorFieldTypeDescriptor extends VectorFieldTypeDescriptor<byte[]> {

	public static final ByteVectorFieldTypeDescriptor INSTANCE = new ByteVectorFieldTypeDescriptor();

	public static final int size = 4;

	private ByteVectorFieldTypeDescriptor() {
		super( byte[].class, "byte_vector" );
	}

	@Override
	public VectorFieldTypeOptionsStep<?, byte[]> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.asByteVector().dimension( size ).projectable( Projectable.YES );
	}

	@Override
	public int vectorSize() {
		return size;
	}

	@Override
	protected List<byte[]> createUniquelyMatchableValues() {
		return Arrays.asList(
				arrayOf( size, Byte.MIN_VALUE ),
				arrayOf( size, (byte) -42 ),
				arrayOf( size, (byte) -1 ),
				arrayOf( size, (byte) 0 ),
				arrayOf( size, (byte) 1 ),
				arrayOf( size, (byte) 3 ),
				arrayOf( size, (byte) 42 ),
				arrayOf( size, Byte.MAX_VALUE )
		);
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
		Arrays.fill( bytes, value );
		return bytes;
	}
}
