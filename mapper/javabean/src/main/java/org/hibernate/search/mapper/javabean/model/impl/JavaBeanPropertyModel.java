/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.stream.Stream;

import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class JavaBeanPropertyModel<T> implements PojoPropertyModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JavaBeanBootstrapIntrospector introspector;
	private final JavaBeanTypeModel<?> parentTypeModel;

	private final String propertyName;
	private final Method readMethod;

	private PojoGenericTypeModel<T> typeModel;
	private PropertyHandle<T> handle;

	JavaBeanPropertyModel(JavaBeanBootstrapIntrospector introspector, JavaBeanTypeModel<?> parentTypeModel, String propertyName, Method readMethod) {
		this.introspector = introspector;
		this.parentTypeModel = parentTypeModel;
		this.propertyName = propertyName;
		this.readMethod = readMethod;
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
		return propertyName;
	}

	@Override
	public <A extends Annotation> Stream<A> getAnnotationsByType(Class<A> annotationType) {
		return introspector.getAnnotationsByType( readMethod, annotationType );
	}

	@Override
	public Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(Class<? extends Annotation> metaAnnotationType) {
		return introspector.getAnnotationsByMetaAnnotationType( readMethod, metaAnnotationType );
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
				throw log.errorRetrievingPropertyTypeModel( getName(), parentTypeModel, e );
			}
		}
		return typeModel;
	}

	@Override
	@SuppressWarnings("unchecked") // By construction, we know the member returns values of type T
	public PropertyHandle<T> getHandle() {
		if ( handle == null ) {
			try {
				handle = (PropertyHandle<T>) introspector.createPropertyHandle( getName(), readMethod );
			}
			catch (IllegalAccessException | RuntimeException e) {
				throw log.errorRetrievingPropertyTypeModel( getName(), parentTypeModel, e );
			}
		}
		return handle;
	}

	Type getGetterGenericReturnType() {
		return readMethod.getGenericReturnType();
	}
}
