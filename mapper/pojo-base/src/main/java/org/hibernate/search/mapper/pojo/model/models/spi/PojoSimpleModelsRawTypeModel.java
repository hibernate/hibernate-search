/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public final class PojoSimpleModelsRawTypeModel<T>
		extends AbstractPojoModelsRawTypeModel<T, AbstractPojoModelsBootstrapIntrospector> {

	public PojoSimpleModelsRawTypeModel(AbstractPojoModelsBootstrapIntrospector introspector,
			PojoRawTypeIdentifier<T> typeIdentifier,
			RawTypeDeclaringContext<T> rawTypeDeclaringContext) {
		super( introspector, typeIdentifier, rawTypeDeclaringContext );
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<PojoSimpleModelsRawTypeModel<? super T>> ascendingSuperTypes() {
		return introspector.ascendingSuperClasses( classDetails )
				.map( xc -> (PojoSimpleModelsRawTypeModel<? super T>) introspector.typeModel( xc ) );
	}

	@Override
	@SuppressWarnings("unchecked") // xClass represents T, so its supertypes represent ? super T
	public Stream<PojoSimpleModelsRawTypeModel<? super T>> descendingSuperTypes() {
		return introspector.descendingSuperClasses( classDetails )
				.map( xc -> (PojoSimpleModelsRawTypeModel<? super T>) introspector.typeModel( xc ) );
	}

	@Override
	protected PojoPropertyModel<?> createPropertyModel(String propertyName) {
		List<MemberDetails> declaredProperties = new ArrayList<>( 2 );
		List<MemberDetails> methodAccessProperties = declaredMethodAccessPropertiesByName().get( propertyName );
		if ( methodAccessProperties != null ) {
			declaredProperties.addAll( methodAccessProperties );
		}
		MemberDetails fieldAccessProperty = declaredFieldAccessPropertiesByName().get( propertyName );
		if ( fieldAccessProperty != null ) {
			declaredProperties.add( fieldAccessProperty );
		}

		List<Member> members = findPropertyMember( propertyName );
		if ( members == null ) {
			return null;
		}

		return new PojoSimpleModelsPropertyModel<>( introspector, this, propertyName,
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

	private <T2> T2 findInSelfOrParents(Function<PojoSimpleModelsRawTypeModel<?>, T2> getter) {
		return ascendingSuperTypes()
				.map( getter )
				.filter( Objects::nonNull )
				.findFirst()
				.orElse( null );
	}

}
