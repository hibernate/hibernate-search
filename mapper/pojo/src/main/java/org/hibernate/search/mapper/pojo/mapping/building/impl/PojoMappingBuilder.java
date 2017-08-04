/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.common.SearchManagerBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMappingContributorProvider;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoMappingImpl;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoTypeManager;
import org.hibernate.search.mapper.pojo.model.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.processing.impl.ProvidedToStringIdentifierConverter;


/**
 * @author Yoann Rodiere
 */
public class PojoMappingBuilder implements MappingBuilder<PojoTypeNodeMappingCollector, SearchManagerBuilder<PojoSearchManager>> {

	private final PojoIntrospector introspector;
	private final PojoProxyIntrospector proxyIntrospector;
	private final boolean implicitProvidedId;

	private final Map<Class<?>, PojoTypeManagerBuilder<?, ?>> typeManagerBuilders = new HashMap<>();

	public PojoMappingBuilder(PojoIntrospector introspector, PojoProxyIntrospector proxyIntrospector, boolean implicitProvidedId) {
		this.introspector = introspector;
		this.proxyIntrospector = proxyIntrospector;
		this.implicitProvidedId = implicitProvidedId;
	}

	@Override
	public void addIndexed(IndexedTypeIdentifier typeId,
			IndexManagerBuildingState<?> indexManagerBuildingState,
			TypeMappingContributorProvider<PojoTypeNodeMappingCollector> contributorProvider) {
		PojoIndexedTypeIdentifier pojoTypeId = (PojoIndexedTypeIdentifier) typeId;
		Class<?> javaType = pojoTypeId.toJavaType();
		PojoTypeManagerBuilder<?, ?> builder = new PojoTypeManagerBuilder<>(
				introspector, proxyIntrospector, javaType, indexManagerBuildingState, contributorProvider,
				implicitProvidedId ? ProvidedToStringIdentifierConverter.get() : null );
		contributorProvider.get( pojoTypeId ).contribute( builder.asCollector() );
		typeManagerBuilders.put( javaType, builder );
	}

	@Override
	public PojoMappingImpl build() {
		Map<Class<?>, PojoTypeManager<?, ?, ?>> typeManagers = new HashMap<>();
		typeManagerBuilders.forEach( (key, builder) -> typeManagers.put( key, builder.build() ) );
		return new PojoMappingImpl( proxyIntrospector, typeManagers );
	}

}
