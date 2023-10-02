/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.projection.filters.includepaths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.Collections;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.search.projection.filters.Human;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ProjectionConstructorObjectProjectionIncludePathsIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple(), true,
			// Since we disable classpath scanning in tests for performance reasons,
			// we need to register annotated projection types explicitly.
			// This wouldn't be needed in a typical application.
			context -> context.annotationMapping().add( HumanProjection.class )
	);

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Human.class );
	}

	@Test
	void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Human human1 = new Human();
			human1.setId( 1 );
			human1.setName( "George Bush Senior" );
			human1.setNickname( "The Ancient" );

			Human human2 = new Human();
			human2.setId( 2 );
			human2.setName( "George Bush Junior" );
			human2.setNickname( "The Old" );
			human1.getChildren().add( human2 );
			human2.getParents().add( human1 );

			Human human3 = new Human();
			human3.setId( 3 );
			human3.setName( "George Bush The Third" );
			human3.setNickname( "The Young" );
			human2.getChildren().add( human3 );
			human3.getParents().add( human2 );

			Human human4 = new Human();
			human4.setId( 4 );
			human4.setName( "George Bush The Fourth" );
			human4.setNickname( "The Babe" );
			human3.getChildren().add( human4 );
			human4.getParents().add( human3 );

			entityManager.persist( human1 );
			entityManager.persist( human2 );
			entityManager.persist( human3 );
			entityManager.persist( human4 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			assertThat( searchSession.search( Human.class )
					.select( HumanProjection.class )
					.where( f -> f.id().matching( 4 ) )
					.fetchHits( 20 ) )
					.containsExactly( new HumanProjection(
							"George Bush The Fourth", "The Babe",
							Arrays.asList( new HumanProjection(
									"George Bush The Third", "The Young",
									Arrays.asList( new HumanProjection(
											"George Bush Junior", null,
											Collections.emptyList()
									) )
							) )
					) );
		} );
	}

}
