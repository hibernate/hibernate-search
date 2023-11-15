/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;

public abstract class TypeAssertionHelper<F, T> {

	private TypeAssertionHelper() {
	}

	public abstract Class<T> getJavaClass();

	public abstract T create(F fieldValue);

	public static <F> TypeAssertionHelper<F, F> identity(FieldTypeDescriptor<F, ?> typeDescriptor) {
		return new TypeAssertionHelper<F, F>() {
			@Override
			public Class<F> getJavaClass() {
				return typeDescriptor.getJavaType();
			}

			@Override
			public F create(F fieldValue) {
				return fieldValue;
			}
		};
	}

	@SuppressWarnings("rawtypes")
	public static <F> TypeAssertionHelper<F, ValueWrapper> wrapper(FieldTypeDescriptor<F, ?> typeDescriptor) {
		return new TypeAssertionHelper<F, ValueWrapper>() {
			@Override
			public Class<ValueWrapper> getJavaClass() {
				return ValueWrapper.class;
			}

			@Override
			public ValueWrapper create(F fieldValue) {
				return new ValueWrapper<>( fieldValue );
			}
		};
	}

	public static <F, T> TypeAssertionHelper<F, T> nullType() {
		return new TypeAssertionHelper<F, T>() {
			@Override
			public Class<T> getJavaClass() {
				return null;
			}

			@Override
			public T create(F fieldValue) {
				return neverCalled( fieldValue );
			}
		};
	}

	public static <F, T> TypeAssertionHelper<F, T> wrongType(FieldTypeDescriptor<T, ?> wrongTypeDescriptor) {
		return new TypeAssertionHelper<F, T>() {
			@Override
			public Class<T> getJavaClass() {
				return wrongTypeDescriptor.getJavaType();
			}

			@Override
			public T create(F fieldValue) {
				return neverCalled( fieldValue );
			}
		};
	}

	private static <P1, R> R neverCalled(P1 param) {
		throw new IllegalStateException( "This should not be called; called with parameter " + param );
	}

}
