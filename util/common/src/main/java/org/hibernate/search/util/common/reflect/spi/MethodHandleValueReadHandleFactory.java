/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reflect.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hibernate.search.util.common.reflect.impl.MethodHandleValueReadHandle;

final class MethodHandleValueReadHandleFactory implements ValueReadHandleFactory {

	private final MethodHandles.Lookup lookup;

	MethodHandleValueReadHandleFactory(MethodHandles.Lookup lookup) {
		this.lookup = lookup;
	}

	@Override
	public ValueReadHandle<?> createForField(Field field) throws IllegalAccessException {
		return new MethodHandleValueReadHandle( field, lookup.unreflectGetter( field ) );
	}

	@Override
	public ValueReadHandle<?> createForMethod(Method method) throws IllegalAccessException {
		return new MethodHandleValueReadHandle( method, lookup.unreflect( method ) );
	}
}
