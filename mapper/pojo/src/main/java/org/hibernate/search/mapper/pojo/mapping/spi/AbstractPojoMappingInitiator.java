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
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorConfigurationContext;
import org.hibernate.search.mapper.pojo.extractor.spi.ContainerExtractorRegistry;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoMapper;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperDelegate;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl.AnnotationMappingConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl.ProgrammaticMappingConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;

public abstract class AbstractPojoMappingInitiator<MPBS extends MappingPartialBuildState>
		implements MappingInitiator<PojoTypeMetadataContributor, MPBS> {

	private final PojoBootstrapIntrospector introspector;

	private boolean implicitProvidedId;
	private boolean multiTenancyEnabled;

	private final AnnotationMappingConfigurationContextImpl annotationMappingConfiguration;

	private final ContainerExtractorRegistry.Builder containerExtractorRegistryBuilder;

	private final List<PojoMappingConfigurationContributor> delegates = new ArrayList<>();

	protected AbstractPojoMappingInitiator(PojoBootstrapIntrospector introspector) {
		this.introspector = introspector;

		/*
		 * Make sure to create and add the annotation mapping even if the user does not call the
		 * annotationMapping() method to register annotated types explicitly,
		 * in case annotated type discovery is enabled.
		 * Also, make sure to re-use the same mapping, so as not to parse annotations on a given type twice,
		 * which would lead to duplicate field definitions.
		 */
		annotationMappingConfiguration = new AnnotationMappingConfigurationContextImpl( introspector );
		addConfigurationContributor( annotationMappingConfiguration );

		containerExtractorRegistryBuilder = ContainerExtractorRegistry.builder();
	}

	public ProgrammaticMappingConfigurationContext programmaticMapping() {
		ProgrammaticMappingConfigurationContextImpl context = new ProgrammaticMappingConfigurationContextImpl( introspector );
		addConfigurationContributor( context );
		return context;
	}

	public AnnotationMappingConfigurationContext annotationMapping() {
		return annotationMappingConfiguration;
	}

	public ContainerExtractorConfigurationContext containerExtractors() {
		return containerExtractorRegistryBuilder;
	}

	public void setImplicitProvidedId(boolean implicitProvidedId) {
		this.implicitProvidedId = implicitProvidedId;
	}

	public void setMultiTenancyEnabled(boolean multiTenancyEnabled) {
		this.multiTenancyEnabled = multiTenancyEnabled;
	}

	public void setAnnotatedTypeDiscoveryEnabled(boolean annotatedTypeDiscoveryEnabled) {
		annotationMappingConfiguration.setAnnotatedTypeDiscoveryEnabled( annotatedTypeDiscoveryEnabled );
	}

	@Override
	public void configure(MappingBuildContext buildContext,
			MappingConfigurationCollector<PojoTypeMetadataContributor> configurationCollector) {
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
				multiTenancyEnabled,
				createMapperDelegate()
		);
	}

	protected abstract PojoMapperDelegate<MPBS> createMapperDelegate();

	protected final void addConfigurationContributor(PojoMappingConfigurationContributor contributor) {
		delegates.add( contributor );
	}
}
