/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.reindexonupdate.no.correct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ReindexOnUpdateNoIT {

	public static List<? extends Arguments> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				mapping -> {
					//tag::programmatic[]
					TypeMappingStep sensorMapping = mapping.type( Sensor.class );
					sensorMapping.indexed();
					sensorMapping.property( "name" )
							.fullTextField();
					sensorMapping.property( "status" )
							.keywordField();
					sensorMapping.property( "rollingAverage" )
							.genericField()
							.indexingDependency().reindexOnUpdate( ReindexOnUpdate.NO );
					//end::programmatic[]
				} );
	}

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );
	private EntityManagerFactory entityManagerFactory;

	public void init(DocumentationSetupHelper.SetupVariant variant) {
		entityManagerFactory = setupHelper.start( variant )
				.setup( Sensor.class );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void reindexOnUpdateNo(DocumentationSetupHelper.SetupVariant variant) {
		init( variant );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			for ( int i = 0; i < 2000; ++i ) {
				Sensor sensor = new Sensor();
				sensor.setId( i );
				sensor.setName( "Sensor " + i );
				sensor.setStatus( SensorStatus.ONLINE );
				sensor.setValue( 1.0 );
				sensor.setRollingAverage( 1.0 );
				entityManager.persist( sensor );
			}
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			assertThat( countSensorsWithinOperatingParameters( entityManager ) )
					.isEqualTo( 2000L );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Sensor sensor = entityManager.getReference( Sensor.class, 50 );
			sensor.setStatus( SensorStatus.OFFLINE );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// The sensor was reindexed, as expected.
			assertThat( countSensorsWithinOperatingParameters( entityManager ) )
					.isEqualTo( 1999L );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Sensor sensor = entityManager.getReference( Sensor.class, 70 );
			sensor.setRollingAverage( 0.5 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// The sensor was *not* been reindexed, as expected.
			assertThat( countSensorsWithinOperatingParameters( entityManager ) )
					.isEqualTo( 1999L );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Sensor sensor = entityManager.getReference( Sensor.class, 70 );
			Search.session( entityManager ).indexingPlan().addOrUpdate( sensor );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			// The sensor was reindexed, as expected.
			assertThat( countSensorsWithinOperatingParameters( entityManager ) )
					.isEqualTo( 1998L );
		} );
	}

	private long countSensorsWithinOperatingParameters(EntityManager entityManager) {
		return Search.session( entityManager ).search( Sensor.class )
				.where( f -> f.and(
						f.match().field( "status" ).matching( SensorStatus.ONLINE ),
						f.range().field( "rollingAverage" ).between( 0.9, 1.1 ) ) )
				.fetchTotalHitCount();
	}

}
