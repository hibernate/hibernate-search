/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.mapper.pojo.mapping.PojoMapping;
import org.hibernate.search.mapper.pojo.mapping.PojoMappingContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMapperFactory;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl.AnnotationMappingDefinitionImpl;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl.ProgrammaticMappingDefinitionImpl;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

public abstract class PojoMappingContributorImpl<M extends PojoMapping>
		implements PojoMappingContributor<M>, MetadataContributor {

	private final SearchMappingRepositoryBuilder mappingRepositoryBuilder;
	private final PojoMapperFactory<M> mapperFactory;
	private final PojoBootstrapIntrospector introspector;

	private final AnnotationMappingDefinitionImpl annotationMappingDefinition;

	private final List<MetadataContributor> delegates = new ArrayList<>();

	protected PojoMappingContributorImpl(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			MappingKey<M> mappingKey, PojoMappingFactory<M> mappingFactory,
			PojoBootstrapIntrospector introspector,
			boolean implicitProvidedId,
			boolean annotatedTypeDiscoveryEnabled,
			boolean multiTenancyEnabled) {
		this.mappingRepositoryBuilder = mappingRepositoryBuilder;
		this.mapperFactory = new PojoMapperFactory<>(
				mappingKey, mappingFactory, introspector, implicitProvidedId, multiTenancyEnabled
		);
		this.introspector = introspector;

		mappingRepositoryBuilder.addMetadataContributor( this );

		/*
		 * Make sure to create and add the annotation mapping even if the user does not call the
		 * annotationMapping() method to register annotated types explicitly,
		 * in case annotated type discovery is enabled.
		 * Also, make sure to re-use the same mapping, so as not to parse annotations on a given type twice,
		 * which would lead to duplicate field definitions.
		 */
		annotationMappingDefinition = new AnnotationMappingDefinitionImpl(
				mapperFactory, introspector, annotatedTypeDiscoveryEnabled
		);
		addMetadataContributor( annotationMappingDefinition );
	}

	@Override
	public ProgrammaticMappingDefinition programmaticMapping() {
		ProgrammaticMappingDefinitionImpl definition =
				new ProgrammaticMappingDefinitionImpl( mapperFactory, introspector );
		addMetadataContributor( definition );
		return definition;
	}

	@Override
	public AnnotationMappingDefinition annotationMapping() {
		return annotationMappingDefinition;
	}

	@Override
	public M getResult() {
		return mappingRepositoryBuilder.getBuiltResult().getMapping( mapperFactory.getMappingKey() );
	}

	@Override
	public void contribute(BuildContext buildContext, ConfigurationPropertySource propertySource,
			MetadataCollector collector) {
		for ( MetadataContributor delegate : delegates ) {
			delegate.contribute( buildContext, propertySource, collector );
		}
	}

	protected final MapperFactory<PojoTypeMetadataContributor, ?> getMapperFactory() {
		return mapperFactory;
	}

	protected final void addMetadataContributor(MetadataContributor contributor) {
		delegates.add( contributor );
	}
}
