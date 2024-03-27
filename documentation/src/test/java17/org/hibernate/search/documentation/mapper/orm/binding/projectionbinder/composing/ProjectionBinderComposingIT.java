/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


class ProjectionBinderComposingIT {
	@RegisterExtension
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

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start()
				.setup( Book.class, Author.class );
	}

	@Test
	void smoke() {
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
