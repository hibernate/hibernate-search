/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.reindexonupdate.no.correct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinJPATransaction;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ReindexOnUpdateNoIT {

	@Parameterized.Parameters(name = "{0}")
	public static List<?> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				BackendConfigurations.simple(),
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

	@Parameterized.Parameter
	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Sensor.class );
	}

	@Test
	public void reindexOnUpdateNo() {
		withinJPATransaction( entityManagerFactory, entityManager -> {
			for ( int i = 0 ; i < 2000 ; ++i ) {
				Sensor sensor = new Sensor();
				sensor.setId( i );
				sensor.setName( "Sensor " + i );
				sensor.setStatus( SensorStatus.ONLINE );
				sensor.setValue( 1.0 );
				sensor.setRollingAverage( 1.0 );
				entityManager.persist( sensor );
			}
		} );

		withinJPATransaction( entityManagerFactory, entityManager -> {
			assertThat( countSensorsWithinOperatingParameters( entityManager ) )
					.isEqualTo( 2000L );
		} );

		withinJPATransaction( entityManagerFactory, entityManager -> {
			Sensor sensor = entityManager.getReference( Sensor.class, 50 );
			sensor.setStatus( SensorStatus.OFFLINE );
		} );

		withinJPATransaction( entityManagerFactory, entityManager -> {
			// The sensor was reindexed, as expected.
			assertThat( countSensorsWithinOperatingParameters( entityManager ) )
					.isEqualTo( 1999L );
		} );

		withinJPATransaction( entityManagerFactory, entityManager -> {
			Sensor sensor = entityManager.getReference( Sensor.class, 70 );
			sensor.setRollingAverage( 0.5 );
		} );

		withinJPATransaction( entityManagerFactory, entityManager -> {
			// The sensor was *not* been reindexed, as expected.
			assertThat( countSensorsWithinOperatingParameters( entityManager ) )
					.isEqualTo( 1999L );
		} );

		withinJPATransaction( entityManagerFactory, entityManager -> {
			Sensor sensor = entityManager.getReference( Sensor.class, 70 );
			Search.session( entityManager ).indexingPlan().addOrUpdate( sensor );
		} );

		withinJPATransaction( entityManagerFactory, entityManager -> {
			// The sensor was reindexed, as expected.
			assertThat( countSensorsWithinOperatingParameters( entityManager ) )
					.isEqualTo( 1998L );
		} );
	}

	private long countSensorsWithinOperatingParameters(EntityManager entityManager) {
		return Search.session( entityManager ).search( Sensor.class )
				.where( f -> f.bool()
						.must( f.match().field( "status" ).matching( SensorStatus.ONLINE ) )
						.must( f.range().field( "rollingAverage" ).between( 0.9, 1.1 ) ) )
				.fetchTotalHitCount();
	}

}
