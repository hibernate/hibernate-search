/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class StandalonePojoMultiTenancyIT {

	@Rule
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private CloseableSearchMapping theSearchMapping;

	@Before
	public void setup() {
		// tag::setup[]
		CloseableSearchMapping searchMapping = SearchMapping.builder() // <1>
				// ...
				.property( "hibernate.search.mapping.multi_tenancy.enabled", true ) // <2>
				// end::setup[]
				.properties( TestConfiguration.standalonePojoMapperProperties( configurationProvider,
						BackendConfigurations.simple() ) )
				.property( "hibernate.search.mapping.configurer",
						(StandalonePojoMappingConfigurer) context -> context.addEntityTypes( Book.class )
				)
				// tag::setup[]
				.build(); // <3>
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
	public void test() {
		// tag::searchSession[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::searchSession[]
				theSearchMapping;
		// tag::searchSession[]
		try ( SearchSession searchSession = searchMapping.createSessionWithOptions() // <2>
				.tenantId( "myTenantId" ) // <3>
				.build() ) { // <4>
			// ...
			// end::searchSession[]
			assertThat( searchSession.tenantIdentifier() ).isEqualTo( "myTenantId" );
			// tag::searchSession[]
		}
		// end::searchSession[]
	}

}
