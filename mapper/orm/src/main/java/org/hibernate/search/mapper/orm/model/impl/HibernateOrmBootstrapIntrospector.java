/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmMapperSpiSettings;
import org.hibernate.search.mapper.orm.cfg.spi.HibernateOrmPropertyHandleFactoryName;
import org.hibernate.search.mapper.orm.util.impl.HibernateOrmXClassOrdering;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandleFactory;
import org.hibernate.search.mapper.pojo.util.spi.AnnotationHelper;
import org.hibernate.search.util.common.impl.ReflectionHelper;
import org.hibernate.search.util.common.impl.StreamHelper;

public class HibernateOrmBootstrapIntrospector implements PojoBootstrapIntrospector {

	private static final ConfigurationProperty<HibernateOrmPropertyHandleFactoryName> PROPERTY_HANDLE_FACTORY =
			ConfigurationProperty.forKey( HibernateOrmMapperSpiSettings.Radicals.PROPERTY_HANDLE_FACTORY )
					.as( HibernateOrmPropertyHandleFactoryName.class, HibernateOrmPropertyHandleFactoryName::of )
					.withDefault( HibernateOrmMapperSpiSettings.Defaults.PROPERTY_HANDLE_FACTORY )
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

		// TODO get the user lookup from Hibernate ORM?
		MethodHandles.Lookup lookup = MethodHandles.publicLookup();
		AnnotationHelper annotationHelper = new AnnotationHelper( lookup );

		HibernateOrmPropertyHandleFactoryName propertyHandleFactoryName = PROPERTY_HANDLE_FACTORY.get( propertySource );
		PropertyHandleFactory propertyHandleFactory;
		switch ( propertyHandleFactoryName ) {
			case JAVA_LANG_REFLECT:
				propertyHandleFactory = PropertyHandleFactory.usingJavaLangReflect();
				break;
			case METHOD_HANDLE:
				propertyHandleFactory = PropertyHandleFactory.usingMethodHandle( lookup );
				break;
			default:
				throw new AssertionFailure( "Unexpected property handle factory name: " + propertyHandleFactoryName );
		}

		return new HibernateOrmBootstrapIntrospector(
				typeMetadata, ormReflectionManager, annotationHelper, propertyHandleFactory
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
	private final ReflectionManager reflectionManager;
	private final PropertyHandleFactory propertyHandleFactory;
	private final AnnotationHelper annotationHelper;
	private final HibernateOrmGenericContextHelper genericContextHelper;
	private final RawTypeDeclaringContext<?> missingRawTypeDeclaringContext;

	/**
	 * Note: the main purpose of this cache is not to improve performance,
	 * but to ensure the unicity of the returned {@link PojoTypeModel}s.
	 * so as to ensure the unicity of {@link PojoPropertyModel}s,
	 * which lowers the risk of generating duplicate {@link org.hibernate.search.mapper.pojo.model.spi.PropertyHandle}s.
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
			PropertyHandleFactory propertyHandleFactory) {
		this.typeMetadata = typeMetadata;
		this.reflectionManager = reflectionManager;
		this.propertyHandleFactory = propertyHandleFactory;
		this.annotationHelper = annotationHelper;
		this.genericContextHelper = new HibernateOrmGenericContextHelper( this );
		this.missingRawTypeDeclaringContext = new RawTypeDeclaringContext<>(
				genericContextHelper, Object.class
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> PojoRawTypeModel<T> getTypeModel(Class<T> clazz) {
		if ( clazz.isPrimitive() ) {
			/*
			 * We'll never manipulate the primitive type, as we're using generics everywhere,
			 * so let's consider every occurrence of the primitive type as an occurrence of its wrapper type.
			 */
			clazz = (Class<T>) ReflectionHelper.getPrimitiveWrapperType( clazz );
		}
		return (PojoRawTypeModel<T>) typeModelCache.computeIfAbsent( clazz, this::createTypeModel );
	}

	@Override
	public <T> PojoGenericTypeModel<T> getGenericTypeModel(Class<T> clazz) {
		return missingRawTypeDeclaringContext.createGenericTypeModel( clazz );
	}

	@SuppressWarnings( "unchecked" )
	<T> Stream<HibernateOrmRawTypeModel<? super T>> getAscendingSuperTypes(XClass xClass) {
		return HibernateOrmXClassOrdering.get().getAscendingSuperTypes( xClass )
				.map( superType -> (HibernateOrmRawTypeModel<? super T>) getTypeModel( superType ) );
	}

	@SuppressWarnings( "unchecked" )
	<T> Stream<HibernateOrmRawTypeModel<? super T>> getDescendingSuperTypes(XClass xClass) {
		return HibernateOrmXClassOrdering.get().getDescendingSuperTypes( xClass )
				.map( superType -> (HibernateOrmRawTypeModel<? super T>) getTypeModel( superType ) );
	}

	XClass toXClass(Class<?> type) {
		return reflectionManager.toXClass( type );
	}

	Map<String, XProperty> getDeclaredFieldAccessXPropertiesByName(XClass xClass) {
		// TODO HSEARCH-3056 remove lambdas if possible
		return xClass.getDeclaredProperties( XClass.ACCESS_FIELD ).stream()
				.collect( xPropertiesByNameNoDuplicate() );
	}

	Map<String, XProperty> getDeclaredMethodAccessXPropertiesByName(XClass xClass) {
		// TODO HSEARCH-3056 remove lambdas if possible
		return xClass.getDeclaredProperties( XClass.ACCESS_PROPERTY ).stream()
				.collect( xPropertiesByNameNoDuplicate() );
	}

	<A extends Annotation> Optional<A> getAnnotationByType(XAnnotatedElement xAnnotated, Class<A> annotationType) {
		return Optional.ofNullable( xAnnotated.getAnnotation( annotationType ) );
	}

	<A extends Annotation> Stream<A> getAnnotationsByType(XAnnotatedElement xAnnotated, Class<A> annotationType) {
		return Arrays.stream( xAnnotated.getAnnotations() )
				.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
				.filter( annotation -> annotationType.isAssignableFrom( annotation.annotationType() ) )
				.map( annotationType::cast );
	}

	Stream<? extends Annotation> getAnnotationsByMetaAnnotationType(
			XAnnotatedElement xAnnotated, Class<? extends Annotation> metaAnnotationType) {
		return Arrays.stream( xAnnotated.getAnnotations() )
				.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
				.filter( annotation -> annotationHelper.isMetaAnnotated( annotation, metaAnnotationType ) );
	}

	PropertyHandle<?> createPropertyHandle(String name, Member member) throws IllegalAccessException {
		if ( member instanceof Method ) {
			Method method = (Method) member;
			setAccessible( method );
			return propertyHandleFactory.createForMethod( name, method );
		}
		else if ( member instanceof Field ) {
			Field field = (Field) member;
			setAccessible( field );
			return propertyHandleFactory.createForField( name, field );
		}
		else {
			throw new AssertionFailure( "Unexpected type for a " + Member.class.getName() + ": " + member );
		}
	}

	@SuppressWarnings( "unchecked" )
	private PojoTypeModel<?> getTypeModel(XClass xClass) {
		return getTypeModel( reflectionManager.toClass( xClass ) );
	}

	private <T> PojoRawTypeModel<T> createTypeModel(Class<T> type) {
		return new HibernateOrmRawTypeModel<>(
				this, type, typeMetadata.get( type ),
				new RawTypeDeclaringContext<>( genericContextHelper, type )
		);
	}

	private Collector<XProperty, ?, Map<String, XProperty>> xPropertiesByNameNoDuplicate() {
		return StreamHelper.toMap(
				XProperty::getName, Function.identity(),
				TreeMap::new // Sort properties by name for deterministic iteration
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
}
