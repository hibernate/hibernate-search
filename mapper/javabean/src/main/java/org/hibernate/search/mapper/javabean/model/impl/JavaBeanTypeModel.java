/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.util.SearchException;

class JavaBeanTypeModel<T> implements TypeModel<T> {

	private final JavaBeanIntrospector introspector;
	private final Class<T> type;
	private final BeanInfo beanInfo;
	private final BeanInfo declaredBeanInfo;

	JavaBeanTypeModel(JavaBeanIntrospector introspector, Class<T> type) throws IntrospectionException {
		this.introspector = introspector;
		this.type = type;
		this.beanInfo = Introspector.getBeanInfo( type );
		this.declaredBeanInfo = Introspector.getBeanInfo( type, type.getSuperclass() );
	}

	@Override
	public Class<T> getJavaType() {
		return type;
	}

	@Override
	public <A extends Annotation> Optional<A> getAnnotationByType(Class<A> annotationType) {
		return introspector.getAnnotationByType( type, annotationType );
	}

	@Override
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return introspector.getAnnotationsByType( type, annotationType );
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType) {
		return introspector.getAnnotationsByMetaAnnotationType( type, metaAnnotationType );
	}

	@Override
	public PropertyModel<?> getProperty(String propertyName) {
		try {
			String normalizedName = Introspector.decapitalize( propertyName );
			PropertyDescriptor propertyDescriptor = getPropertyDescriptor( normalizedName );
			return createProperty( propertyDescriptor );
		}
		catch (RuntimeException e) {
			throw new SearchException( "Exception while retrieving property model for '"
					+ propertyName + "' on '" + getJavaType() + "'", e );
		}
	}

	@Override
	public Stream<PropertyModel<?>> getDeclaredProperties() {
		return Arrays.stream( declaredBeanInfo.getPropertyDescriptors() )
				.filter( descriptor -> descriptor.getReadMethod() != null )
				.map( this::createProperty );
	}

	private PropertyDescriptor getPropertyDescriptor(String normalizedName) {
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for ( PropertyDescriptor descriptor : propertyDescriptors ) {
			if ( normalizedName.equals( descriptor.getName() ) ) {
				return descriptor;
			}
		}
		throw new SearchException( "JavaBean property '" + normalizedName + "' not found in '"
				+ getJavaType() + "'" );
	}

	private PropertyModel<?> createProperty(PropertyDescriptor propertyDescriptor) {
		if ( propertyDescriptor.getReadMethod() == null ) {
			throw new SearchException( "Property '" + propertyDescriptor.getName() + "' on '" + getJavaType() + "' can't be read" );
		}
		return new JavaBeanPropertyModel<>(
				introspector, this, propertyDescriptor.getPropertyType(), propertyDescriptor
		);
	}
}
