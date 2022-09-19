/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.composing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ProjectionBinderComposingIT {
	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple(),
			true,
			// Since we disable classpath scanning in tests for performance reasons,
			// we need to register annotated projection types explicitly.
			// This wouldn't be needed in a typical application.
			context -> {
				context.annotationMapping()
						.add( MyBookProjection.class )
						.add( MyBookProjection.MyAuthorProjection.class );
			}
	);

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.setup( Book.class, Author.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Author author = new Author();
			author.setName( "Isaac Asimov" );
			entityManager.persist( author );

			Book book = new Book();
			book.setAuthor( author );
			author.getBooks().add( book );
			entityManager.persist( book );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			//tag::call[]
			List<MyBookProjection> hits = searchSession.search( Book.class )
					.select( MyBookProjection.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			//end::call[]

			assertThat( hits ).containsExactlyInAnyOrderElementsOf(
					entityManager.createQuery( "select b from Book b", Book.class ).getResultList().stream()
							.map( book -> new MyBookProjection(
									new MyBookProjection.MyAuthorProjection( book.getAuthor().getName() )
							) )
							.collect( Collectors.toList() ) );
		} );
	}

}
