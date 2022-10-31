/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.entrypoints;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class StandalonePojoConfigurerIT {

	private CloseableSearchMapping theSearchMapping;

	@Rule
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	@Before
	public void setup() {
		// tag::setup[]
		CloseableSearchMapping searchMapping = SearchMapping.builder() // <1>
				.property(
						"hibernate.search.mapping.configurer", // <2>
						"class:org.hibernate.search.documentation.mapper.pojo.standalone.entrypoints.StandalonePojoConfigurer"
				)
				.property(
						"hibernate.search.backend.hosts", // <3>
						"elasticsearch.mycompany.com"
				)
				// end::setup[]
				.properties( TestConfiguration.standalonePojoMapperProperties(
						configurationProvider,
						BackendConfigurations.simple()
				) )
				// tag::setup[]
				.build(); // <4>
		// end::setup[]
		this.theSearchMapping = searchMapping;
	}

	@After
	public void cleanup() {
		if ( theSearchMapping != null ) {
			theSearchMapping.close();
		}
	}

	@Test
	public void mappingContainsExpectedEntities() {
		assertThat( theSearchMapping.allIndexedEntities() )
				.extracting( SearchIndexedEntity::name )
				.contains( "Book", "Associate", "Manager" )
		;
	}
}
