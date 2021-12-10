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
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.property.access.spi.Getter;

@SuppressWarnings("unchecked") // Hibernate ORM gives us raw types, we must make do.
public class HibernateOrmBasicTypeMetadataProvider {

	public static HibernateOrmBasicTypeMetadataProvider create(Metadata metadata) {
		/*
		 * The persistent classes from Hibernate ORM are stored in a HashMap whose order is not well defined.
		 * We use a sorted map here to make iteration deterministic.
		 */
		Collection<PersistentClass> persistentClasses =
				new TreeSet<>( Comparator.comparing( PersistentClass::getEntityName ) );
		persistentClasses.addAll( metadata.getEntityBindings() );

		Builder builder = new Builder( metadata );

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

			collectClassType(
					metadataProviderBuilder, javaClass,
					persistentClass.getIdentifierProperty(), persistentClass.getPropertyIterator()
			);

			metadataProviderBuilder.typeIdentifierResolverBuilder.addClassEntityType(
					javaClass, jpaEntityName, hibernateOrmEntityName
			);
		}
		else {
			collectDynamicMapType(
					metadataProviderBuilder, hibernateOrmEntityName,
					persistentClass.getSuperclass(),
					persistentClass.getIdentifierProperty(), persistentClass.getPropertyIterator()
			);

			metadataProviderBuilder.typeIdentifierResolverBuilder.addDynamicMapEntityType(
					jpaEntityName, hibernateOrmEntityName
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
			if ( property.isSynthetic() ) {
				continue;
			}
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
			if ( property.isSynthetic() ) {
				continue;
			}
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
		// This also recurses and collects embedded types
		HibernateOrmTypeModelFactory<?> typeModelFactory =
				collectValue( metadataProviderBuilder, property.getValue() );
		collectedProperties.put(
				property.getName(),
				new HibernateOrmBasicDynamicMapPropertyMetadata( typeModelFactory )
		);
	}

	private static HibernateOrmTypeModelFactory<?> collectValue(Builder metadataProviderBuilder, Value value) {
		if ( value instanceof Component ) {
			return collectEmbedded( metadataProviderBuilder, (Component) value );
		}
		else if ( value instanceof org.hibernate.mapping.Array ) {
			org.hibernate.mapping.Array array = (org.hibernate.mapping.Array) value;
			return HibernateOrmTypeModelFactory.array(
					collectValue( metadataProviderBuilder, array.getElement() )
			);
		}
		else if ( value instanceof org.hibernate.mapping.Map ) {
			org.hibernate.mapping.Map map = (org.hibernate.mapping.Map) value;
			return HibernateOrmTypeModelFactory.map(
					map.getCollectionType().getReturnedClass(),
					/*
					 * Do not let ORM confuse you: getKey() doesn't return the value of the map key,
					 * but the value of the foreign key to the targeted entity...
					 * We need to call getIndex() to retrieve the value of the map key.
					 */
					collectValue( metadataProviderBuilder, map.getIndex() ),
					collectValue( metadataProviderBuilder, map.getElement() )
			);
		}
		else if ( value instanceof org.hibernate.mapping.Collection ) {
			org.hibernate.mapping.Collection collection = (org.hibernate.mapping.Collection) value;
			return HibernateOrmTypeModelFactory.collection(
					collection.getCollectionType().getReturnedClass(),
					collectValue( metadataProviderBuilder, collection.getElement() )
			);
		}
		else if ( value instanceof org.hibernate.mapping.ToOne ) {
			org.hibernate.mapping.ToOne toOne = (org.hibernate.mapping.ToOne) value;
			return HibernateOrmTypeModelFactory.entityReference(
					toOne.getType().getReturnedClass(), toOne.getReferencedEntityName()
			);
		}
		else if ( value instanceof org.hibernate.mapping.OneToMany ) {
			org.hibernate.mapping.OneToMany oneToMany = (org.hibernate.mapping.OneToMany) value;
			return HibernateOrmTypeModelFactory.entityReference(
					oneToMany.getType().getReturnedClass(), oneToMany.getReferencedEntityName()
			);
		}
		else {
			// Basic type (mapped to a database column)
			return HibernateOrmTypeModelFactory.rawType( value.getType().getReturnedClass() );
		}
	}

	private static HibernateOrmTypeModelFactory<?> collectEmbedded(Builder metadataProviderBuilder, Component component) {
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
			return HibernateOrmTypeModelFactory.dynamicMap( name );
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
			return HibernateOrmTypeModelFactory.rawType( javaClass );
		}
	}

	private final Metadata metadata;

	private final Map<String, PersistentClass> persistentClasses;
	private final Map<Class<?>, HibernateOrmBasicClassTypeMetadata> classTypeMetadata;
	private final Map<String, HibernateOrmBasicDynamicMapTypeMetadata> dynamicMapTypeMetadata;

	private final Map<String, String> jpaEntityNameToHibernateOrmEntityName;
	private final HibernateOrmRawTypeIdentifierResolver typeIdentifierResolver;

	private HibernateOrmBasicTypeMetadataProvider(Builder builder) {
		this.metadata = builder.metadata;
		this.persistentClasses = builder.persistentClasses;
		this.classTypeMetadata = builder.classTypeMetadata;
		this.dynamicMapTypeMetadata = builder.dynamicMapTypeMetadata;
		this.jpaEntityNameToHibernateOrmEntityName = builder.jpaEntityNameToHibernateOrmEntityName;
		this.typeIdentifierResolver = builder.typeIdentifierResolverBuilder.build();
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public Dialect getDialect() {
		return metadata.getDatabase().getDialect();
	}

	public Collection<PersistentClass> getPersistentClasses() {
		return persistentClasses.values();
	}

	public PersistentClass getPersistentClass(String hibernateOrmEntityName) {
		return persistentClasses.get( hibernateOrmEntityName );
	}

	public HibernateOrmRawTypeIdentifierResolver getTypeIdentifierResolver() {
		return typeIdentifierResolver;
	}

	HibernateOrmBasicClassTypeMetadata getBasicClassTypeMetadata(Class<?> clazz) {
		return classTypeMetadata.get( clazz );
	}

	HibernateOrmBasicDynamicMapTypeMetadata getBasicDynamicMapTypeMetadata(String name) {
		return dynamicMapTypeMetadata.get( name );
	}

	// Strangely, there is nothing of the sort in Hibernate ORM,
	// or at least nothing that would work for dynamic-map entities.
	public String getHibernateOrmEntityNameByJpaEntityName(String jpaEntityName) {
		return jpaEntityNameToHibernateOrmEntityName.get( jpaEntityName );
	}

	Set<String> getKnownDynamicMapTypeNames() {
		return dynamicMapTypeMetadata.keySet();
	}

	private static class Builder {
		private final Metadata metadata;

		private final Map<String, PersistentClass> persistentClasses = new LinkedHashMap<>();
		private final Map<Class<?>, HibernateOrmBasicClassTypeMetadata> classTypeMetadata = new LinkedHashMap<>();
		private final Map<String, HibernateOrmBasicDynamicMapTypeMetadata> dynamicMapTypeMetadata = new LinkedHashMap<>();

		private final Map<String, String> jpaEntityNameToHibernateOrmEntityName = new LinkedHashMap<>();
		private final HibernateOrmRawTypeIdentifierResolver.Builder typeIdentifierResolverBuilder =
				new HibernateOrmRawTypeIdentifierResolver.Builder();

		public Builder(Metadata metadata) {
			this.metadata = metadata;
		}

		HibernateOrmBasicTypeMetadataProvider build() {
			return new HibernateOrmBasicTypeMetadataProvider( this );
		}
	}

}
