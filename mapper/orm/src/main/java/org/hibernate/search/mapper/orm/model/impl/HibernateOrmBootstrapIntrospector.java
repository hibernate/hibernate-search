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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.persistence.metamodel.ManagedType;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.orm.util.impl.HibernateOrmXClassOrdering;
import org.hibernate.search.mapper.pojo.model.spi.FieldPropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.MethodPropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.util.spi.AnnotationHelper;
import org.hibernate.search.util.common.impl.ReflectionHelper;
import org.hibernate.search.util.common.impl.StreamHelper;

/**
 * @author Yoann Rodiere
 */
public class HibernateOrmBootstrapIntrospector implements PojoBootstrapIntrospector {

	private final ReflectionManager reflectionManager;
	private final MethodHandles.Lookup lookup;
	private final AnnotationHelper annotationHelper;
	private final SessionFactoryImplementor sessionFactoryImplementor;
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
	 * @see HibernateOrmRawTypeModel#propertyModelCache
	 */
	private final Map<Class<?>, PojoRawTypeModel<?>> typeModelCache = new HashMap<>();

	@SuppressWarnings("deprecation") // There is no alternative to getReflectionManager() at the moment.
	public HibernateOrmBootstrapIntrospector(Metadata metadata, SessionFactoryImplementor sessionFactoryImplementor) {
		ReflectionManager metadataReflectionManager = null;
		if ( metadata instanceof MetadataImplementor ) {
			metadataReflectionManager = ((MetadataImplementor) metadata).getTypeConfiguration().getMetadataBuildingContext().getBootstrapContext().getReflectionManager();
		}
		if ( metadataReflectionManager != null ) {
			this.reflectionManager = metadataReflectionManager;
		}
		else {
			// Fall back to our own instance of JavaReflectionManager
			// when metadata is not a MetadataImplementor or
			// the reflection manager were not created by Hibernate yet.
			this.reflectionManager = new JavaReflectionManager();
		}
		// TODO get the user lookup from Hibernate ORM?
		this.lookup = MethodHandles.publicLookup();
		this.annotationHelper = new AnnotationHelper( lookup );
		this.sessionFactoryImplementor = sessionFactoryImplementor;
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

	PropertyHandle createPropertyHandle(String name, Member member) throws IllegalAccessException {
		if ( member instanceof Method ) {
			Method method = (Method) member;
			setAccessible( method );
			return new MethodPropertyHandle( name, method );
		}
		else if ( member instanceof Field ) {
			Field field = (Field) member;
			setAccessible( field );
			return new FieldPropertyHandle( name, field );
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
		ManagedType<T> managedType = getManagedType( type );
		return new HibernateOrmRawTypeModel<>(
				this, type, managedType,
				new RawTypeDeclaringContext<>( genericContextHelper, type )
		);
	}

	private <T> ManagedType<T> getManagedType(Class<T> type) {
		try {
			return sessionFactoryImplementor.getMetamodel().managedType( type );
		}
		catch (IllegalArgumentException ignored) {
			// The type is not managed in the current session factory
			return null;
		}
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
