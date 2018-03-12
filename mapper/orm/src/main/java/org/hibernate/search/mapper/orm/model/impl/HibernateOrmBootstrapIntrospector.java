/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
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
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessStrategyResolver;
import org.hibernate.search.mapper.orm.util.impl.XClassOrdering;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoGenericTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.mapper.pojo.util.spi.AnnotationHelper;

/**
 * @author Yoann Rodiere
 */
public class HibernateOrmBootstrapIntrospector implements PojoBootstrapIntrospector {

	private final ReflectionManager reflectionManager;
	private final AnnotationHelper annotationHelper;
	private final SessionFactoryImplementor sessionFactoryImplementor;
	private final PropertyAccessStrategyResolver accessStrategyResolver;
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
	 * @see AbstractHibernateOrmTypeModel#propertyModelCache
	 */
	private final Map<Class<?>, PojoRawTypeModel<?>> typeModelCache = new HashMap<>();

	public HibernateOrmBootstrapIntrospector(Metadata metadata, SessionFactoryImplementor sessionFactoryImplementor) {
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
		this.genericContextHelper = new HibernateOrmGenericContextHelper( this );
		this.missingRawTypeDeclaringContext = new RawTypeDeclaringContext<>(
				genericContextHelper, Object.class
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> PojoRawTypeModel<T> getTypeModel(Class<T> clazz) {
		return (PojoRawTypeModel<T>) typeModelCache.computeIfAbsent( clazz, this::createTypeModel );
	}

	@Override
	public <T> PojoGenericTypeModel<T> getGenericTypeModel(Class<T> clazz) {
		return missingRawTypeDeclaringContext.createGenericTypeModel( clazz );
	}

	@SuppressWarnings( "unchecked" )
	<T> Stream<PojoRawTypeModel<? super T>> getAscendingSuperTypes(XClass xClass) {
		return XClassOrdering.get().getAscendingSuperTypes( xClass )
				.map( superType -> (PojoRawTypeModel<? super T>) getTypeModel( superType ) );
	}

	@SuppressWarnings( "unchecked" )
	<T> Stream<PojoRawTypeModel<? super T>> getDescendingSuperTypes(XClass xClass) {
		return XClassOrdering.get().getDescendingSuperTypes( xClass )
				.map( superType -> (PojoRawTypeModel<? super T>) getTypeModel( superType ) );
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

	PojoPropertyModel<?> createMemberPropertyModel(AbstractHibernateOrmTypeModel<?> holderTypeModel,
			String propertyName, Member member, List<XProperty> declaredXProperties) {
		Class<?> holderType = holderTypeModel.getJavaClass();
		Getter getter;
		if ( member instanceof Method ) {
			getter = new GetterMethodImpl( holderType, propertyName, (Method) member );
		}
		else if ( member instanceof Field ) {
			getter = new GetterFieldImpl( holderType, propertyName, (Field) member );
		}
		else {
			throw new AssertionFailure( "Unexpected type for a " + Member.class.getName() + ": " + member );
		}
		return new HibernateOrmPropertyModel<>( this, holderTypeModel, propertyName,
				declaredXProperties, getter );
	}

	PojoPropertyModel<?> createFallbackPropertyModel(AbstractHibernateOrmTypeModel<?> holderTypeModel,
			String explicitAccessStrategyName, EntityMode entityMode, String propertyName,
			List<XProperty> declaredXProperties) {
		Class<?> holderType = holderTypeModel.getJavaClass();
		PropertyAccessStrategy accessStrategy = accessStrategyResolver.resolvePropertyAccessStrategy(
				holderType, explicitAccessStrategyName, entityMode
		);
		PropertyAccess propertyAccess = accessStrategy.buildPropertyAccess(
				holderType, propertyName
		);
		Getter getter = propertyAccess.getGetter();
		return new HibernateOrmPropertyModel<>( this, holderTypeModel, propertyName,
				declaredXProperties, getter );
	}

	@SuppressWarnings( "unchecked" )
	private PojoTypeModel<?> getTypeModel(XClass xClass) {
		return getTypeModel( reflectionManager.toClass( xClass ) );
	}

	private <T> PojoRawTypeModel<T> createTypeModel(Class<T> type) {
		PojoRawTypeModel<T> typeModel = tryCreateEntityTypeModel( type );
		if ( typeModel == null ) {
			typeModel = tryCreateEmbeddableTypeModel( type );
		}
		if ( typeModel == null ) {
			typeModel = tryCreateMappedSuperclassTypeModel( type );
		}
		if ( typeModel == null ) {
			typeModel = createNonManagedTypeModel( type );
		}
		return typeModel;
	}

	private <T> PojoRawTypeModel<T> tryCreateEntityTypeModel(Class<T> type) {
		try {
			EntityPersister persister = sessionFactoryImplementor.getMetamodel().entityPersister( type );
			return new HibernateOrmEntityTypeModel<>(
					this, type, persister,
					new RawTypeDeclaringContext<>( genericContextHelper, type )
			);
		}
		catch (MappingException ignored) {
			// The type is not an entity in the current session factory
			return null;
		}
	}

	private <T> PojoRawTypeModel<T> tryCreateEmbeddableTypeModel(Class<T> type) {
		try {
			EmbeddableType<T> embeddableType = sessionFactoryImplementor.getMetamodel().embeddable( type );
			return new HibernateOrmEmbeddableTypeModel<>(
					this, embeddableType,
					new RawTypeDeclaringContext<>( genericContextHelper, type )
			);
		}
		catch (IllegalArgumentException ignored) {
			// The type is not embeddable in the current session factory
			return null;
		}
	}

	private <T> PojoRawTypeModel<T> tryCreateMappedSuperclassTypeModel(Class<T> type) {
		try {
			/*
			 * We try this after having tried to create an entity type model and an embeddable type model,
			 * so if the type is managed it must be a mapped superclass.
			 */
			ManagedType<T> managedType = sessionFactoryImplementor.getMetamodel().managedType( type );
			return new HibernateOrmMappedSuperclassTypeModel<>(
					this, managedType,
					new RawTypeDeclaringContext<>( genericContextHelper, type )
			);
		}
		catch (IllegalArgumentException ignored) {
			// The type is not managed in the current session factory
			return null;
		}
	}

	private <T> PojoRawTypeModel<T> createNonManagedTypeModel(Class<T> type) {
		return new HibernateOrmNonManagedTypeModel<>(
				this, type,
				new RawTypeDeclaringContext<>( genericContextHelper, type )
		);
	}
}
