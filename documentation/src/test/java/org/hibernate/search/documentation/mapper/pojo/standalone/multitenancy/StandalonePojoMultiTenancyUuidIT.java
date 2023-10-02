/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.pojo.standalone.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.TestConfiguration;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.tenancy.TenantIdentifierConverter;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StandalonePojoMultiTenancyUuidIT {

	@RegisterExtension
	public TestConfigurationProvider configurationProvider = new TestConfigurationProvider();

	private CloseableSearchMapping theSearchMapping;

	@BeforeEach
	void setup() {
		// tag::setup[]
		CloseableSearchMapping searchMapping = SearchMapping.builder( AnnotatedTypeSource.fromClasses( // <1>
				Book.class
		) )
				// ...
				.property( "hibernate.search.mapping.multi_tenancy.enabled", true ) // <2>
				.property(
						"hibernate.search.mapping.multi_tenancy.tenant_identifier_converter",
						new TenantIdentifierConverter() {
							@Override
							public String toStringValue(Object tenantId) {
								return tenantId == null ? null : tenantId.toString();
							}

							@Override
							public Object fromStringValue(String tenantId) {
								return tenantId == null ? null : UUID.fromString( tenantId );
							}
						}
				) // <3>
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

	@AfterEach
	void cleanup() {
		if ( theSearchMapping != null ) {
			theSearchMapping.close();
		}
	}

	@Test
	void test() {
		// tag::searchSession[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::searchSession[]
				theSearchMapping;
		// tag::searchSession[]
		Object tenantId = UUID.randomUUID();
		try ( SearchSession searchSession = searchMapping.createSessionWithOptions() // <2>
				.tenantId( tenantId ) // <3>
				.build() ) { // <4>
			// ...
			// end::searchSession[]
			assertThat( searchSession.tenantIdentifierValue() ).isEqualTo( tenantId );
			// tag::searchSession[]
		}
		// end::searchSession[]
	}

}
