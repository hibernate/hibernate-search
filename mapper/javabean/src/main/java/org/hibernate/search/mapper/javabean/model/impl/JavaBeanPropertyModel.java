/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.stream.Stream;

import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.impl.common.LoggerFactory;

class JavaBeanPropertyModel<T> implements PojoPropertyModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JavaBeanBootstrapIntrospector introspector;
	private final JavaBeanTypeModel<?> parentTypeModel;

	private final PropertyDescriptor descriptor;

	private PojoGenericTypeModel<T> typeModel;
	private PropertyHandle handle;

	JavaBeanPropertyModel(JavaBeanBootstrapIntrospector introspector, JavaBeanTypeModel<?> parentTypeModel,
			PropertyDescriptor descriptor) {
		this.introspector = introspector;
		this.parentTypeModel = parentTypeModel;
		this.descriptor = descriptor;
	}

	/**
	 * N.B.: equals and hashCode must be defined properly for
	 * {@link JavaBeanGenericContextHelper#getPropertyCacheKey(PojoPropertyModel)}
	 * to work properly.
	 */
	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		JavaBeanPropertyModel<?> that = (JavaBeanPropertyModel<?>) o;
		return Objects.equals( introspector, that.introspector ) &&
				Objects.equals( parentTypeModel, that.parentTypeModel ) &&
				Objects.equals( getHandle(), that.getHandle() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( introspector, parentTypeModel, handle );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + getName() + ", " + getGetterGenericReturnType().getTypeName() + "]";
	}

	@Override
	public String getName() {
		return descriptor.getName();
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
	/*
	 * The cast is safe as long as both type parameter T and getGetterGenericReturnType
	 * match the actual type for this property.
	 */
	@SuppressWarnings( "unchecked" )
	public PojoGenericTypeModel<T> getTypeModel() {
		if ( typeModel == null ) {
			try {
				typeModel = (PojoGenericTypeModel<T>) parentTypeModel.getRawTypeDeclaringContext()
						.createGenericTypeModel( getGetterGenericReturnType() );
			}
			catch (RuntimeException e) {
				log.errorRetrievingPropertyTypeModel( getName(), parentTypeModel, e );
			}
		}
		return typeModel;
	}

	@Override
	public PropertyHandle getHandle() {
		if ( handle == null ) {
			try {
				handle = introspector.createPropertyHandle( getName(), descriptor.getReadMethod() );
			}
			catch (IllegalAccessException | RuntimeException e) {
				log.errorRetrievingPropertyTypeModel( getName(), parentTypeModel, e );
			}
		}
		return handle;
	}

	Type getGetterGenericReturnType() {
		return descriptor.getReadMethod().getGenericReturnType();
	}
}
