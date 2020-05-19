/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.lang.reflect.Type;

import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public class JavaBeanGenericContextHelper implements GenericContextAwarePojoGenericTypeModel.Helper {
	private final JavaBeanBootstrapIntrospector introspector;

	public JavaBeanGenericContextHelper(JavaBeanBootstrapIntrospector introspector) {
		this.introspector = introspector;
	}

	@Override
	public <T> PojoRawTypeModel<T> rawTypeModel(Class<T> clazz) {
		return introspector.typeModel( clazz );
	}

	@Override
	public Object propertyCacheKey(PojoPropertyModel<?> rawPropertyModel) {
		return rawPropertyModel; // Properties define equals and hashCode as required
	}

	@Override
	public Type propertyGenericType(PojoPropertyModel<?> rawPropertyModel) {
		JavaBeanPropertyModel<?> javaBeanPropertyModel = (JavaBeanPropertyModel<?>) rawPropertyModel;
		return javaBeanPropertyModel.getGetterGenericReturnType();
	}
}
