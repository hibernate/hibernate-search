/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.bridgeresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BridgeResolverIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						MyDefaultBridgesConfigurer.class.getName()
				)
				.setup( Book.class );
	}

	@Test
	public void smoke() {
		MyProductId book1Id = new MyProductId( "oreilly", "14425" );
		ISBN book1Isbn = ISBN.parse( "978-0-58-600835-5" );
		MyProductId book2Id = new MyProductId( "largevue", "84784-484-44" );
		ISBN book2Isbn = ISBN.parse( "978-0-58-600824-5" );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book1 = new Book();
			book1.setId( book1Id );
			book1.setIsbn( book1Isbn );
			book1.setTitle( "The Caves Of Steel" );
			book1.setGenre( Genre.SCIFI );
			book1.setLocation( new MyCoordinates( 42.0, 42.0 ) );

			Book book2 = new Book();
			book2.setId( book2Id );
			book2.setIsbn( book2Isbn );
			book2.setTitle( "The Automatic Detective" );
			book2.setGenre( Genre.CRIME );
			book2.setLocation( new MyCoordinates( -42.0, -42.0 ) );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			List<ISBN> result = Search.session( entityManager ).search( Book.class )
					.select( f -> f.field( "isbn", ISBN.class ) )
					.where( f -> f.match().field( "genre" ).matching( "science", ValueConvert.NO ) )
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 )
					.containsExactly( book1Isbn );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			List<Genre> result = Search.session( entityManager ).search( Book.class )
					.select( f -> f.field( "genre", Genre.class ) )
					.where( f -> f.match().field( "title" ).matching( "steel" ) )
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 )
					.containsExactly( Genre.SCIFI );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			List<Book> result = Search.session( entityManager ).search( Book.class )
					.where( f -> f.spatial().within().field( "location" )
							.circle( 42.0, 41.0, 100, DistanceUnit.KILOMETERS ) )
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 )
					.extracting( Book::getId )
					.containsExactly( book1Id );
		} );
	}

}
