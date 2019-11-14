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

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.pojo.model.hcann.spi.PojoCommonsAnnotationsHelper;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class JavaBeanPropertyModel<T> implements PojoPropertyModel<T> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final JavaBeanBootstrapIntrospector introspector;
	private final JavaBeanTypeModel<?> parentTypeModel;

	private final XProperty property;
	private final Method readMethod;

	private PojoGenericTypeModel<T> typeModel;
	private ValueReadHandle<T> handle;

	JavaBeanPropertyModel(JavaBeanBootstrapIntrospector introspector, JavaBeanTypeModel<?> parentTypeModel, XProperty property) {
		this.introspector = introspector;
		this.parentTypeModel = parentTypeModel;
		this.property = property;
		this.readMethod = PojoCommonsAnnotationsHelper.getUnderlyingMethod( property );
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
		return property.getName();
	}

	@Override
	public Stream<Annotation> getAnnotations() {
		return introspector.getAnnotations( property );
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
	public ValueReadHandle<T> getHandle() {
		if ( handle == null ) {
			try {
				handle = (ValueReadHandle<T>) introspector.createValueReadHandle( readMethod );
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
