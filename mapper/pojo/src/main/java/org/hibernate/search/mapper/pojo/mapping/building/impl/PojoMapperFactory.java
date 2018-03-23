/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.building.impl;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.mapper.pojo.mapping.PojoMapping;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

public class PojoMapperFactory<M extends PojoMapping>
		implements MapperFactory<PojoTypeMetadataContributor, M> {

	private final MappingKey<M> mappingKey;
	private final PojoMappingFactory<M> mappingFactory;
	private final PojoBootstrapIntrospector introspector;
	private final boolean implicitProvidedId;

	public PojoMapperFactory(MappingKey<M> mappingKey, PojoMappingFactory<M> mappingFactory,
			PojoBootstrapIntrospector introspector, boolean implicitProvidedId) {
		this.mappingKey = mappingKey;
		this.mappingFactory = mappingFactory;
		this.introspector = introspector;
		this.implicitProvidedId = implicitProvidedId;
	}

	@Override
	public MappingKey<M> getMappingKey() {
		return mappingKey;
	}

	@Override
	public final PojoMapper<M> createMapper(BuildContext buildContext, ConfigurationPropertySource propertySource,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider) {
		return new PojoMapper<>(
				buildContext, propertySource, contributorProvider,
				introspector, implicitProvidedId, mappingFactory::createMapping
		);
	}

}
