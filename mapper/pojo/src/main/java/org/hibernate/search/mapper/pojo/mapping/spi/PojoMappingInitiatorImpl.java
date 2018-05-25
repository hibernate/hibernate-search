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
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingInitiator;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.mapper.pojo.mapping.PojoMapping;
import org.hibernate.search.mapper.pojo.mapping.PojoMappingInitiator;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMapper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl.AnnotationMappingDefinitionImpl;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl.ProgrammaticMappingDefinitionImpl;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

public abstract class PojoMappingInitiatorImpl<M extends PojoMapping>
		implements PojoMappingInitiator<M>, MappingInitiator<PojoTypeMetadataContributor, M> {

	private final SearchMappingRepositoryBuilder mappingRepositoryBuilder;
	private final MappingKey<M> mappingKey;
	private final PojoMappingFactory<M> mappingFactory;
	private final PojoBootstrapIntrospector introspector;
	private final boolean implicitProvidedId;
	private final boolean multiTenancyEnabled;

	private final AnnotationMappingDefinitionImpl annotationMappingDefinition;

	private final List<PojoMappingConfigurationContributor> delegates = new ArrayList<>();

	protected PojoMappingInitiatorImpl(SearchMappingRepositoryBuilder mappingRepositoryBuilder,
			MappingKey<M> mappingKey, PojoMappingFactory<M> mappingFactory,
			PojoBootstrapIntrospector introspector,
			boolean implicitProvidedId,
			boolean annotatedTypeDiscoveryEnabled,
			boolean multiTenancyEnabled) {
		this.mappingRepositoryBuilder = mappingRepositoryBuilder;
		this.mappingKey = mappingKey;
		this.mappingFactory = mappingFactory;
		this.introspector = introspector;
		this.implicitProvidedId = implicitProvidedId;
		this.multiTenancyEnabled = multiTenancyEnabled;

		mappingRepositoryBuilder.addMappingInitiator( this );

		/*
		 * Make sure to create and add the annotation mapping even if the user does not call the
		 * annotationMapping() method to register annotated types explicitly,
		 * in case annotated type discovery is enabled.
		 * Also, make sure to re-use the same mapping, so as not to parse annotations on a given type twice,
		 * which would lead to duplicate field definitions.
		 */
		annotationMappingDefinition = new AnnotationMappingDefinitionImpl(
				introspector, annotatedTypeDiscoveryEnabled
		);
		addConfigurationContributor( annotationMappingDefinition );
	}

	@Override
	public ProgrammaticMappingDefinition programmaticMapping() {
		ProgrammaticMappingDefinitionImpl definition = new ProgrammaticMappingDefinitionImpl( introspector );
		addConfigurationContributor( definition );
		return definition;
	}

	@Override
	public AnnotationMappingDefinition annotationMapping() {
		return annotationMappingDefinition;
	}

	@Override
	public M getResult() {
		return mappingRepositoryBuilder.getBuiltResult().getMapping( mappingKey );
	}

	@Override
	public MappingKey<M> getMappingKey() {
		return mappingKey;
	}

	@Override
	public void configure(BuildContext buildContext, ConfigurationPropertySource propertySource,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		for ( PojoMappingConfigurationContributor delegate : delegates ) {
			delegate.configure( buildContext, propertySource, configurationCollector );
		}
	}

	@Override
	public Mapper<M> createMapper(BuildContext buildContext, ConfigurationPropertySource propertySource,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider) {
		return new PojoMapper<>(
				buildContext, propertySource, contributorProvider,
				introspector, implicitProvidedId, multiTenancyEnabled, mappingFactory::createMapping
		);
	}

	protected final void addConfigurationContributor(PojoMappingConfigurationContributor contributor) {
		delegates.add( contributor );
	}
}
