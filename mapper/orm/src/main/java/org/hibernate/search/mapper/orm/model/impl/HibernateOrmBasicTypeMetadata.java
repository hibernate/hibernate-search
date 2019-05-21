/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.model.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.property.access.spi.Getter;

class HibernateOrmBasicTypeMetadata {
	private final Class<?> javaClass;
	private final Map<String, Property> properties;
	private final Property identifierProperty;

	public static HibernateOrmBasicTypeMetadata create(PersistentClass persistentClass) {
		Map<String, Property> properties = new HashMap<>();
		collectProperties( properties, persistentClass.getPropertyIterator() );
		Property identifierProperty = persistentClass.getIdentifierProperty();
		if ( identifierProperty != null ) {
			properties.put( identifierProperty.getName(), identifierProperty );
		}
		return new HibernateOrmBasicTypeMetadata( persistentClass.getMappedClass(), properties, identifierProperty );
	}

	public static HibernateOrmBasicTypeMetadata create(Component component) {
		Map<String, Property> properties = new HashMap<>();
		collectProperties( properties, component.getPropertyIterator() );
		return new HibernateOrmBasicTypeMetadata( component.getComponentClass(), properties, null );
	}

	private static void collectProperties(Map<String, Property> collected, Iterator<Property> propertyIterator) {
		while ( propertyIterator.hasNext() ) {
			Property property = propertyIterator.next();
			collected.put( property.getName(), property );
		}
	}

	private HibernateOrmBasicTypeMetadata(Class<?> javaClass, Map<String, Property> properties,
			Property identifierProperty) {
		this.javaClass = javaClass;
		this.properties = properties;
		this.identifierProperty = identifierProperty;
	}

	HibernateOrmBasicPropertyMetadata getPropertyMetadataOrNull(String propertyName) {
		try {
			Property property = properties.get( propertyName );
			if ( property == null ) {
				return null;
			}
			Getter getter = property.getGetter( javaClass );
			boolean isId = property.equals( identifierProperty );
			return new HibernateOrmBasicPropertyMetadata( getter.getMember(), isId );
		}
		catch (MappingException e) {
			// Ignore, we just don't have any information
			return null;
		}
	}
}
