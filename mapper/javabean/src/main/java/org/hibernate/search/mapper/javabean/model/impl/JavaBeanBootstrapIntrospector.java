/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.mapper.javabean.log.impl.Log;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.impl.ReflectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory;

/**
 * A very simple introspector roughly following Java Beans conventions.
 * <p>
 * As per JavaBeans conventions, only public getters are supported, and field access is not.
 *
 */
public class JavaBeanBootstrapIntrospector extends AbstractPojoHCAnnBootstrapIntrospector implements PojoBootstrapIntrospector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static JavaBeanBootstrapIntrospector create(MethodHandles.Lookup lookup) {
		ValueReadHandleFactory valueReadHandleFactory = ValueReadHandleFactory.usingMethodHandle( lookup );
		return new JavaBeanBootstrapIntrospector( valueReadHandleFactory );
	}

	private final ValueReadHandleFactory valueReadHandleFactory;
	private final JavaBeanGenericContextHelper genericContextHelper;
	private final RawTypeDeclaringContext<?> missingRawTypeDeclaringContext;

	private final Map<Class<?>, PojoRawTypeModel<?>> typeModelCache = new HashMap<>();

	private JavaBeanBootstrapIntrospector(ValueReadHandleFactory valueReadHandleFactory) {
		super( new JavaReflectionManager() );
		this.valueReadHandleFactory = valueReadHandleFactory;
		this.genericContextHelper = new JavaBeanGenericContextHelper( this );
		this.missingRawTypeDeclaringContext = new RawTypeDeclaringContext<>(
				genericContextHelper, Object.class
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> JavaBeanTypeModel<T> getTypeModel(Class<T> clazz) {
		if ( clazz.isPrimitive() ) {
			/*
			 * We'll never manipulate the primitive type, as we're using generics everywhere,
			 * so let's consider every occurrence of the primitive type as an occurrence of its wrapper type.
			 */
			clazz = (Class<T>) ReflectionHelper.getPrimitiveWrapperType( clazz );
		}
		return (JavaBeanTypeModel<T>) typeModelCache.computeIfAbsent( clazz, this::createTypeModel );
	}

	@Override
	public PojoRawTypeModel<?> getTypeModel(String name) {
		throw log.namedTypesNotSupported( name );
	}

	@Override
	public <T> PojoGenericTypeModel<T> getGenericTypeModel(Class<T> clazz) {
		return missingRawTypeDeclaringContext.createGenericTypeModel( clazz );
	}

	@Override
	public ValueReadHandleFactory getAnnotationValueReadHandleFactory() {
		return valueReadHandleFactory;
	}

	Stream<? extends JavaBeanTypeModel<?>> getAscendingSuperTypes(XClass xClass) {
		return getAscendingSuperClasses( xClass ).map( this::getTypeModel );
	}

	Stream<? extends JavaBeanTypeModel<?>> getDescendingSuperTypes(XClass xClass) {
		return getDescendingSuperClasses( xClass ).map( this::getTypeModel );
	}

	ValueReadHandle<?> createValueReadHandle(Method method) throws IllegalAccessException {
		return valueReadHandleFactory.createForMethod( method );
	}

	private <T> PojoRawTypeModel<T> createTypeModel(Class<T> clazz) {
		PojoRawTypeIdentifier<T> typeIdentifier = PojoRawTypeIdentifier.of( clazz );
		try {
			return new JavaBeanTypeModel<>(
					this, typeIdentifier,
					new RawTypeDeclaringContext<>( genericContextHelper, clazz )
			);
		}
		catch (RuntimeException e) {
			throw log.errorRetrievingTypeModel( clazz, e );
		}
	}
}
