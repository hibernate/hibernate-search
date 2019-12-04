/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.hibernate.boot.Metadata;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;

@SuppressWarnings( "unchecked" ) // Hibernate ORM gives us raw types, we must make do.
public class HibernateOrmBasicTypeMetadataProvider {

	public static HibernateOrmBasicTypeMetadataProvider create(Metadata metadata) {
		Collection<PersistentClass> persistentClasses = metadata.getEntityBindings()
				.stream()
				.filter( PersistentClass::hasPojoRepresentation )
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
		metadataProviderBuilder.persistentClasses.put( persistentClass.getEntityName(), persistentClass );
		metadataProviderBuilder.typeMetadata.put(
				persistentClass.getMappedClass(), HibernateOrmBasicTypeMetadata.create( persistentClass ) );
		collectEmbeddedTypesRecursively( metadataProviderBuilder, persistentClass.getIdentifier() );
		collectEmbeddedTypesRecursively( metadataProviderBuilder, persistentClass.getPropertyIterator() );
	}

	private static void collectEmbeddedTypesRecursively(Builder metadataProviderBuilder,
			Iterator<Property> propertyIterator) {
		while ( propertyIterator.hasNext() ) {
			Property property = propertyIterator.next();
			collectEmbeddedTypesRecursively( metadataProviderBuilder, property.getValue() );
		}
	}

	private static void collectEmbeddedTypesRecursively(Builder metadataProviderBuilder, Value value) {
		if ( value instanceof Component ) {
			Component component = (Component) value;
			// We don't care about duplicates, we assume they are all the same regarding the information we need
			metadataProviderBuilder.typeMetadata.computeIfAbsent(
					component.getComponentClass(),
					ignored -> HibernateOrmBasicTypeMetadata.create( component )
			);
			// Recurse
			collectEmbeddedTypesRecursively( metadataProviderBuilder, component.getPropertyIterator() );
		}
		else if ( value instanceof org.hibernate.mapping.Collection ) {
			org.hibernate.mapping.Collection collection = (org.hibernate.mapping.Collection) value;
			// Recurse
			collectEmbeddedTypesRecursively( metadataProviderBuilder, collection.getElement() );
			if ( collection instanceof IndexedCollection ) {
				IndexedCollection indexedCollection = (IndexedCollection) collection;
				/*
				 * Do not let ORM confuse you: getKey() doesn't return the value of the map key,
				 * but the value of the foreign key to the targeted entity...
				 * We need to call getIndex() to retrieve the value of the map key.
				 */
				collectEmbeddedTypesRecursively( metadataProviderBuilder, indexedCollection.getIndex() );
			}
		}
	}

	private final Map<String, PersistentClass> persistentClasses;
	private final Map<Class<?>, HibernateOrmBasicTypeMetadata> typeMetadata;

	private HibernateOrmBasicTypeMetadataProvider(Builder builder) {
		this.persistentClasses = builder.persistentClasses;
		this.typeMetadata = builder.typeMetadata;
	}

	public Collection<PersistentClass> getPersistentClasses() {
		return persistentClasses.values();
	}

	public PersistentClass getPersistentClass(String hibernateOrmEntityName) {
		return persistentClasses.get( hibernateOrmEntityName );
	}

	HibernateOrmBasicTypeMetadata getBasicTypeMetadata(Class<?> clazz) {
		return typeMetadata.get( clazz );
	}

	private static class Builder {
		private final Map<String, PersistentClass> persistentClasses = new LinkedHashMap<>();
		private final Map<Class<?>, HibernateOrmBasicTypeMetadata> typeMetadata = new LinkedHashMap<>();

		HibernateOrmBasicTypeMetadataProvider build() {
			return new HibernateOrmBasicTypeMetadataProvider( this );
		}
	}
}