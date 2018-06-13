/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.impl.JavaBeanMappingInitiatorImpl;
import org.hibernate.search.mapper.javabean.model.impl.JavaBeanBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.mapping.PojoMappingInitiator;

public interface JavaBeanMappingInitiator extends PojoMappingInitiator<JavaBeanMapping> {

	static JavaBeanMappingInitiator create(SearchMappingRepositoryBuilder mappingRepositoryBuilder) {
		return create( mappingRepositoryBuilder, true );
	}

	static JavaBeanMappingInitiator create(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			boolean annotatedTypeDiscoveryEnabled) {
		return create( mappingRepositoryBuilder, MethodHandles.publicLookup(), annotatedTypeDiscoveryEnabled, false );
	}

	static JavaBeanMappingInitiator create(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			boolean annotatedTypeDiscoveryEnabled, boolean multiTenancyEnabled) {
		return create(
				mappingRepositoryBuilder, MethodHandles.publicLookup(),
				annotatedTypeDiscoveryEnabled, multiTenancyEnabled
		);
	}

	static JavaBeanMappingInitiator create(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			MethodHandles.Lookup lookup, boolean annotatedTypeDiscoveryEnabled, boolean multiTenancyEnabled) {
		return new JavaBeanMappingInitiatorImpl(
				mappingRepositoryBuilder, new JavaBeanBootstrapIntrospector( lookup ),
				annotatedTypeDiscoveryEnabled, multiTenancyEnabled
		);
	}

	/**
	 * @param type The type to be considered as an entity type, i.e. a type that may be indexed
	 * and whose instances be added/updated/deleted through the {@link org.hibernate.search.mapper.pojo.mapping.PojoWorker}.
	 */
	void addEntityType(Class<?> type);

	/**
	 * @param types The types to be considered as entity types, i.e. types that may be indexed
	 * and whose instances be added/updated/deleted through the {@link org.hibernate.search.mapper.pojo.mapping.PojoWorker}.
	 */
	void addEntityTypes(Set<Class<?>> types);

}
