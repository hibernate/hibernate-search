/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

public class HibernateOrmDynamicMapRawTypeModel extends AbstractHibernateOrmRawTypeModel<Map> {

	private final HibernateOrmBasicDynamicMapTypeMetadata ormTypeMetadata;

	private final Map<String, HibernateOrmDynamicMapPropertyModel<?>> propertyModelCache = new HashMap<>();

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
		AbstractHibernateOrmRawTypeModel<?> superType = getSuperType();
		if ( superType != null ) {
			return superType.isSubTypeOf( superTypeCandidate );
		}
		return false;
	}

	@Override
	public Stream<AbstractHibernateOrmRawTypeModel<? super Map>> ascendingSuperTypes() {
		return Stream.concat( Stream.of( this ), getSuperType().ascendingSuperTypes() );
	}

	@Override
	public Stream<AbstractHibernateOrmRawTypeModel<? super Map>> descendingSuperTypes() {
		return Stream.concat( getSuperType().descendingSuperTypes(), Stream.of( this ) );
	}

	@Override
	public Stream<Annotation> getAnnotations() {
		return Stream.empty();
	}

	@Override
	HibernateOrmDynamicMapPropertyModel<?> getPropertyOrNull(String propertyName) {
		return propertyModelCache.computeIfAbsent( propertyName, this::createPropertyModel );
	}

	@Override
	Stream<String> getDeclaredPropertyNames() {
		return ormTypeMetadata.getPropertyNames().stream();
	}

	/**
	 * @return The supertype of this type.
	 * Dynamic-map types cannot implement interfaces, so they only have one direct supertype.
	 */
	private AbstractHibernateOrmRawTypeModel<? super Map> getSuperType() {
		HibernateOrmDynamicMapRawTypeModel entitySupertypeOrNull = getSuperEntityOrNull();
		if ( entitySupertypeOrNull != null ) {
			return entitySupertypeOrNull;
		}
		else {
			return introspector.getTypeModel( typeIdentifier.getJavaClass() );
		}
	}

	private HibernateOrmDynamicMapRawTypeModel getSuperEntityOrNull() {
		String superEntityName = ormTypeMetadata.getSuperEntityNameOrNull();
		if ( superEntityName != null ) {
			// This cast is safe, because if a dynamic-map entity type has an entity supertype,
			// that entity supertype is also a dynamic-map entity type.
			return (HibernateOrmDynamicMapRawTypeModel) introspector.getTypeModel( superEntityName );
		}
		return null;
	}

	private HibernateOrmDynamicMapPropertyModel<?> createPropertyModel(String propertyName) {
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
