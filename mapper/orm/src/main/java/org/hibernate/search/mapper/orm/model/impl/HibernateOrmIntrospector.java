/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.mapper.pojo.util.spi.AnnotationHelper;

/**
 * @author Yoann Rodiere
 */
public class HibernateOrmIntrospector implements PojoIntrospector {

	private final ReflectionManager reflectionManager;
	private final AnnotationHelper annotationHelper;
	private final SessionFactoryImplementor sessionFactoryImplementor;
	private final PropertyAccessStrategyResolver accessStrategyResolver;

	/**
	 * Note: the main purpose of this cache is not to improve performance,
	 * but to ensure the unicity of the returned {@link TypeModel}s,
	 * so as to ensure the unicity of {@link PropertyModel}s,
	 * which lowers the risk of generating duplicate {@link org.hibernate.search.mapper.pojo.model.spi.PropertyHandle}s.
	 *
	 * @see AbstractHibernateOrmTypeModel#propertyModelCache
	 */
	private final Map<Class<?>, TypeModel<?>> typeModelCache = new HashMap<>();

	public HibernateOrmIntrospector(Metadata metadata, SessionFactoryImplementor sessionFactoryImplementor) {
		ReflectionManager metadataReflectionManager = null;
		if ( metadata instanceof MetadataImplementor ) {
			metadataReflectionManager = ((MetadataImplementor) metadata).getMetadataBuildingOptions().getReflectionManager();
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
		this.annotationHelper = new AnnotationHelper( MethodHandles.publicLookup() );
		this.sessionFactoryImplementor = sessionFactoryImplementor;
		this.accessStrategyResolver = sessionFactoryImplementor.getServiceRegistry()
				.getService( PropertyAccessStrategyResolver.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> TypeModel<T> getTypeModel(Class<T> type) {
		return (TypeModel<T>) typeModelCache.computeIfAbsent( type, this::createTypeModel );
	}

	@Override
	// The actual class of a proxy of type T is always a Class<? extends T> (unless T is HibernateProxy, but we don't expect that)
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> getClass(T entity) {
		return Hibernate.getClass( entity );
	}

	XClass toXClass(Class<?> type) {
		return reflectionManager.toXClass( type );
	}

	Map<String, XProperty> getFieldAccessPropertiesByName(XClass xClass) {
		return xClass.getDeclaredProperties( XClass.ACCESS_FIELD ).stream()
				.collect( Collectors.toMap( XProperty::getName, Function.identity() ) );
	}

	Map<String, XProperty> getMethodAccessPropertiesByName(XClass xClass) {
		return xClass.getDeclaredProperties( XClass.ACCESS_PROPERTY ).stream()
				.collect( Collectors.toMap( XProperty::getName, Function.identity() ) );
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

	PropertyModel<?> createFallbackPropertyModel(TypeModel<?> holderTypeModel, String explicitAccessStrategyName,
			EntityMode entityMode, String propertyName, List<XProperty> xProperties) {
		Class<?> holderType = holderTypeModel.getJavaType();
		PropertyAccessStrategy accessStrategy = accessStrategyResolver.resolvePropertyAccessStrategy(
				holderType, explicitAccessStrategyName, entityMode
		);
		PropertyAccess propertyAccess = accessStrategy.buildPropertyAccess(
				holderType, propertyName
		);
		Getter getter = propertyAccess.getGetter();
		return new HibernateOrmPropertyModel<>( this, holderTypeModel, propertyName, xProperties, getter );
	}

	private <T> TypeModel<T> createTypeModel(Class<T> type) {
		TypeModel<T> typeModel = tryCreateEntityTypeModel( type );
		if ( typeModel == null ) {
			typeModel = tryCreateEmbeddableTypeModel( type );
		}
		if ( typeModel == null ) {
			typeModel = createNonManagedTypeModel( type );
		}
		return typeModel;
	}

	private <T> TypeModel<T> tryCreateEntityTypeModel(Class<T> type) {
		try {
			EntityPersister persister = sessionFactoryImplementor.getMetamodel().entityPersister( type );
			return new EntityTypeModel<>( this, type, persister );
		}
		catch (MappingException ignored) {
			// The type is not an entity in the current session factory
			return null;
		}
	}

	private <T> TypeModel<T> tryCreateEmbeddableTypeModel(Class<T> type) {
		try {
			EmbeddableType<T> embeddableType = sessionFactoryImplementor.getMetamodel().embeddable( type );
			return new EmbeddableTypeModel<>( this, embeddableType );
		}
		catch (IllegalArgumentException ignored) {
			// The type is not embeddable in the current session factory
			return null;
		}
	}

	private <T> TypeModel<T> createNonManagedTypeModel(Class<T> type) {
		return new NonManagedTypeModel<>( this, type );
	}
}
