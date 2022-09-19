/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.identifierbridge.compatible;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.List;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class IdentifierBridgeCompatibleIT {
	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, Magazine.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.getId().setPublisherId( 1L );
			book.getId().setPublisherSpecificBookId( 42L );
			entityManager.persist( book );

			Magazine magazine = new Magazine();
			magazine.getId().setPublisherId( 2L );
			magazine.getId().setPublisherSpecificBookId( 42L );
			entityManager.persist( magazine );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Object> result = searchSession.search( Arrays.asList( Book.class, Magazine.class ) )
					.where( f -> f.id().matching( new BookOrMagazineId( 1L, 42L ) ) )
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 )
					.allSatisfy( b -> assertThat( b ).isInstanceOf( Book.class ) );
		} );
	}

}
