/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.reflect.spi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hibernate.search.util.common.reflect.impl.FieldValueReadHandle;
import org.hibernate.search.util.common.reflect.impl.MethodValueReadHandle;

final class MemberValueReadHandleFactory implements ValueReadHandleFactory {
	@Override
	public ValueReadHandle<?> createForField(Field field) {
		return new FieldValueReadHandle( field );
	}

	@Override
	public ValueReadHandle<?> createForMethod(Method method) {
		return new MethodValueReadHandle( method );
	}
}
