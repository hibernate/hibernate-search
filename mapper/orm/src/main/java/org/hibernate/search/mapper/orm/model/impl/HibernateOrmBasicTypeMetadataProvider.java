/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

@SuppressWarnings( "unchecked" ) // Hibernate ORM gives us raw types, we must make do.
public class HibernateOrmBasicTypeMetadataProvider {

	static <T> PojoRawTypeIdentifier<T> createClassTypeIdentifier(Class<T> javaClass) {
		return PojoRawTypeIdentifier.of( javaClass );
	}

	static PojoRawTypeIdentifier<Map> createDynamicMapTypeIdentifier(String name) {
		return PojoRawTypeIdentifier.of( Map.class, name );
	}

	public static HibernateOrmBasicTypeMetadataProvider create(Metadata metadata) {
		Collection<PersistentClass> persistentClasses = metadata.getEntityBindings()
				.stream()
				/*
				 * The persistent classes from Hibernate ORM are stored in a HashMap whose order is not well defined.
				 * We use a sorted map here to make iteration deterministic.
				 */
				.collect( Collectors.toCollection(
						() -> new TreeSet<>( Comparator.comparing( PersistentClass::getEntityName ) )
				) );

		Builder builder = new Builder();

		for ( PersistentClass persistentClass : persistentClasses ) {
			collectPersistentClass( builder, persistentClass );
		}

		return builder.build();
	}

	private static void collectPersistentClass(Builder metadataProviderBuilder, PersistentClass persistentClass) {
		String jpaEntityName = persistentClass.getJpaEntityName();
		String hibernateOrmEntityName = persistentClass.getEntityName();

		metadataProviderBuilder.persistentClasses.put( hibernateOrmEntityName, persistentClass );
		metadataProviderBuilder.jpaEntityNameToHibernateOrmEntityName.put( jpaEntityName, hibernateOrmEntityName );

		if ( persistentClass.hasPojoRepresentation() ) {
			Class<?> javaClass = persistentClass.getMappedClass();
			PojoRawTypeIdentifier<?> typeIdentifier = createClassTypeIdentifier( javaClass );

			collectClassType(
					metadataProviderBuilder, javaClass,
					persistentClass.getIdentifierProperty(), persistentClass.getPropertyIterator()
			);

			metadataProviderBuilder.entityTypeIdentifiersByJavaClass.put(
					javaClass, typeIdentifier
			);
		}
		else {
			PojoRawTypeIdentifier<Map> typeIdentifier = createDynamicMapTypeIdentifier( hibernateOrmEntityName );

			collectDynamicMapType(
					metadataProviderBuilder, hibernateOrmEntityName,
					persistentClass.getSuperclass(),
					persistentClass.getIdentifierProperty(), persistentClass.getPropertyIterator()
			);

			metadataProviderBuilder.entityTypeIdentifiersByHibernateOrmEntityName.put(
					hibernateOrmEntityName, typeIdentifier
			);
		}
	}

	private static void collectClassType(Builder metadataProviderBuilder, Class<?> javaClass,
			Property identifierProperty, Iterator<Property> propertyIterator) {
		Map<String, HibernateOrmBasicClassPropertyMetadata> properties = new LinkedHashMap<>();
		if ( identifierProperty != null ) {
			collectClassProperty( metadataProviderBuilder, properties, javaClass, identifierProperty, true );
		}
		while ( propertyIterator.hasNext() ) {
			Property property = propertyIterator.next();
			collectClassProperty( metadataProviderBuilder, properties, javaClass, property, false );
		}

		metadataProviderBuilder.classTypeMetadata.put(
				javaClass,
				new HibernateOrmBasicClassTypeMetadata( properties )
		);
	}

	private static void collectDynamicMapType(Builder metadataProviderBuilder, String name,
			PersistentClass superClass,
			Property identifierProperty, Iterator<Property> propertyIterator) {
		String superEntityName = superClass == null ? null : superClass.getEntityName();

		Map<String, HibernateOrmBasicDynamicMapPropertyMetadata> properties = new LinkedHashMap<>();
		if ( identifierProperty != null ) {
			collectDynamicMapProperty( metadataProviderBuilder, properties, identifierProperty );
		}
		while ( propertyIterator.hasNext() ) {
			Property property = propertyIterator.next();
			collectDynamicMapProperty( metadataProviderBuilder, properties, property );
		}

		metadataProviderBuilder.dynamicMapTypeMetadata.put(
				name,
				new HibernateOrmBasicDynamicMapTypeMetadata( superEntityName, properties )
		);
	}

	private static void collectClassProperty(Builder metadataProviderBuilder,
			Map<String, HibernateOrmBasicClassPropertyMetadata> collectedProperties,
			Class<?> propertyHolderJavaClass, Property property, boolean isId) {
		try {
			Getter getter = property.getGetter( propertyHolderJavaClass );
			collectedProperties.put(
					property.getName(),
					new HibernateOrmBasicClassPropertyMetadata( getter.getMember(), isId )
			);
		}
		catch (MappingException ignored) {
			// Ignore, we just don't have any useful information
		}
		// Recurse to collect embedded types
		collectValue( metadataProviderBuilder, property.getValue() );
	}

	private static void collectDynamicMapProperty(Builder metadataProviderBuilder,
			Map<String, HibernateOrmBasicDynamicMapPropertyMetadata> collectedProperties,
			Property property) {
		Value value = property.getValue();
		collectedProperties.put(
				property.getName(),
				new HibernateOrmBasicDynamicMapPropertyMetadata( value )
		);
		// Recurse to collect embedded types
		// FIXME guess the property type from this "value"
		collectValue( metadataProviderBuilder, value );
	}

	private static void collectValue(Builder metadataProviderBuilder, Value value) {
		if ( value instanceof Component ) {
			collectEmbeddedType( metadataProviderBuilder, (Component) value );
		}
		else if ( value instanceof org.hibernate.mapping.Collection ) {
			org.hibernate.mapping.Collection collection = (org.hibernate.mapping.Collection) value;
			// Recurse
			collectValue( metadataProviderBuilder, collection.getElement() );
			if ( collection instanceof IndexedCollection ) {
				IndexedCollection indexedCollection = (IndexedCollection) collection;
				/*
				 * Do not let ORM confuse you: getKey() doesn't return the value of the map key,
				 * but the value of the foreign key to the targeted entity...
				 * We need to call getIndex() to retrieve the value of the map key.
				 */
				collectValue( metadataProviderBuilder, indexedCollection.getIndex() );
			}
		}
	}

	private static void collectEmbeddedType(Builder metadataProviderBuilder, Component component) {
		if ( component.isDynamic() ) {
			String name = component.getRoleName();
			// We don't care about duplicates, we assume they are all the same regarding the information we need
			if ( !metadataProviderBuilder.dynamicMapTypeMetadata.containsKey( name ) ) {
				collectDynamicMapType(
						metadataProviderBuilder, name,
						null, /* No supertype */
						null /* No ID */, component.getPropertyIterator()
				);
			}
		}
		else {
			Class<?> javaClass = component.getComponentClass();
			// We don't care about duplicates, we assume they are all the same regarding the information we need
			if ( !metadataProviderBuilder.classTypeMetadata.containsKey( javaClass ) ) {
				collectClassType(
						metadataProviderBuilder, javaClass,
						null /* No ID */, component.getPropertyIterator()
				);
			}
		}
	}

	private final Map<String, PersistentClass> persistentClasses;
	private final Map<Class<?>, HibernateOrmBasicClassTypeMetadata> classTypeMetadata;
	private final Map<String, HibernateOrmBasicDynamicMapTypeMetadata> dynamicMapTypeMetadata;

	private final Map<String, String> jpaEntityNameToHibernateOrmEntityName;
	private final Map<Class<?>, PojoRawTypeIdentifier<?>> entityTypeIdentifiersByJavaClass;
	private final Map<String, PojoRawTypeIdentifier<?>> entityTypeIdentifiersByHibernateOrmEntityName;

	private HibernateOrmBasicTypeMetadataProvider(Builder builder) {
		this.persistentClasses = builder.persistentClasses;
		this.classTypeMetadata = builder.classTypeMetadata;
		this.dynamicMapTypeMetadata = builder.dynamicMapTypeMetadata;
		this.jpaEntityNameToHibernateOrmEntityName = builder.jpaEntityNameToHibernateOrmEntityName;
		this.entityTypeIdentifiersByJavaClass =
				Collections.unmodifiableMap( builder.entityTypeIdentifiersByJavaClass );
		this.entityTypeIdentifiersByHibernateOrmEntityName =
				Collections.unmodifiableMap( builder.entityTypeIdentifiersByHibernateOrmEntityName );
	}

	public Collection<PersistentClass> getPersistentClasses() {
		return persistentClasses.values();
	}

	public PersistentClass getPersistentClass(String hibernateOrmEntityName) {
		return persistentClasses.get( hibernateOrmEntityName );
	}

	public Map<Class<?>, PojoRawTypeIdentifier<?>> getEntityTypeIdentifiersByJavaClass() {
		return entityTypeIdentifiersByJavaClass;
	}

	public Map<String, PojoRawTypeIdentifier<?>> getEntityTypeIdentifiersByHibernateOrmEntityName() {
		return entityTypeIdentifiersByHibernateOrmEntityName;
	}

	HibernateOrmBasicClassTypeMetadata getBasicTypeMetadata(Class<?> clazz) {
		return classTypeMetadata.get( clazz );
	}

	HibernateOrmBasicDynamicMapTypeMetadata getBasicTypeMetadata(String name) {
		return dynamicMapTypeMetadata.get( name );
	}

	// Strangely, there is nothing of the sort in Hibernate ORM,
	// or at least nothing that would work for dynamic-map entities.
	public String getHibernateOrmEntityNameByJpaEntityName(String jpaEntityName) {
		return jpaEntityNameToHibernateOrmEntityName.get( jpaEntityName );
	}

	Set<String> getKnownTypeNames() {
		return dynamicMapTypeMetadata.keySet();
	}

	private static class Builder {
		private final Map<String, PersistentClass> persistentClasses = new LinkedHashMap<>();
		private final Map<Class<?>, HibernateOrmBasicClassTypeMetadata> classTypeMetadata = new LinkedHashMap<>();
		private final Map<String, HibernateOrmBasicDynamicMapTypeMetadata> dynamicMapTypeMetadata = new LinkedHashMap<>();

		private final Map<String, String> jpaEntityNameToHibernateOrmEntityName = new LinkedHashMap<>();
		private final Map<Class<?>, PojoRawTypeIdentifier<?>> entityTypeIdentifiersByJavaClass = new LinkedHashMap<>();
		private final Map<String, PojoRawTypeIdentifier<?>> entityTypeIdentifiersByHibernateOrmEntityName = new LinkedHashMap<>();

		HibernateOrmBasicTypeMetadataProvider build() {
			return new HibernateOrmBasicTypeMetadataProvider( this );
		}
	}

}