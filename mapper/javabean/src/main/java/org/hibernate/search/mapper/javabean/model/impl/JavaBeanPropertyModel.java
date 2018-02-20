/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.model.spi.MemberPropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PojoContainerTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.SearchException;

class JavaBeanPropertyModel<T> implements PojoPropertyModel<T> {

	private final JavaBeanIntrospector introspector;
	private final PojoTypeModel<?> parentTypeModel;

	private final Class<T> clazz;
	private final PropertyDescriptor descriptor;
	private PropertyHandle handle;

	private PojoTypeModel<T> typeModel;
	private Optional<PojoContainerTypeModel<?>> containerTypeModelOptional;

	JavaBeanPropertyModel(JavaBeanIntrospector introspector, PojoTypeModel<?> parentTypeModel,
			Class<T> clazz, PropertyDescriptor descriptor) {
		this.introspector = introspector;
		this.parentTypeModel = parentTypeModel;
		this.clazz = clazz;
		this.descriptor = descriptor;
	}

	@Override
	public String getName() {
		return descriptor.getName();
	}

	@Override
	public Class<T> getJavaClass() {
		return clazz;
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
	public PojoTypeModel<T> getTypeModel() {
		if ( typeModel == null ) {
			try {
				typeModel = introspector.getTypeModel( clazz );
			}
			catch (RuntimeException e) {
				throw new SearchException( "Exception while retrieving property type model for '"
						+ getName() + "' on '" + parentTypeModel + "'", e );
			}
		}
		return typeModel;
	}

	@Override
	public Optional<PojoContainerTypeModel<?>> getContainerTypeModel() {
		if ( containerTypeModelOptional == null ) {
			Type type = descriptor.getReadMethod().getGenericReturnType();
			if ( type instanceof ParameterizedType ) {
				ParameterizedType parameterizedType = (ParameterizedType) type;
				Class<?> rawType = (Class<?>) ( (ParameterizedType) type ).getRawType();
				Type[] typeArguments = parameterizedType.getActualTypeArguments();
				PojoTypeModel<?> elementTypeModel = null;
				// TODO clean this up... maybe use Hibernate Commons Annotations instead of javax.beans?
				if ( Map.class.isAssignableFrom( rawType ) ) {
					elementTypeModel = introspector.getTypeModel( (Class<?>) typeArguments[1] );
				}
				else if ( Iterable.class.isAssignableFrom( rawType ) ) {
					elementTypeModel = introspector.getTypeModel( (Class<?>) typeArguments[0] );
				}
				if ( elementTypeModel != null ) {
					containerTypeModelOptional = Optional.of( new JavaBeanContainerTypeModel<>(
							rawType, elementTypeModel ) );
				}
			}
			if ( containerTypeModelOptional == null ) {
				containerTypeModelOptional = Optional.empty();
			}
		}
		return containerTypeModelOptional;
	}

	@Override
	public PropertyHandle getHandle() {
		if ( handle == null ) {
			try {
				handle = new MemberPropertyHandle( getName(), descriptor.getReadMethod() );
			}
			catch (IllegalAccessException | RuntimeException e) {
				throw new SearchException( "Exception while retrieving property handle for '"
						+ getName() + "' on '" + parentTypeModel + "'", e );
			}
		}
		return handle;
	}

}
