/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.spatial.geopointbinding.multiple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GeoPointBindingMultipleIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Author.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Author author = new Author();
			author.setName( "Isaac Asimov" );
			author.setPlaceOfBirthLatitude( 53.976177 );
			author.setPlaceOfBirthLongitude( 32.158627 );
			author.setPlaceOfDeathLatitude( 40.6464494 );
			author.setPlaceOfDeathLongitude( -73.9792831 );

			entityManager.persist( author );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Author> result = searchSession.search( Author.class )
					.where( f -> f.and(
							f.spatial().within().field( "placeOfBirth" )
									.circle( 53.970000, 32.150000, 50, DistanceUnit.KILOMETERS ),
							f.spatial().within().field( "placeOfDeath" )
									.circle( 40.6500000, -73.9800000, 50, DistanceUnit.KILOMETERS )
					) )
					.fetchAllHits();
			assertThat( result ).hasSize( 1 );
		} );
	}

}
