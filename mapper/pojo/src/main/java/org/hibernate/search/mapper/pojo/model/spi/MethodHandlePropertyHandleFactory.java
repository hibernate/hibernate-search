/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class MethodHandlePropertyHandleFactory implements PropertyHandleFactory {

	private final MethodHandles.Lookup lookup;

	MethodHandlePropertyHandleFactory(MethodHandles.Lookup lookup) {
		this.lookup = lookup;
	}

	@Override
	public PropertyHandle<?> createForField(Field field) throws IllegalAccessException {
		return new MethodHandlePropertyHandle( field, lookup.unreflectGetter( field ) );
	}

	@Override
	public PropertyHandle<?> createForMethod(Method method) throws IllegalAccessException {
		return new MethodHandlePropertyHandle( method, lookup.unreflect( method ) );
	}
}
