/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.model.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.search.mapper.pojo.standalone.log.impl.Log;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.hcann.spi.PojoHCannOrmGenericContextHelper;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.ReflectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

/**
 * A very simple introspector for Pojo mapping in standalone mode (without Hibernate ORM).
 */
public class StandalonePojoBootstrapIntrospector extends AbstractPojoHCAnnBootstrapIntrospector implements PojoBootstrapIntrospector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static StandalonePojoBootstrapIntrospector create(MethodHandles.Lookup lookup) {
		ValueHandleFactory valueHandleFactory = ValueHandleFactory.usingMethodHandle( lookup );
		return new StandalonePojoBootstrapIntrospector( valueHandleFactory );
	}

	private final PojoHCannOrmGenericContextHelper genericContextHelper;

	private final Map<Class<?>, PojoRawTypeModel<?>> typeModelCache = new HashMap<>();

	private StandalonePojoBootstrapIntrospector(ValueHandleFactory valueHandleFactory) {
		super( new JavaReflectionManager(), valueHandleFactory );
		this.genericContextHelper = new PojoHCannOrmGenericContextHelper( this );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> StandalonePojoRawTypeModel<T> typeModel(Class<T> clazz) {
		if ( clazz.isPrimitive() ) {
			/*
			 * We'll never manipulate the primitive type, as we're using generics everywhere,
			 * so let's consider every occurrence of the primitive type as an occurrence of its wrapper type.
			 */
			clazz = (Class<T>) ReflectionHelper.getPrimitiveWrapperType( clazz );
		}
		return (StandalonePojoRawTypeModel<T>) typeModelCache.computeIfAbsent( clazz, this::createTypeModel );
	}

	@Override
	public PojoRawTypeModel<?> typeModel(String name) {
		throw log.namedTypesNotSupported( name );
	}

	@Override
	public ValueHandleFactory annotationValueReadHandleFactory() {
		return valueHandleFactory;
	}

	@Override
	protected <T> ValueCreateHandle<T> createValueCreateHandle(Constructor<T> constructor) throws IllegalAccessException {
		setAccessible( constructor );
		return valueHandleFactory.createForConstructor( constructor );
	}

	ValueReadHandle<?> createValueReadHandle(Member member) throws IllegalAccessException {
		if ( member instanceof Method ) {
			Method method = (Method) member;
			setAccessible( method );
			return valueHandleFactory.createForMethod( method );
		}
		else if ( member instanceof Field ) {
			Field field = (Field) member;
			setAccessible( field );
			return valueHandleFactory.createForField( field );
		}
		else {
			throw new AssertionFailure( "Unexpected type for a " + Member.class.getName() + ": " + member );
		}
	}

	private <T> PojoRawTypeModel<T> createTypeModel(Class<T> clazz) {
		PojoRawTypeIdentifier<T> typeIdentifier = PojoRawTypeIdentifier.of( clazz );
		try {
			return new StandalonePojoRawTypeModel<>(
					this, typeIdentifier,
					new RawTypeDeclaringContext<>( genericContextHelper, clazz )
			);
		}
		catch (RuntimeException e) {
			throw log.errorRetrievingTypeModel( clazz, e );
		}
	}

	private static void setAccessible(AccessibleObject member) {
		try {
			// always set accessible to true as it bypasses the security model checks
			// at execution time and is faster.
			member.setAccessible( true );
		}
		catch (SecurityException se) {
			if ( !Modifier.isPublic( ( (Member) member ).getModifiers() ) ) {
				throw se;
			}
		}
	}
}
