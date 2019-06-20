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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.Metadata;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmMapperSpiSettings;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmReflectionStrategyName;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandle;
import org.hibernate.search.util.common.reflect.spi.ValueReadHandleFactory;
import org.hibernate.search.mapper.pojo.util.spi.AnnotationHelper;
import org.hibernate.search.util.common.impl.ReflectionHelper;

public class HibernateOrmBootstrapIntrospector extends AbstractPojoHCAnnBootstrapIntrospector implements PojoBootstrapIntrospector {

	private static final ConfigurationProperty<HibernateOrmReflectionStrategyName> REFLECTION_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSpiSettings.Radicals.REFLECTION_STRATEGY )
					.as( HibernateOrmReflectionStrategyName.class, HibernateOrmReflectionStrategyName::of )
					.withDefault( HibernateOrmMapperSpiSettings.Defaults.REFLECTION_STRATEGY )
					.build();

	public static HibernateOrmBootstrapIntrospector create(Metadata metadata,
			ReflectionManager ormReflectionManager,
			ConfigurationPropertySource propertySource) {
		Collection<PersistentClass> persistentClasses = metadata.getEntityBindings();
		Map<Class<?>, HibernateOrmBasicTypeMetadata> typeMetadata = new HashMap<>();
		collectPersistentTypes( typeMetadata, metadata.getEntityBindings() );
		for ( PersistentClass persistentClass : persistentClasses ) {
			collectEmbeddedTypesRecursively( typeMetadata, persistentClass.getIdentifier() );
			collectEmbeddedTypesRecursively( typeMetadata, persistentClass.getPropertyIterator() );
		}

		MethodHandles.Lookup lookup = MethodHandles.publicLookup();
		AnnotationHelper annotationHelper = new AnnotationHelper( lookup );

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
				typeMetadata, ormReflectionManager, annotationHelper, valueReadHandleFactory
		);
	}

	private static void collectPersistentTypes(Map<Class<?>, HibernateOrmBasicTypeMetadata> collected, Collection<PersistentClass> persistentClasses) {
		for ( PersistentClass persistentClass : persistentClasses ) {
			collected.put( persistentClass.getMappedClass(), HibernateOrmBasicTypeMetadata.create( persistentClass ) );
		}
	}

	private static void collectEmbeddedTypesRecursively(Map<Class<?>, HibernateOrmBasicTypeMetadata> collected, Iterator<Property> propertyIterator) {
		while ( propertyIterator.hasNext() ) {
			Property property = propertyIterator.next();
			collectEmbeddedTypesRecursively( collected, property.getValue() );
		}
	}

	private static void collectEmbeddedTypesRecursively(Map<Class<?>, HibernateOrmBasicTypeMetadata> collected, Value value) {
		if ( value instanceof Component ) {
			Component component = (Component) value;
			// We don't care about duplicates, we assume they are all the same regarding the information we need
			collected.computeIfAbsent(
					component.getComponentClass(),
					ignored -> HibernateOrmBasicTypeMetadata.create( component )
			);
			// Recurse
			collectEmbeddedTypesRecursively( collected, component.getPropertyIterator() );
		}
		else if ( value instanceof org.hibernate.mapping.Collection ) {
			org.hibernate.mapping.Collection collection = (org.hibernate.mapping.Collection) value;
			// Recurse
			collectEmbeddedTypesRecursively( collected, collection.getElement() );
			if ( collection instanceof IndexedCollection ) {
				IndexedCollection indexedCollection = (IndexedCollection) collection;
				/*
				 * Do not let ORM confuse you: getKey() doesn't return the value of the map key,
				 * but the value of the foreign key to the targeted entity...
				 * We need to call getIndex() to retrieve the value of the map key.
				 */
				collectEmbeddedTypesRecursively( collected, indexedCollection.getIndex() );
			}
		}
	}

	private final Map<Class<?>, HibernateOrmBasicTypeMetadata> typeMetadata;
	private final ValueReadHandleFactory valueReadHandleFactory;
	private final HibernateOrmGenericContextHelper genericContextHelper;
	private final RawTypeDeclaringContext<?> missingRawTypeDeclaringContext;

	/**
	 * Note: the main purpose of this cache is not to improve performance,
	 * but to ensure the unicity of the returned {@link PojoTypeModel}s.
	 * so as to ensure the unicity of {@link PojoPropertyModel}s,
	 * which lowers the risk of generating duplicate {@link ValueReadHandle}s.
	 * <p>
	 * Also, this cache allows to not care at all about implementing equals and hashcode,
	 * since type models are presumably instantiated only once per type.
	 *
	 * See also HibernateOrmRawTypeModel#propertyModelCache
	 */
	private final Map<Class<?>, PojoRawTypeModel<?>> typeModelCache = new HashMap<>();

	private HibernateOrmBootstrapIntrospector(
			Map<Class<?>, HibernateOrmBasicTypeMetadata> typeMetadata,
			ReflectionManager reflectionManager,
			AnnotationHelper annotationHelper,
			ValueReadHandleFactory valueReadHandleFactory) {
		super( reflectionManager, annotationHelper );
		this.typeMetadata = typeMetadata;
		this.valueReadHandleFactory = valueReadHandleFactory;
		this.genericContextHelper = new HibernateOrmGenericContextHelper( this );
		this.missingRawTypeDeclaringContext = new RawTypeDeclaringContext<>(
				genericContextHelper, Object.class
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> HibernateOrmRawTypeModel<T> getTypeModel(Class<T> clazz) {
		if ( clazz.isPrimitive() ) {
			/*
			 * We'll never manipulate the primitive type, as we're using generics everywhere,
			 * so let's consider every occurrence of the primitive type as an occurrence of its wrapper type.
			 */
			clazz = (Class<T>) ReflectionHelper.getPrimitiveWrapperType( clazz );
		}
		return (HibernateOrmRawTypeModel<T>) typeModelCache.computeIfAbsent( clazz, this::createTypeModel );
	}

	@Override
	public <T> PojoGenericTypeModel<T> getGenericTypeModel(Class<T> clazz) {
		return missingRawTypeDeclaringContext.createGenericTypeModel( clazz );
	}

	<T> Stream<HibernateOrmRawTypeModel<? super T>> getAscendingSuperTypes(XClass xClass) {
		return getAscendingSuperClasses( xClass ).map( this::getTypeModel );
	}

	<T> Stream<HibernateOrmRawTypeModel<? super T>> getDescendingSuperTypes(XClass xClass) {
		return getDescendingSuperClasses( xClass ).map( this::getTypeModel );
	}

	ValueReadHandle<?> createValueReadHandle(Member member,
			HibernateOrmBasicPropertyMetadata ormPropertyMetadata) throws IllegalAccessException {
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

	private <T> PojoRawTypeModel<T> createTypeModel(Class<T> type) {
		return new HibernateOrmRawTypeModel<>(
				this, type, typeMetadata.get( type ),
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
