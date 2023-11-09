/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;

public abstract class VectorFieldTypeDescriptor<F> extends AbstractFieldTypeDescriptor<F> {

	private static List<VectorFieldTypeDescriptor<?>> all;

	public static List<VectorFieldTypeDescriptor<?>> getAll() {
		if ( all == null ) {
			List<VectorFieldTypeDescriptor<?>> list = new ArrayList<>();
			Collections.addAll(
					list,
					ByteVectorFieldTypeDescriptor.INSTANCE,
					FloatVectorFieldTypeDescriptor.INSTANCE
			);
			all = Collections.unmodifiableList( list );
		}
		return all;
	}

	public static VectorFieldTypeDescriptor<?> getIncompatible(VectorFieldTypeDescriptor<?> typeDescriptor) {
		if ( ByteVectorFieldTypeDescriptor.INSTANCE.equals( typeDescriptor ) ) {
			return FloatVectorFieldTypeDescriptor.INSTANCE;
		}
		else {
			return ByteVectorFieldTypeDescriptor.INSTANCE;
		}
	}

	protected VectorFieldTypeDescriptor(Class<F> javaType) {
		super( javaType );
	}

	protected VectorFieldTypeDescriptor(Class<F> javaType, String uniqueName) {
		super( javaType, uniqueName );
	}

	@Override
	public abstract VectorFieldTypeOptionsStep<?, F> configure(IndexFieldTypeFactory fieldContext);

	public abstract int vectorSize();
}
