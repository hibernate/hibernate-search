/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.model.spi.MemberPropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.util.SearchException;

class JavaBeanPropertyModel<T> implements PropertyModel<T> {

	private final JavaBeanIntrospector introspector;
	private final TypeModel<?> parentTypeModel;

	private final Class<T> type;
	private final PropertyDescriptor descriptor;
	private PropertyHandle handle;

	private TypeModel<T> typeModel;

	JavaBeanPropertyModel(JavaBeanIntrospector introspector, TypeModel<?> parentTypeModel,
			Class<T> type, PropertyDescriptor descriptor) {
		this.introspector = introspector;
		this.parentTypeModel = parentTypeModel;
		this.type = type;
		this.descriptor = descriptor;
	}

	@Override
	public String getName() {
		return descriptor.getName();
	}

	@Override
	public Class<T> getJavaType() {
		return type;
	}

	@Override
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return introspector.getAnnotationsByType( descriptor.getReadMethod(), annotationType );
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType) {
		return introspector.getAnnotationsByMetaAnnotationType( descriptor.getReadMethod(), metaAnnotationType );
	}

	@Override
	public TypeModel<T> getTypeModel() {
		if ( typeModel == null ) {
			try {
				typeModel = new JavaBeanTypeModel<>( introspector, type );
			}
			catch (IntrospectionException | RuntimeException e) {
				throw new SearchException( "Exception while retrieving property type model for '"
						+ getName() + "' on '" + parentTypeModel.getJavaType() + "'", e );
			}
		}
		return typeModel;
	}

	@Override
	public PropertyHandle getHandle() {
		if ( handle == null ) {
			try {
				handle = new MemberPropertyHandle( getName(), descriptor.getReadMethod() );
			}
			catch (IllegalAccessException | RuntimeException e) {
				throw new SearchException( "Exception while retrieving property handle for '"
						+ getName() + "' on '" + parentTypeModel.getJavaType() + "'", e );
			}
		}
		return handle;
	}

}
