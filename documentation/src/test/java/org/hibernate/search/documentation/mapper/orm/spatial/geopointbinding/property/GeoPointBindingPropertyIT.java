/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.spatial.geopointbinding.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class GeoPointBindingPropertyIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Author.class, MyCoordinates.class );
	}

	@Test
	void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Author author = new Author();
			author.setName( "Isaac Asimov" );
			author.setPlaceOfBirth( new MyCoordinates( 53.976177, 32.158627 ) );

			entityManager.persist( author );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Author> result = searchSession.search( Author.class )
					.where( f -> f.spatial().within().field( "placeOfBirth" )
							.circle( 53.970000, 32.150000, 50, DistanceUnit.KILOMETERS ) )
					.fetchAllHits();
			assertThat( result ).hasSize( 1 );
		} );
	}

}
