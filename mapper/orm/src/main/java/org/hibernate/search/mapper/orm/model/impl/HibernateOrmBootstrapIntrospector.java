/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.hcann.spi.PojoHCannOrmGenericContextHelper;
import org.hibernate.search.mapper.pojo.model.spi.AbstractPojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.impl.ReflectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reflect.spi.ValueCreateHandle;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;

public class HibernateOrmBootstrapIntrospector extends AbstractPojoHCAnnBootstrapIntrospector
		implements PojoBootstrapIntrospector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static HibernateOrmBootstrapIntrospector create(
			HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider,
			ReflectionManager ormReflectionManager,
			ValueHandleFactory valueHandleFactory) {
		return new HibernateOrmBootstrapIntrospector(
				basicTypeMetadataProvider, ormReflectionManager, valueHandleFactory
		);
	}

	private final HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider;
	private final PojoHCannOrmGenericContextHelper genericContextHelper;

	/*
	 * Note: the main purpose of these caches is not to improve performance,
	 * but to ensure the unicity of the returned PojoTypeModels.
	 * so as to ensure the unicity of PojoPropertyModels,
	 * which lowers the risk of generating duplicate ValueReadHandles.
	 *
	 * Also, this cache allows to not care at all about implementing equals and hashcode,
	 * since type models are presumably instantiated only once per type.
	 */
	private final Map<Class<?>, HibernateOrmClassRawTypeModel<?>> classTypeModelCache = new HashMap<>();
	private final Map<String, HibernateOrmDynamicMapRawTypeModel> dynamicMapTypeModelCache = new HashMap<>();

	private HibernateOrmBootstrapIntrospector(
			HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider,
			ReflectionManager reflectionManager,
			ValueHandleFactory valueHandleFactory) {
		super( reflectionManager, valueHandleFactory );
		this.basicTypeMetadataProvider = basicTypeMetadataProvider;
		this.genericContextHelper = new PojoHCannOrmGenericContextHelper( this );
	}

	@Override
	public AbstractPojoRawTypeModel<?, ?> typeModel(String name) {
		HibernateOrmBasicDynamicMapTypeMetadata dynamicMapTypeOrmMetadata =
				basicTypeMetadataProvider.getBasicDynamicMapTypeMetadata( name );
		if ( dynamicMapTypeOrmMetadata != null ) {
			// Dynamic-map entity *or component* type
			return dynamicMapTypeModelCache.computeIfAbsent( name, this::createDynamicMapTypeModel );
		}

		PojoRawTypeIdentifier<?> typeIdentifier = basicTypeMetadataProvider.getTypeIdentifierResolver()
				.resolveByJpaOrHibernateOrmEntityName( name );
		if ( typeIdentifier != null ) {
			// Class entity type
			return typeModel( typeIdentifier.javaClass() );
		}

		Set<String> typeNames = new LinkedHashSet<>( basicTypeMetadataProvider.getKnownDynamicMapTypeNames() );
		typeNames.addAll( basicTypeMetadataProvider.getTypeIdentifierResolver().allKnownJpaOrHibernateOrmEntityNames() );
		throw log.unknownNamedType( name, typeNames );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> HibernateOrmClassRawTypeModel<T> typeModel(Class<T> clazz) {
		if ( clazz.isPrimitive() ) {
			/*
			 * We'll never manipulate the primitive type, as we're using generics everywhere,
			 * so let's consider every occurrence of the primitive type as an occurrence of its wrapper type.
			 */
			clazz = (Class<T>) ReflectionHelper.getPrimitiveWrapperType( clazz );
		}
		return (HibernateOrmClassRawTypeModel<T>) classTypeModelCache.computeIfAbsent( clazz, this::createClassTypeModel );
	}

	@Override
	protected <T> ValueCreateHandle<T> createValueCreateHandle(Constructor<T> constructor) throws IllegalAccessException {
		setAccessible( constructor );
		return valueHandleFactory.createForConstructor( constructor );
	}

	ValueReadHandle<?> createValueReadHandle(Class<?> holderClass, Member member,
			HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata)
			throws IllegalAccessException {
		if ( member instanceof Method ) {
			Method method = (Method) member;
			setAccessible( method );
			return valueHandleFactory.createForMethod( method );
		}
		else if ( member instanceof Field ) {
			Field field = (Field) member;
			if ( ormPropertyMetadata != null && !ormPropertyMetadata.isId() ) {
				Method bytecodeEnhancerReaderMethod = getBytecodeEnhancerReaderMethod( holderClass, field );
				if ( bytecodeEnhancerReaderMethod != null ) {
					setAccessible( bytecodeEnhancerReaderMethod );
					return valueHandleFactory.createForMethod( bytecodeEnhancerReaderMethod );
				}
			}

			setAccessible( field );
			return valueHandleFactory.createForField( field );
		}
		else {
			throw new AssertionFailure( "Unexpected type for a " + Member.class.getName() + ": " + member );
		}
	}

	@SuppressWarnings("rawtypes")
	private HibernateOrmDynamicMapRawTypeModel createDynamicMapTypeModel(String name) {
		HibernateOrmBasicDynamicMapTypeMetadata ormMetadata = basicTypeMetadataProvider.getBasicDynamicMapTypeMetadata( name );
		PojoRawTypeIdentifier<Map> typeIdentifier =
				HibernateOrmRawTypeIdentifierResolver.createDynamicMapTypeIdentifier( name );
		return new HibernateOrmDynamicMapRawTypeModel(
				this, typeIdentifier, ormMetadata
		);
	}

	private <T> HibernateOrmClassRawTypeModel<T> createClassTypeModel(Class<T> type) {
		HibernateOrmBasicClassTypeMetadata ormMetadataOrNull =
				basicTypeMetadataProvider.getBasicClassTypeMetadata( type );
		PojoRawTypeIdentifier<T> typeIdentifier =
				HibernateOrmRawTypeIdentifierResolver.createClassTypeIdentifier( type );
		return new HibernateOrmClassRawTypeModel<>(
				this, typeIdentifier, ormMetadataOrNull,
				new RawTypeDeclaringContext<>( genericContextHelper, type )
		);
	}

	private static void setAccessible(AccessibleObject member) {
		try {
			// always set accessible to true as it bypass the security model checks
			// at execution time and is faster.
			member.setAccessible( true );
		}
		catch (SecurityException se) {
			if ( !Modifier.isPublic( ( (Member) member ).getModifiers() ) ) {
				throw se;
			}
		}
	}

	/**
	 * @param holderClass A class exposing the given field.
	 * @param field A member field from the Hibernate metamodel or from a XProperty.
	 * @return A method generated through bytecode enhancement that triggers lazy-loading before returning the member's value,
	 * or {@code null} if there is no such method.
	 */
	private static Method getBytecodeEnhancerReaderMethod(Class<?> holderClass, Field field) {
		if ( !PersistentAttributeInterceptable.class.isAssignableFrom( holderClass ) ) {
			// The declaring class is not enhanced, the only way to access the field is to read it directly.
			return null;
		}

		/*
		 * The class is enhanced.
		 * Use the "magic" methods that trigger lazy loading instead of accessing the field directly.
		 */
		try {
			return holderClass.getMethod( EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + field.getName() );
		}
		catch (NoSuchMethodException e) {
			throw new AssertionFailure( "Read method for enhanced field " + field + " is unexpectedly missing.", e );
		}
	}
}
