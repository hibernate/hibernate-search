/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoMappingDelegateImpl;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoTypeManagerContainer;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.TypeModel;
import org.hibernate.search.mapper.pojo.processing.impl.ProvidedToStringIdentifierConverter;


/**
 * @author Yoann Rodiere
 */
public class PojoMapper<M extends MappingImplementor> implements Mapper<PojoTypeNodeMetadataContributor, M> {

	private final ConfigurationPropertySource propertySource;
	private final PojoIntrospector introspector;
	private final boolean implicitProvidedId;
	private final BiFunction<ConfigurationPropertySource, PojoMappingDelegate, M> wrapperFactory;

	private final List<PojoTypeManagerBuilder<?, ?>> typeManagerBuilders = new ArrayList<>();

	public PojoMapper(
			ConfigurationPropertySource propertySource,
			PojoIntrospector introspector,
			boolean implicitProvidedId,
			BiFunction<ConfigurationPropertySource, PojoMappingDelegate, M> wrapperFactory) {
		this.propertySource = propertySource;
		this.introspector = introspector;
		this.implicitProvidedId = implicitProvidedId;
		this.wrapperFactory = wrapperFactory;
	}

	@Override
	public void addIndexed(IndexedTypeIdentifier typeId,
			IndexManagerBuildingState<?> indexManagerBuildingState,
			TypeMetadataContributorProvider<PojoTypeNodeMetadataContributor> contributorProvider) {
		PojoIndexedTypeIdentifier pojoTypeId = (PojoIndexedTypeIdentifier) typeId;
		Class<?> javaType = pojoTypeId.toJavaType();
		TypeModel<?> typeModel = introspector.getEntityTypeModel( javaType );
		PojoTypeManagerBuilder<?, ?> builder = new PojoTypeManagerBuilder<>(
				typeModel, indexManagerBuildingState, contributorProvider,
				implicitProvidedId ? ProvidedToStringIdentifierConverter.get() : null );
		PojoTypeNodeMappingCollector collector = builder.asCollector();
		contributorProvider.get( pojoTypeId ).forEach( c -> c.contributeMapping( collector ) );
		typeManagerBuilders.add( builder );
	}

	@Override
	public M build() {
		PojoTypeManagerContainer.Builder typeManagersBuilder = PojoTypeManagerContainer.builder();
		typeManagerBuilders.forEach( b -> b.addTo( typeManagersBuilder ) );
		PojoMappingDelegate mappingImplementor = new PojoMappingDelegateImpl( typeManagersBuilder.build() );
		return wrapperFactory.apply( propertySource, mappingImplementor );
	}

}
