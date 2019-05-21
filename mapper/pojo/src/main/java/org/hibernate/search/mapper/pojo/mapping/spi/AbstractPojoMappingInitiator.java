/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingInitiator;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPartialBuildState;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorDefinitionContext;
import org.hibernate.search.mapper.pojo.extractor.spi.ContainerExtractorRegistry;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMapper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinitionContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl.AnnotationMappingDefinitionContextImpl;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinitionContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl.ProgrammaticMappingDefinitionContextImpl;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

public abstract class AbstractPojoMappingInitiator<MPBS extends MappingPartialBuildState>
		implements MappingInitiator<PojoTypeMetadataContributor, MPBS> {

	private final PojoMappingFactory<MPBS> mappingFactory;
	private final PojoBootstrapIntrospector introspector;

	private boolean implicitProvidedId;
	private boolean multiTenancyEnabled;

	private final AnnotationMappingDefinitionContextImpl annotationMappingDefinition;

	private final ContainerExtractorRegistry.Builder containerExtractorRegistryBuilder;

	private final List<PojoMappingConfigurationContributor> delegates = new ArrayList<>();

	protected AbstractPojoMappingInitiator(PojoMappingFactory<MPBS> mappingFactory,
			PojoBootstrapIntrospector introspector) {
		this.mappingFactory = mappingFactory;
		this.introspector = introspector;

		/*
		 * Make sure to create and add the annotation mapping even if the user does not call the
		 * annotationMapping() method to register annotated types explicitly,
		 * in case annotated type discovery is enabled.
		 * Also, make sure to re-use the same mapping, so as not to parse annotations on a given type twice,
		 * which would lead to duplicate field definitions.
		 */
		annotationMappingDefinition = new AnnotationMappingDefinitionContextImpl( introspector );
		addConfigurationContributor( annotationMappingDefinition );

		containerExtractorRegistryBuilder = ContainerExtractorRegistry.builder();
	}

	public ProgrammaticMappingDefinitionContext programmaticMapping() {
		ProgrammaticMappingDefinitionContextImpl definition = new ProgrammaticMappingDefinitionContextImpl( introspector );
		addConfigurationContributor( definition );
		return definition;
	}

	public AnnotationMappingDefinitionContext annotationMapping() {
		return annotationMappingDefinition;
	}

	public ContainerExtractorDefinitionContext containerExtractors() {
		return containerExtractorRegistryBuilder;
	}

	public void setImplicitProvidedId(boolean implicitProvidedId) {
		this.implicitProvidedId = implicitProvidedId;
	}

	public void setMultiTenancyEnabled(boolean multiTenancyEnabled) {
		this.multiTenancyEnabled = multiTenancyEnabled;
	}

	public void setAnnotatedTypeDiscoveryEnabled(boolean annotatedTypeDiscoveryEnabled) {
		annotationMappingDefinition.setAnnotatedTypeDiscoveryEnabled( annotatedTypeDiscoveryEnabled );
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
		if ( multiTenancyEnabled ) {
			configurationCollector.enableMultiTenancy();
		}
		for ( PojoMappingConfigurationContributor delegate : delegates ) {
			delegate.configure( buildContext, configurationCollector );
		}
	}

	@Override
	public Mapper<MPBS> createMapper(MappingBuildContext buildContext,
			TypeMetadataContributorProvider<PojoTypeMetadataContributor> contributorProvider) {
		return new PojoMapper<>(
				buildContext, contributorProvider,
				introspector,
				containerExtractorRegistryBuilder.build(),
				implicitProvidedId,
				mappingFactory::createMapping
		);
	}

	protected final void addConfigurationContributor(PojoMappingConfigurationContributor contributor) {
		delegates.add( contributor );
	}
}
