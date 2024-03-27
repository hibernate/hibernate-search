/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class HibernateOrmClassRawTypeModel<T>
		extends AbstractPojoHCAnnRawTypeModel<T, HibernateOrmBootstrapIntrospector> {

	private final HibernateOrmBasicClassTypeMetadata ormTypeMetadata;

	private List<HibernateOrmClassRawTypeModel<? super T>> ascendingSuperTypesCache;
	private List<HibernateOrmClassRawTypeModel<? super T>> descendingSuperTypesCache;

	HibernateOrmClassRawTypeModel(HibernateOrmBootstrapIntrospector introspector,
			PojoRawTypeIdentifier<T> typeIdentifier,
			HibernateOrmBasicClassTypeMetadata ormTypeMetadata, RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		super( introspector, typeIdentifier, rawTypeDeclaringContext );
		this.ormTypeMetadata = ormTypeMetadata;
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<HibernateOrmClassRawTypeModel<? super T>> ascendingSuperTypes() {
		if ( ascendingSuperTypesCache == null ) {
			ascendingSuperTypesCache = introspector.ascendingSuperClasses( xClass )
					.map( xc -> (HibernateOrmClassRawTypeModel<? super T>) introspector.typeModel( xc ) )
					.collect( Collectors.toList() );
		}
		return ascendingSuperTypesCache.stream();
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<HibernateOrmClassRawTypeModel<? super T>> descendingSuperTypes() {
		if ( descendingSuperTypesCache == null ) {
			descendingSuperTypesCache = introspector.descendingSuperClasses( xClass )
					.map( xc -> (HibernateOrmClassRawTypeModel<? super T>) introspector.typeModel( xc ) )
					.collect( Collectors.toList() );
		}
		return descendingSuperTypesCache.stream();
	}

	@Override
	protected HibernateOrmClassPropertyModel<?> createPropertyModel(String propertyName) {
		List<XProperty> declaredXProperties = new ArrayList<>( 2 );
		List<XProperty> methodAccessXProperties = declaredMethodAccessXPropertiesByName().get( propertyName );
		if ( methodAccessXProperties != null ) {
			declaredXProperties.addAll( methodAccessXProperties );
		}
		XProperty fieldAccessXProperty = declaredFieldAccessXPropertiesByName().get( propertyName );
		if ( fieldAccessXProperty != null ) {
			declaredXProperties.add( fieldAccessXProperty );
		}

		HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata = findOrmPropertyMetadata( propertyName );
		List<Member> members = findPropertyMember( propertyName, ormPropertyMetadata );

		if ( members == null ) {
			return null;
		}

		return new HibernateOrmClassPropertyModel<>( introspector, this, propertyName,
				declaredXProperties, ormPropertyMetadata, members );
	}

	private HibernateOrmBasicClassPropertyMetadata findOrmPropertyMetadata(String propertyName) {
		return findInSelfOrParents( t -> t.ormPropertyMetadataFromThisType( propertyName ) );
	}

	private HibernateOrmBasicClassPropertyMetadata ormPropertyMetadataFromThisType(String propertyName) {
		if ( ormTypeMetadata != null ) {
			return ormTypeMetadata.getClassPropertyMetadataOrNull( propertyName );
		}
		else {
			return null;
		}
	}

	private List<Member> findPropertyMember(String propertyName, HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata) {
		if ( ormPropertyMetadata != null ) {
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
			Member memberFromHibernateOrmMetamodel = ormPropertyMetadata.getMember();
			if ( memberFromHibernateOrmMetamodel instanceof Method ) {
				return findInSelfOrParents( t -> t.declaredPropertyGetters( propertyName ) );
			}
			else if ( memberFromHibernateOrmMetamodel instanceof Field ) {
				// The field name may be different from the property name,
				// in particular with Grails when using Groovy traits (see HSEARCH-4348)
				String memberName = memberFromHibernateOrmMetamodel.getName();
				Member field = findInSelfOrParents( t -> t.declaredPropertyField( memberName ) );
				return field == null ? null : Collections.singletonList( field );
			}
			else {
				return null;
			}
		}
		else {
			// Hibernate ORM doesn't have any metadata for this property (the property is transient).
			// Try using the getter first (if declared)...
			List<Member> getters = findInSelfOrParents( t -> t.declaredPropertyGetters( propertyName ) );
			if ( getters != null ) {
				return getters;
			}
			// ... and fall back to the field (or null if not found)
			Member field = findInSelfOrParents( t -> t.declaredPropertyField( propertyName ) );
			return field == null ? null : Collections.singletonList( field );
		}
	}

	private <T2> T2 findInSelfOrParents(Function<HibernateOrmClassRawTypeModel<?>, T2> getter) {
		return ascendingSuperTypes()
				.map( getter )
				.filter( Objects::nonNull )
				.findFirst()
				.orElse( null );
	}

}
