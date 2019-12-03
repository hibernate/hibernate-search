/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
				.collect( Collectors.toList() );
		Map<Class<?>, HibernateOrmBasicTypeMetadata> typeMetadata = new HashMap<>();
		collectPersistentTypes( typeMetadata, persistentClasses );
		for ( PersistentClass persistentClass : persistentClasses ) {
			collectEmbeddedTypesRecursively( typeMetadata, persistentClass.getIdentifier() );
			collectEmbeddedTypesRecursively( typeMetadata, persistentClass.getPropertyIterator() );
		}

		return new HibernateOrmBasicTypeMetadataProvider( typeMetadata );
	}

	private static void collectPersistentTypes(Map<Class<?>, HibernateOrmBasicTypeMetadata> collected, Collection<PersistentClass> persistentClasses) {
		for ( PersistentClass persistentClass : persistentClasses ) {
			collected.put( persistentClass.getMappedClass(), HibernateOrmBasicTypeMetadata.create( persistentClass ) );
		}
	}

	private static void collectEmbeddedTypesRecursively(Map<Class<?>, HibernateOrmBasicTypeMetadata> collected, Iterator<Property> propertyIterator) {
		while ( propertyIterator.hasNext() ) {
			Property property = propertyIterator.next();
			collectEmbeddedTypesRecursively( collected, property.getValue() );
		}
	}

	private static void collectEmbeddedTypesRecursively(Map<Class<?>, HibernateOrmBasicTypeMetadata> collected, Value value) {
		if ( value instanceof Component ) {
			Component component = (Component) value;
			// We don't care about duplicates, we assume they are all the same regarding the information we need
			collected.computeIfAbsent(
					component.getComponentClass(),
					ignored -> HibernateOrmBasicTypeMetadata.create( component )
			);
			// Recurse
			collectEmbeddedTypesRecursively( collected, component.getPropertyIterator() );
		}
		else if ( value instanceof org.hibernate.mapping.Collection ) {
			org.hibernate.mapping.Collection collection = (org.hibernate.mapping.Collection) value;
			// Recurse
			collectEmbeddedTypesRecursively( collected, collection.getElement() );
			if ( collection instanceof IndexedCollection ) {
				IndexedCollection indexedCollection = (IndexedCollection) collection;
				/*
				 * Do not let ORM confuse you: getKey() doesn't return the value of the map key,
				 * but the value of the foreign key to the targeted entity...
				 * We need to call getIndex() to retrieve the value of the map key.
				 */
				collectEmbeddedTypesRecursively( collected, indexedCollection.getIndex() );
			}
		}
	}

	private final Map<Class<?>, HibernateOrmBasicTypeMetadata> typeMetadata;

	private HibernateOrmBasicTypeMetadataProvider(Map<Class<?>, HibernateOrmBasicTypeMetadata> typeMetadata) {
		this.typeMetadata = typeMetadata;
	}

	HibernateOrmBasicTypeMetadata getBasicTypeMetadata(Class<?> clazz) {
		return typeMetadata.get( clazz );
	}
}
