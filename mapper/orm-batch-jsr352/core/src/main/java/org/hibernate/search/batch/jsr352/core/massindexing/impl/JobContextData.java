/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.batch.jsr352.core.massindexing.util.impl.EntityTypeDescriptor;

/**
 * Container for data shared across the entire batch job.
 *
 * @author Gunnar Morling
 * @author Mincong Huang
 */
public class JobContextData {

	private EntityManagerFactory entityManagerFactory;

	/*
	 * In JSR 352 standard, only string values can be propagated using job properties, but class types are frequently
	 * used too. So this map has string keys to facilitate lookup for values extracted from job properties.
	 */
	private Map<String, EntityTypeDescriptor> entityTypeDescriptorMap;

	public JobContextData() {
		entityTypeDescriptorMap = new HashMap<>();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public void setEntityTypeDescriptors(Collection<EntityTypeDescriptor> descriptors) {
		for ( EntityTypeDescriptor descriptor : descriptors ) {
			entityTypeDescriptorMap.put( descriptor.getJavaClass().getName(), descriptor );
		}
	}

	public EntityTypeDescriptor getEntityTypeDescriptor(String entityName) {
		EntityTypeDescriptor descriptor = entityTypeDescriptorMap.get( entityName );
		if ( descriptor == null ) {
			String msg = String.format( Locale.ROOT, "entity type %s not found.", entityName );
			throw new NoSuchElementException( msg );
		}
		return descriptor;
	}

	public EntityTypeDescriptor getEntityTypeDescriptor(Class<?> entityType) {
		return getEntityTypeDescriptor( entityType.getName() );
	}

	public List<EntityTypeDescriptor> getEntityTypeDescriptors() {
		return entityTypeDescriptorMap.values().stream()
				.collect( Collectors.toList() );
	}

	public List<Class<?>> getEntityTypes() {
		return entityTypeDescriptorMap.values().stream()
				.map( EntityTypeDescriptor::getJavaClass )
				.collect( Collectors.toList() );
	}

	public Class<?> getEntityType(String entityName) {
		return getEntityTypeDescriptor( entityName ).getJavaClass();
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( "JobContextData [" )
				.append( "entityManagerFactory=" ).append( entityManagerFactory )
				.append( ", entityTypeDescriptorMap=" ).append( entityTypeDescriptorMap )
				.append( "]" )
				.toString();
	}
}
