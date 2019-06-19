/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.spi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class MemberPropertyHandleFactory implements PropertyHandleFactory {
	@Override
	public PropertyHandle<?> createForField(Field field) {
		return new FieldPropertyHandle( field );
	}

	@Override
	public PropertyHandle<?> createForMethod(Method method) {
		return new MethodPropertyHandle( method );
	}
}
