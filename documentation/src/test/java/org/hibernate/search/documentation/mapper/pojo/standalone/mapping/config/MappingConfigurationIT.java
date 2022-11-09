/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.mapping.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMappingBuilder;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MappingConfigurationIT {

	@Rule
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private CloseableSearchMapping searchMapping;

	@Before
	public void setup() {
		// tag::setup[]
		SearchMappingBuilder builder = SearchMapping.builder()
				// end::setup[]
				.properties( TestConfiguration.standalonePojoMapperProperties(
						configurationProvider,
						BackendConfigurations.simple()
				) )
				// tag::setup[]
				.property(
						"hibernate.search.mapping.configurer",
						(StandalonePojoMappingConfigurer) context -> {
							context.addEntityType( Book.class ); // <1>

							context.annotationMapping() // <2>
									.discoverAnnotationsFromReferencedTypes( false )
									.discoverAnnotatedTypesFromRootMappingAnnotations( false );

							ProgrammaticMappingConfigurationContext mappingContext = context.programmaticMapping(); // <3>
							TypeMappingStep bookMapping = mappingContext.type( Book.class );
							bookMapping.indexed();
							bookMapping.property( "id" ).documentId();
							bookMapping.property( "title" )
									.fullTextField().analyzer( "english" );
						}
				);

		CloseableSearchMapping searchMapping = builder.build();
		// end::setup[]
		this.searchMapping = searchMapping;
	}

	@After
	public void cleanup() {
		if ( searchMapping != null ) {
			searchMapping.close();
		}
	}

	@Test
	public void simple() {
		try ( SearchSession session = searchMapping.createSessionWithOptions()
				.refreshStrategy( DocumentRefreshStrategy.FORCE )
				.build() ) {
			Book book = new Book();
			book.setId( 1 );
			book.setTitle( "The Caves Of Steel" );

			session.indexingPlan().add( book );
		}

		try ( SearchSession session = searchMapping.createSession() ) {
			List<Integer> result = session.search( Book.class )
					.select( f -> f.id( Integer.class ) )
					.where( f -> f.match().field( "title" ).matching( "steel" ) )
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 );
		}
	}

}
