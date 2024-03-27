/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.jakarta.batch.core.massindexing.util.impl.EntityTypeDescriptor;
import org.hibernate.search.mapper.orm.tenancy.spi.TenancyConfiguration;

/**
 * Container for data shared across the entire batch job.
 *
 * @author Gunnar Morling
 * @author Mincong Huang
 */
public class JobContextData {

	private EntityManagerFactory entityManagerFactory;

	/*
	 * In Jakarta Batch standard, only string values can be propagated using job properties, but class types are frequently
	 * used too. So this map has string keys to facilitate lookup for values extracted from job properties.
	 */
	private Map<String, EntityTypeDescriptor<?, ?>> entityTypeDescriptorMap;

	private TenancyConfiguration tenancyConfiguration;

	public JobContextData() {
		entityTypeDescriptorMap = new HashMap<>();
	}

	public EntityManagerFactory getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public void setEntityTypeDescriptors(Collection<EntityTypeDescriptor<?, ?>> descriptors) {
		for ( EntityTypeDescriptor<?, ?> descriptor : descriptors ) {
			entityTypeDescriptorMap.put( descriptor.jpaEntityName(), descriptor );
		}
	}

	public void setTenancyConfiguration(TenancyConfiguration tenancyConfiguration) {
		this.tenancyConfiguration = tenancyConfiguration;
	}

	public TenancyConfiguration getTenancyConfiguration() {
		return tenancyConfiguration;
	}

	public EntityTypeDescriptor<?, ?> getEntityTypeDescriptor(String entityName) {
		EntityTypeDescriptor<?, ?> descriptor = entityTypeDescriptorMap.get( entityName );
		if ( descriptor == null ) {
			String msg = String.format( Locale.ROOT, "entity type %s not found.", entityName );
			throw new NoSuchElementException( msg );
		}
		return descriptor;
	}

	public List<EntityTypeDescriptor<?, ?>> getEntityTypeDescriptors() {
		return new ArrayList<>( entityTypeDescriptorMap.values() );
	}

	public List<Class<?>> getEntityTypes() {
		return entityTypeDescriptorMap.values().stream()
				.map( EntityTypeDescriptor::javaClass )
				.collect( Collectors.toList() );
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
