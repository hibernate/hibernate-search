/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.AbstractPojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

@SuppressWarnings("rawtypes")
public class HibernateOrmDynamicMapRawTypeModel
		extends AbstractPojoRawTypeModel<Map, HibernateOrmBootstrapIntrospector> {

	private final HibernateOrmBasicDynamicMapTypeMetadata ormTypeMetadata;

	HibernateOrmDynamicMapRawTypeModel(HibernateOrmBootstrapIntrospector introspector,
			PojoRawTypeIdentifier<Map> typeIdentifier,
			HibernateOrmBasicDynamicMapTypeMetadata ormTypeMetadata) {
		super( introspector, typeIdentifier );
		this.ormTypeMetadata = ormTypeMetadata;
	}

	@Override
	public final boolean isAbstract() {
		return false;
	}

	@Override
	public boolean isSubTypeOf(MappableTypeModel superTypeCandidate) {
		if ( equals( superTypeCandidate ) ) {
			return true;
		}
		PojoRawTypeModel<?> superType = getSuperType();
		if ( superType != null ) {
			return superType.isSubTypeOf( superTypeCandidate );
		}
		return false;
	}

	@Override
	public Stream<PojoRawTypeModel<? super Map>> ascendingSuperTypes() {
		return Stream.concat( Stream.of( this ), getSuperType().ascendingSuperTypes() );
	}

	@Override
	public Stream<PojoRawTypeModel<? super Map>> descendingSuperTypes() {
		return Stream.concat( getSuperType().descendingSuperTypes(), Stream.of( this ) );
	}

	@Override
	public Optional<? extends PojoTypeModel<?>> typeArgument(Class<?> rawSuperType, int typeParameterIndex) {
		return Optional.empty();
	}

	@Override
	public Optional<? extends PojoTypeModel<?>> arrayElementType() {
		return Optional.empty();
	}

	@Override
	public Stream<Annotation> annotations() {
		return Stream.empty();
	}

	@Override
	@SuppressWarnings("unchecked")
	public PojoTypeModel<? extends Map> cast(PojoTypeModel<?> other) {
		if ( other.rawType().isSubTypeOf( this ) ) {
			// Redundant cast; no need to create a new type.
			return (PojoTypeModel<? extends Map>) other;
		}
		else {
			// There is no generic type information to retain for dynamic-map types; we can just return this.
			// Also, calling other.castTo(...) would mean losing the type name, and we definitely don't want that.
			return this;
		}
	}

	@Override
	protected Stream<String> declaredPropertyNames() {
		return ormTypeMetadata.getPropertyNames().stream();
	}

	/**
	 * @return The supertype of this type.
	 * Dynamic-map types cannot implement interfaces, so they only have one direct supertype.
	 */
	private PojoRawTypeModel<? super Map> getSuperType() {
		HibernateOrmDynamicMapRawTypeModel entitySupertypeOrNull = getSuperEntityOrNull();
		if ( entitySupertypeOrNull != null ) {
			return entitySupertypeOrNull;
		}
		else {
			return introspector.typeModel( typeIdentifier.javaClass() );
		}
	}

	private HibernateOrmDynamicMapRawTypeModel getSuperEntityOrNull() {
		String superEntityName = ormTypeMetadata.getSuperEntityNameOrNull();
		if ( superEntityName != null ) {
			// This cast is safe, because if a dynamic-map entity type has an entity supertype,
			// that entity supertype is also a dynamic-map entity type.
			return (HibernateOrmDynamicMapRawTypeModel) introspector.typeModel( superEntityName );
		}
		return null;
	}

	@Override
	protected HibernateOrmDynamicMapPropertyModel<?> createPropertyModel(String propertyName) {
		HibernateOrmBasicDynamicMapPropertyMetadata ormPropertyMetadata = getPropertyMetadata( propertyName );

		if ( ormPropertyMetadata == null ) {
			return null;
		}

		return new HibernateOrmDynamicMapPropertyModel<>(
				introspector, this, propertyName,
				ormPropertyMetadata
		);
	}

	private HibernateOrmBasicDynamicMapPropertyMetadata getPropertyMetadata(String propertyName) {
		HibernateOrmBasicDynamicMapPropertyMetadata ormPropertyMetadata =
				ormTypeMetadata.getDynamicMapPropertyMetadataOrNull( propertyName );

		if ( ormPropertyMetadata == null ) {
			/*
			 * We don't have a metadata for this property in the current type.
			 * The property may be inherited: try to find the metadata in the parent types.
			 */
			HibernateOrmDynamicMapRawTypeModel entitySupertypeOrNull = getSuperEntityOrNull();
			if ( entitySupertypeOrNull != null ) {
				ormPropertyMetadata = entitySupertypeOrNull.getPropertyMetadata( propertyName );
			}
		}

		return ormPropertyMetadata;
	}
}
