/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.common.SearchManagerBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoMappingImpl;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoTypeManagerContainer;
import org.hibernate.search.mapper.pojo.model.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;
import org.hibernate.search.mapper.pojo.processing.impl.ProvidedToStringIdentifierConverter;


/**
 * @author Yoann Rodiere
 */
public class PojoMappingBuilder implements MappingBuilder<PojoTypeNodeMetadataContributor, SearchManagerBuilder<PojoSearchManager>> {

	private final PojoIntrospector introspector;
	private final PojoProxyIntrospector proxyIntrospector;
	private final boolean implicitProvidedId;

	private final List<PojoTypeManagerBuilder<?, ?>> typeManagerBuilders = new ArrayList<>();

	public PojoMappingBuilder(PojoIntrospector introspector, PojoProxyIntrospector proxyIntrospector, boolean implicitProvidedId) {
		this.introspector = introspector;
		this.proxyIntrospector = proxyIntrospector;
		this.implicitProvidedId = implicitProvidedId;
	}

	@Override
	public void addIndexed(IndexedTypeIdentifier typeId,
			IndexManagerBuildingState<?> indexManagerBuildingState,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider) {
		PojoIndexedTypeIdentifier pojoTypeId = (PojoIndexedTypeIdentifier) typeId;
		Class<?> javaType = pojoTypeId.toJavaType();
		PojoTypeManagerBuilder<?, ?> builder = new PojoTypeManagerBuilder<>(
				javaType, introspector, proxyIntrospector, indexManagerBuildingState, contributorProvider,
				implicitProvidedId ? ProvidedToStringIdentifierConverter.get() : null );
		PojoTypeNodeMappingCollector collector = builder.asCollector();
		contributorProvider.get( pojoTypeId ).forEach( c -> c.contributeMapping( collector ) );
		typeManagerBuilders.add( builder );
	}

	@Override
	public PojoMappingImpl build() {
		PojoTypeManagerContainer.Builder typeManagersBuilder = PojoTypeManagerContainer.builder();
		typeManagerBuilders.forEach( b -> b.addTo( typeManagersBuilder ) );
		return new PojoMappingImpl( proxyIntrospector, typeManagersBuilder.build() );
	}

}
