/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.lang.reflect.Member;
import java.util.List;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnPropertyModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

class JavaBeanPropertyModel<T> extends AbstractPojoHCAnnPropertyModel<T, JavaBeanBootstrapIntrospector> {

	JavaBeanPropertyModel(JavaBeanBootstrapIntrospector introspector,
			JavaBeanRawTypeModel<?> holderTypeModel,
			String name, List<XProperty> declaredXProperties,
			Member member) {
		super( introspector, holderTypeModel, name, declaredXProperties, member );
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, we know the member returns values of type T
	protected ValueReadHandle<T> createHandle() throws IllegalAccessException {
		return (ValueReadHandle<T>) introspector.createValueReadHandle( member );
	}
}
