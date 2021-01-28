/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.model.impl;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.search.mapper.pojo.model.hcann.spi.AbstractPojoHCAnnRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.GenericContextAwarePojoGenericTypeModel.RawTypeDeclaringContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

class JavaBeanRawTypeModel<T> extends AbstractPojoHCAnnRawTypeModel<T, JavaBeanBootstrapIntrospector> {

	JavaBeanRawTypeModel(JavaBeanBootstrapIntrospector introspector, PojoRawTypeIdentifier<T> typeIdentifier,
			RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		super( introspector, typeIdentifier, rawTypeDeclaringContext );
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<JavaBeanRawTypeModel<? super T>> ascendingSuperTypes() {
		return introspector.ascendingSuperClasses( xClass )
					.map( xc -> (JavaBeanRawTypeModel<? super T>) introspector.typeModel( xc ) );
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<JavaBeanRawTypeModel<? super T>> descendingSuperTypes() {
		return introspector.descendingSuperClasses( xClass )
				.map( xc -> (JavaBeanRawTypeModel<? super T>) introspector.typeModel( xc ) );
	}

	@Override
	protected JavaBeanPropertyModel<?> createPropertyModel(String propertyName) {
		List<XProperty> declaredXProperties = new ArrayList<>( 2 );
		List<XProperty> methodAccessXProperties = declaredMethodAccessXPropertiesByName().get( propertyName );
		if ( methodAccessXProperties != null ) {
			declaredXProperties.addAll( methodAccessXProperties );
		}
		XProperty fieldAccessXProperty = declaredFieldAccessXPropertiesByName().get( propertyName );
		if ( fieldAccessXProperty != null ) {
			declaredXProperties.add( fieldAccessXProperty );
		}

		List<Member> members = findPropertyMember( propertyName );
		if ( members == null ) {
			return null;
		}

		return new JavaBeanPropertyModel<>( introspector, this, propertyName,
				declaredXProperties, members );
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

	private <T2> T2 findInSelfOrParents(Function<JavaBeanRawTypeModel<?>, T2> getter) {
		return ascendingSuperTypes()
				.map( getter )
				.filter( Objects::nonNull )
				.findFirst()
				.orElse( null );
	}

}
