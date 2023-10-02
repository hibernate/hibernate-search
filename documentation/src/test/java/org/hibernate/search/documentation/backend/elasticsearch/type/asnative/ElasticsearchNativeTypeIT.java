/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.elasticsearch.type.asnative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchNativeTypeIT {
	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( CompanyServer.class );
	}

	@Test
	void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			CompanyServer companyServer = new CompanyServer();
			companyServer.setIpAddress( "192.168.10.2" );
			entityManager.persist( companyServer );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<CompanyServer> result = searchSession.search( CompanyServer.class )
					.extension( ElasticsearchExtension.get() )
					.where( f -> f.fromJson(
							"{\n" +
									"  \"term\": {\n" +
									"    \"ipAddress\": \"192.168.0.0/16\"\n" +
									"  }\n" +
									"}"
					) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
