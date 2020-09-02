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
import java.util.function.Function;
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
	public Stream<Annotation> annotations() {
		return introspector.annotations( xClass );
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
					introspector.declaredFieldAccessXPropertiesByName( xClass );
		}
		return declaredFieldAccessXPropertiesByName;
	}

	private Map<String, XProperty> getDeclaredMethodAccessXPropertiesByName() {
		if ( declaredMethodAccessXPropertiesByName == null ) {
			declaredMethodAccessXPropertiesByName =
					introspector.declaredMethodAccessXPropertiesByName( xClass );
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

	private Member findPropertyMember(String propertyName, HibernateOrmBasicClassPropertyMetadata ormPropertyMetadata) {
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
				return findInSelfOrParents( t -> t.declaredPropertyGetter( propertyName ) );
			}
			else if ( memberFromHibernateOrmMetamodel instanceof Field ) {
				return findInSelfOrParents( t -> t.declaredPropertyField( propertyName ) );
			}
			else {
				return null;
			}
		}
		else {
			// Hibernate ORM doesn't have any metadata for this property (the property is transient).
			// Try using the getter first (if declared)...
			Member getter = findInSelfOrParents( t -> t.declaredPropertyGetter( propertyName ) );
			if ( getter != null ) {
				return getter;
			}
			// ... and fall back to the field (or null if not found)
			return findInSelfOrParents( t -> t.declaredPropertyField( propertyName ) );
		}
	}

	private <T2> T2 findInSelfOrParents(Function<HibernateOrmClassRawTypeModel<?>, T2> getter) {
		return ascendingSuperTypes()
				.skip( 1 ) // Ignore self
				.map( getter )
				.filter( Objects::nonNull )
				.findFirst()
				.orElse( null );
	}

	private Member declaredPropertyGetter(String propertyName) {
		XProperty methodAccessXProperty = getDeclaredMethodAccessXPropertiesByName().get( propertyName );
		if ( methodAccessXProperty != null ) {
			// Method access is available. Get values from the getter.
			return PojoCommonsAnnotationsHelper.extractUnderlyingMember( methodAccessXProperty );
		}
		return null;
	}

	private Member declaredPropertyField(String propertyName) {
		XProperty fieldAccessXProperty = getDeclaredFieldAccessXPropertiesByName().get( propertyName );
		if ( fieldAccessXProperty != null ) {
			// Method access is available. Get values from the getter.
			return PojoCommonsAnnotationsHelper.extractUnderlyingMember( fieldAccessXProperty );
		}
		return null;
	}
}
