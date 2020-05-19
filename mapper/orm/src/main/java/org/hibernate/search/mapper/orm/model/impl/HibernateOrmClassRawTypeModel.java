/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.model.hcann.spi.PojoCommonsAnnotationsHelper;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class HibernateOrmClassRawTypeModel<T> extends AbstractHibernateOrmRawTypeModel<T> {

	private final HibernateOrmBasicClassTypeMetadata ormTypeMetadata;
	private final RawTypeDeclaringContext<T> rawTypeDeclaringContext;

	private List<HibernateOrmClassRawTypeModel<? super T>> ascendingSuperTypesCache;
	private List<HibernateOrmClassRawTypeModel<? super T>> descendingSuperTypesCache;

	private final Map<String, HibernateOrmClassPropertyModel<?>> propertyModelCache = new HashMap<>();

	private Map<String, XProperty> declaredFieldAccessXPropertiesByName;
	private Map<String, XProperty> declaredMethodAccessXPropertiesByName;

	HibernateOrmClassRawTypeModel(HibernateOrmBootstrapIntrospector introspector,
			PojoRawTypeIdentifier<T> typeIdentifier,
			HibernateOrmBasicClassTypeMetadata ormTypeMetadata, RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		super( introspector, typeIdentifier );
		this.ormTypeMetadata = ormTypeMetadata;
		this.rawTypeDeclaringContext = rawTypeDeclaringContext;
	}

	@Override
	public boolean isAbstract() {
		return xClass.isAbstract();
	}

	@Override
	public boolean isSubTypeOf(MappableTypeModel superTypeCandidate) {
		return superTypeCandidate instanceof HibernateOrmClassRawTypeModel
				&& ( (HibernateOrmClassRawTypeModel<?>) superTypeCandidate ).xClass.isAssignableFrom( xClass );
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<HibernateOrmClassRawTypeModel<? super T>> ascendingSuperTypes() {
		if ( ascendingSuperTypesCache == null ) {
			ascendingSuperTypesCache =
					( (Stream<HibernateOrmClassRawTypeModel<? super T>>) introspector.getAscendingSuperTypes( xClass ) )
					.collect( Collectors.toList() );
		}
		return ascendingSuperTypesCache.stream();
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<HibernateOrmClassRawTypeModel<? super T>> descendingSuperTypes() {
		if ( descendingSuperTypesCache == null ) {
			descendingSuperTypesCache =
					( (Stream<HibernateOrmClassRawTypeModel<? super T>>) introspector.getDescendingSuperTypes( xClass ) )
					.collect( Collectors.toList() );
		}
		return descendingSuperTypesCache.stream();
	}

	@Override
	public Stream<Annotation> getAnnotations() {
		return introspector.getAnnotations( xClass );
	}

	@Override
	Stream<String> getDeclaredPropertyNames() {
		return Stream.concat(
				getDeclaredFieldAccessXPropertiesByName().keySet().stream(),
				getDeclaredMethodAccessXPropertiesByName().keySet().stream()
		)
				.distinct();
	}

	@Override
	HibernateOrmClassPropertyModel<?> getPropertyOrNull(String propertyName) {
		return propertyModelCache.computeIfAbsent( propertyName, this::createPropertyModel );
	}

	RawTypeDeclaringContext<T> getRawTypeDeclaringContext() {
		return rawTypeDeclaringContext;
	}

	private Map<String, XProperty> getDeclaredFieldAccessXPropertiesByName() {
		if ( declaredFieldAccessXPropertiesByName == null ) {
			declaredFieldAccessXPropertiesByName =
					introspector.getDeclaredFieldAccessXPropertiesByName( xClass );
		}
		return declaredFieldAccessXPropertiesByName;
	}

	private Map<String, XProperty> getDeclaredMethodAccessXPropertiesByName() {
		if ( declaredMethodAccessXPropertiesByName == null ) {
			declaredMethodAccessXPropertiesByName =
					introspector.getDeclaredMethodAccessXPropertiesByName( xClass );
		}
		return declaredMethodAccessXPropertiesByName;
	}

	private HibernateOrmClassPropertyModel<?> createPropertyModel(String propertyName) {
		List<XProperty> declaredXProperties = new ArrayList<>( 2 );
		XProperty methodAccessXProperty = getDeclaredMethodAccessXPropertiesByName().get( propertyName );
		if ( methodAccessXProperty != null ) {
			declaredXProperties.add( methodAccessXProperty );
		}
		XProperty fieldAccessXProperty = getDeclaredFieldAccessXPropertiesByName().get( propertyName );
		if ( fieldAccessXProperty != null ) {
			declaredXProperties.add( fieldAccessXProperty );
		}

		HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata = findOrmPropertyMetadata( propertyName );
		Member member = findPropertyMember( propertyName, ormPropertyMetadata );

		if ( member == null ) {
			return null;
		}

		return new HibernateOrmClassPropertyModel<>(
				introspector, this, propertyName,
				declaredXProperties, ormPropertyMetadata, member
		);
	}

	private HibernateOrmBasicClassPropertyMetadata findOrmPropertyMetadata(String propertyName) {
		HibernateOrmBasicClassPropertyMetadata propertyMetadata = getOrmPropertyMetadataFromThisType( propertyName );
		if ( propertyMetadata == null ) {
			propertyMetadata = getOrmPropertyMetadataFromParentTypes( propertyName );
		}
		return propertyMetadata;
	}

	private Member findPropertyMember(String propertyName,
			HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata) {
		Member result = getPropertyMemberFromThisType( propertyName, ormPropertyMetadata );

		if ( result == null ) {
			// There is no member for this property on the current type.
			// Try to find one in the closest supertype.
			result = getPropertyMemberFromParentTypes( propertyName, ormPropertyMetadata );
		}

		return result;
	}

	private HibernateOrmBasicClassPropertyMetadata getOrmPropertyMetadataFromParentTypes(String propertyName) {
		// TODO HSEARCH-3056 remove lambdas if possible
		return ascendingSuperTypes()
				.skip( 1 ) // Ignore self
				.map( type -> type.getOrmPropertyMetadataFromThisType( propertyName ) )
				.filter( Objects::nonNull )
				.findFirst()
				.orElse( null );
	}

	private HibernateOrmBasicClassPropertyMetadata getOrmPropertyMetadataFromThisType(String propertyName) {
		if ( ormTypeMetadata != null ) {
			return ormTypeMetadata.getClassPropertyMetadataOrNull( propertyName );
		}
		else {
			return null;
		}
	}

	private Member getPropertyMemberFromParentTypes(String propertyName,
			HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata) {
		// TODO HSEARCH-3056 remove lambdas if possible
		return ascendingSuperTypes()
				.skip( 1 ) // Ignore self
				.map( type -> type.getPropertyMemberFromThisType( propertyName, ormPropertyMetadata ) )
				.filter( Objects::nonNull )
				.findFirst()
				.orElse( null );
	}

	private Member getPropertyMemberFromThisType(String propertyName,
			HibernateOrmBasicClassPropertyMetadata propertyMetadataFromHibernateOrmMetamodel) {
		XProperty methodAccessXProperty = getDeclaredMethodAccessXPropertiesByName().get( propertyName );
		XProperty fieldAccessXProperty = getDeclaredFieldAccessXPropertiesByName().get( propertyName );
		if ( propertyMetadataFromHibernateOrmMetamodel != null ) {
			// Hibernate ORM has metadata for this property (the property is persisted).
			// Use ORM metadata to find the corresponding member (field/method).
			return getPropertyMemberUsingHibernateOrmMetadataFromThisType(
					methodAccessXProperty, fieldAccessXProperty, propertyMetadataFromHibernateOrmMetamodel
			);
		}
		else {
			// Hibernate ORM doesn't have any metadata for this property (the property is transient).
			// Use reflection to find the corresponding member (field/method).
			return getPropertyMemberUsingReflectionFromThisType(
					methodAccessXProperty, fieldAccessXProperty
			);
		}
	}

	private Member getPropertyMemberUsingHibernateOrmMetadataFromThisType(XProperty methodAccessXProperty,
			XProperty fieldAccessXProperty,
			HibernateOrmBasicClassPropertyMetadata propertyMetadataFromHibernateOrmMetamodel) {
		Member memberFromHibernateOrmMetamodel = propertyMetadataFromHibernateOrmMetamodel.getMember();
		/*
		 * Hibernate ORM has metadata for this property,
		 * which means this property is persisted.
		 *
		 * Hibernate ORM might return us the member as declared in a supertype,
		 * in which case the type of that member will not be up-to-date.
		 * Thus we try to get the overridden member declared in the current type,
		 * and failing that we look for the member in supertypes.
		 *
		 * We still try to comply with JPA's configured access type,
		 * which explains the two if/else branches below.
		 */
		if ( memberFromHibernateOrmMetamodel instanceof Method ) {
			return methodAccessXProperty == null
					? memberFromHibernateOrmMetamodel
					: PojoCommonsAnnotationsHelper.getUnderlyingMember( methodAccessXProperty );
		}
		else if ( memberFromHibernateOrmMetamodel instanceof Field ) {
			return fieldAccessXProperty == null
					? memberFromHibernateOrmMetamodel
					: PojoCommonsAnnotationsHelper.getUnderlyingMember( fieldAccessXProperty );
		}
		else {
			return null;
		}
	}

	/*
	 * Hibernate ORM doesn't have any metadata for this property,
	 * which means this property is transient.
	 * We don't need to worry about JPA's access type.
	 */
	private Member getPropertyMemberUsingReflectionFromThisType(
			XProperty methodAccessXProperty, XProperty fieldAccessXProperty) {
		if ( methodAccessXProperty != null ) {
			// Method access is available. Get values from the getter.
			return PojoCommonsAnnotationsHelper.getUnderlyingMember( methodAccessXProperty );
		}
		else if ( fieldAccessXProperty != null ) {
			// Method access is not available, but field access is. Get values directly from the field.
			return PojoCommonsAnnotationsHelper.getUnderlyingMember( fieldAccessXProperty );
		}
		else {
			// Neither method access nor field access is available.
			// The property is not declared in this type.
			return null;
		}
	}
}
