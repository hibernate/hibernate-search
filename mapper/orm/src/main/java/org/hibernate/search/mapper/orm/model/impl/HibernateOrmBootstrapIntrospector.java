/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmMapperSpiSettings;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmReflectionStrategyName;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.impl.ReflectionHelper;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory;

public class HibernateOrmBootstrapIntrospector extends AbstractPojoHCAnnBootstrapIntrospector implements PojoBootstrapIntrospector {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<HibernateOrmReflectionStrategyName> REFLECTION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSpiSettings.Radicals.REFLECTION_STRATEGY )
					.as( HibernateOrmReflectionStrategyName.class, HibernateOrmReflectionStrategyName::of )
					.withDefault( HibernateOrmMapperSpiSettings.Defaults.REFLECTION_STRATEGY )
					.build();

	public static HibernateOrmBootstrapIntrospector create(
			HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider,
			ReflectionManager ormReflectionManager,
			ConfigurationPropertySource propertySource) {
		MethodHandles.Lookup lookup = MethodHandles.publicLookup();

		HibernateOrmReflectionStrategyName reflectionStrategyName = REFLECTION_STRATEGY.get( propertySource );
		ValueReadHandleFactory valueReadHandleFactory;
		switch ( reflectionStrategyName ) {
			case JAVA_LANG_REFLECT:
				valueReadHandleFactory = ValueReadHandleFactory.usingJavaLangReflect();
				break;
			case METHOD_HANDLE:
				valueReadHandleFactory = ValueReadHandleFactory.usingMethodHandle( lookup );
				break;
			default:
				throw new AssertionFailure( "Unexpected reflection strategy name: " + reflectionStrategyName );
		}

		return new HibernateOrmBootstrapIntrospector(
				basicTypeMetadataProvider, ormReflectionManager, valueReadHandleFactory
		);
	}

	private final HibernateOrmBasicTypeMetadataProvider basicTypeMetadataProvider;
	private final ValueReadHandleFactory valueReadHandleFactory;
	private final HibernateOrmGenericContextHelper genericContextHelper;
	private final RawTypeDeclaringContext<?> missingRawTypeDeclaringContext;

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
			ValueReadHandleFactory valueReadHandleFactory) {
		super( reflectionManager );
		this.basicTypeMetadataProvider = basicTypeMetadataProvider;
		this.valueReadHandleFactory = valueReadHandleFactory;
		this.genericContextHelper = new HibernateOrmGenericContextHelper( this );
		this.missingRawTypeDeclaringContext = new RawTypeDeclaringContext<>(
				genericContextHelper, Object.class
		);
	}

	@Override
	public AbstractHibernateOrmRawTypeModel<?> getTypeModel(String name) {
		HibernateOrmBasicDynamicMapTypeMetadata dynamicMapTypeOrmMetadata =
				basicTypeMetadataProvider.getBasicDynamicMapTypeMetadata( name );
		if ( dynamicMapTypeOrmMetadata != null ) {
			// Dynamic-map entity *or component* type
			return dynamicMapTypeModelCache.computeIfAbsent( name, this::createDynamicMapTypeModel );
		}

		PojoRawTypeIdentifier<?> typeIdentifier = basicTypeMetadataProvider.getTypeIdentifierResolver()
				.resolveByHibernateOrmEntityName( name );
		if ( typeIdentifier != null ) {
			// Class entity type
			return getTypeModel( typeIdentifier.getJavaClass() );
		}

		Set<String> typeNames = new LinkedHashSet<>( basicTypeMetadataProvider.getKnownDynamicMapTypeNames() );
		typeNames.addAll( basicTypeMetadataProvider.getTypeIdentifierResolver().getKnownHibernateOrmEntityNames() );
		throw log.unknownNamedType( name, typeNames );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> HibernateOrmClassRawTypeModel<T> getTypeModel(Class<T> clazz) {
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
	public <T> PojoGenericTypeModel<T> getGenericTypeModel(Class<T> clazz) {
		return missingRawTypeDeclaringContext.createGenericTypeModel( clazz );
	}

	@Override
	public ValueReadHandleFactory getAnnotationValueReadHandleFactory() {
		return valueReadHandleFactory;
	}

	<T> Stream<HibernateOrmClassRawTypeModel<? super T>> getAscendingSuperTypes(XClass xClass) {
		return getAscendingSuperClasses( xClass ).map( this::getTypeModel );
	}

	<T> Stream<HibernateOrmClassRawTypeModel<? super T>> getDescendingSuperTypes(XClass xClass) {
		return getDescendingSuperClasses( xClass ).map( this::getTypeModel );
	}

	ValueReadHandle<?> createValueReadHandle(Member member,
			HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata) throws IllegalAccessException {
		if ( member instanceof Method ) {
			Method method = (Method) member;
			setAccessible( method );
			return valueReadHandleFactory.createForMethod( method );
		}
		else if ( member instanceof Field ) {
			Field field = (Field) member;
			if ( ormPropertyMetadata != null && !ormPropertyMetadata.isId() ) {
				Method bytecodeEnhancerReaderMethod = getBytecodeEnhancerReaderMethod( field );
				if ( bytecodeEnhancerReaderMethod != null ) {
					setAccessible( bytecodeEnhancerReaderMethod );
					return valueReadHandleFactory.createForMethod( bytecodeEnhancerReaderMethod );
				}
			}

			setAccessible( field );
			return valueReadHandleFactory.createForField( field );
		}
		else {
			throw new AssertionFailure( "Unexpected type for a " + Member.class.getName() + ": " + member );
		}
	}

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
	 * @param field A member field from the Hibernate metamodel or from a XProperty.
	 * @return A method generated through bytecode enhancement that triggers lazy-loading before returning the member's value,
	 * or {@code null} if there is no such method.
	 */
	private static Method getBytecodeEnhancerReaderMethod(Field field) {
		Class<?> declaringClass = field.getDeclaringClass();

		if ( !PersistentAttributeInterceptable.class.isAssignableFrom( declaringClass ) ) {
			// The declaring class is not enhanced, the only way to access the field is to read it directly.
			return null;
		}

		/*
		 * The declaring class is enhanced.
		 * Use the "magic" methods that trigger lazy loading instead of accessing the field directly.
		 */
		try {
			return declaringClass.getDeclaredMethod( EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + field.getName() );
		}
		catch (NoSuchMethodException e) {
			throw new AssertionFailure( "Read method for enhanced field " + field + " is unexpectedly missing.", e );
		}
	}
}
