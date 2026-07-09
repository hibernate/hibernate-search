/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.indexing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HibernateOrmStatelessSessionIndexingIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Test
	void statelessSession_searchAndLoadEntities() {
		EntityManagerFactory entityManagerFactory = setupHelper.start()
				.setup( Book.class, Author.class );
		SessionFactory sessionFactory = entityManagerFactory.unwrap( SessionFactory.class );

		sessionFactory.inTransaction( session -> {
			for ( int i = 0; i < 3; i++ ) {
				Book book = new Book();
				book.setId( i );
				book.setTitle( "Book #" + i );
				session.persist( book );
			}
		} );

		// tag::stateless-session-search[]
		try ( StatelessSession session = sessionFactory.openStatelessSession() ) {
			SearchSession searchSession = Search.session( session );

			List<Book> hits = searchSession.search( Book.class )
					.where( f -> f.matchAll() )
					.fetchAllHits();
			// end::stateless-session-search[]

			assertThat( hits ).hasSize( 3 );
			assertThat( hits ).extracting( Book::getTitle )
					.containsExactlyInAnyOrder( "Book #0", "Book #1", "Book #2" );
		}
	}
}
