/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.spatial.geopointbinding.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GeoPointBindingTypeIT {

	public static List<? extends Arguments> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				mapping -> {
					//tag::programmatic[]
					TypeMappingStep authorMapping = mapping.type( Author.class );
					authorMapping.indexed();
					authorMapping.binder( GeoPointBinder.create().fieldName( "placeOfBirth" ) );
					authorMapping.property( "placeOfBirthLatitude" )
							.marker( GeoPointBinder.latitude() );
					authorMapping.property( "placeOfBirthLongitude" )
							.marker( GeoPointBinder.longitude() );
					//end::programmatic[]
				} );
	}

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );
	private EntityManagerFactory entityManagerFactory;

	public void init(DocumentationSetupHelper.SetupVariant variant) {
		entityManagerFactory = setupHelper.start( variant )
				.setup( Author.class );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void smoke(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Author author = new Author();
			author.setName( "Isaac Asimov" );
			author.setPlaceOfBirthLatitude( 53.976177 );
			author.setPlaceOfBirthLongitude( 32.158627 );

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
