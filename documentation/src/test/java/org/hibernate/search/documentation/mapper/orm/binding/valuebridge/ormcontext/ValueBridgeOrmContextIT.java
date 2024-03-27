/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.valuebridge.ormcontext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ValueBridgeOrmContextIT {
	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( MyEntity.class );
	}

	@Test
	void smoke() {
		// See MyDataValueBridge
		entityManagerFactory.getProperties().put( "test.data.indexed", MyData.INDEXED );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			MyEntity myEntity = new MyEntity();
			entityManager.persist( myEntity );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// See MyDataValueBridge
			entityManager.setProperty( "test.data.projected", MyData.PROJECTED );

			SearchSession searchSession = Search.session( entityManager );

			List<MyData> result = searchSession.search( MyEntity.class )
					.select( f -> f.field( "myData", MyData.class ) )
					.where( f -> f.match().field( "myData" )
							.matching( "INDEXED", ValueConvert.NO ) )
					.fetchHits( 20 );

			assertThat( result ).containsExactly( MyData.PROJECTED );
		} );
	}

}
