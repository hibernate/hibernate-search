/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.includepaths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class IndexedEmbeddedIncludePathsIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

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

			List<Human> result = searchSession.search( Human.class )
					.where( f -> f.and(
							f.match().field( "name" ).matching( "fourth" ),
							f.match().field( "nickname" ).matching( "babe" ),
							f.match().field( "parents.name" ).matching( "third" ),
							f.match().field( "parents.nickname" ).matching( "young" ),
							f.match().field( "parents.parents.name" ).matching( "junior" )
					) )
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 );
		} );

		SearchMapping searchMapping = Search.mapping( entityManagerFactory );

		assertThatThrownBy(
				() -> {
					searchMapping.scope( Human.class ).predicate()
							.match().field( "parents.parents.nickname" );
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" );
		assertThatThrownBy(
				() -> {
					searchMapping.scope( Human.class ).predicate()
							.match().field( "parents.parents.parents.name" );
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" );

	}

}
