/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.reindexonupdate.no.incorrect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ReindexOnUpdateNoIncorrectIT {

	@Rule
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Sensor.class );
	}

	@Test
	public void missingReindexOnUpdateNo() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
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
			// The sensor was reindexed! That won't perform well
			// if we update the rolling average every few milliseconds on all sensors...
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
