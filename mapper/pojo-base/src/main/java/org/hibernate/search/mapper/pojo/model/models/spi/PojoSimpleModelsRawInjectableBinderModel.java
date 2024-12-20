/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model.models.spi;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.models.spi.MemberDetails;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoInjectableBinderModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoInjectablePropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public final class PojoSimpleModelsRawInjectableBinderModel<T>
		extends AbstractPojoModelsRawTypeModel<T, AbstractPojoModelsBootstrapIntrospector, PojoInjectablePropertyModel<?>>
		implements PojoInjectableBinderModel<T> {

	public PojoSimpleModelsRawInjectableBinderModel(AbstractPojoModelsBootstrapIntrospector introspector,
			PojoRawTypeIdentifier<T> typeIdentifier,
			RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		super( introspector, typeIdentifier, rawTypeDeclaringContext );
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<PojoSimpleModelsRawInjectableBinderModel<? super T>> ascendingSuperTypes() {
		return introspector.ascendingSuperClasses( classDetails )
				.map( xc -> (PojoSimpleModelsRawInjectableBinderModel<? super T>) introspector.injectableBinderModel( xc ) );
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<PojoSimpleModelsRawInjectableBinderModel<? super T>> descendingSuperTypes() {
		return introspector.descendingSuperClasses( classDetails )
				.map( xc -> (PojoSimpleModelsRawInjectableBinderModel<? super T>) introspector.injectableBinderModel( xc ) );
	}

	@Override
	protected Stream<String> declaredPropertyNames() {
		return declaredFieldAccessPropertiesByName().keySet().stream();
	}

	@Override
	protected PojoInjectablePropertyModel<?> createPropertyModel(String propertyName) {
		List<MemberDetails> declaredProperties = new ArrayList<>( 1 );
		MemberDetails fieldAccessProperty = declaredFieldAccessPropertiesByName().get( propertyName );
		if ( fieldAccessProperty != null ) {
			declaredProperties.add( fieldAccessProperty );
		}

		List<Member> members = findPropertyMember( propertyName );
		if ( members == null ) {
			return null;
		}

		return new PojoSimpleModelsInjectablePropertyModel<>( introspector, this, propertyName,
				declaredProperties, members );
	}

	private List<Member> findPropertyMember(String propertyName) {
		// Try using the getter first (if declared)...
		List<Member> getters = findInSelfOrParents( t -> t.declaredPropertyGetters( propertyName ) );
		if ( getters != null ) {
			return getters;
		}
		// ... and fall back to the field (or null if not found)
		Member field = findInSelfOrParents( t -> t.declaredPropertyField( propertyName ) );
		return field == null ? null : Collections.singletonList( field );
	}

	private <T2> T2 findInSelfOrParents(Function<PojoSimpleModelsRawInjectableBinderModel<?>, T2> getter) {
		return ascendingSuperTypes()
				.map( getter )
				.filter( Objects::nonNull )
				.findFirst()
				.orElse( null );
	}

}
