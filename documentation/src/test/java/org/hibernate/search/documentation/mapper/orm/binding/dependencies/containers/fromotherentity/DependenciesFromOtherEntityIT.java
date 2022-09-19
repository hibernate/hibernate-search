/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.dependencies.containers.fromotherentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DependenciesFromOtherEntityIT {
	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( ScientificPaper.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			ScientificPaper paper1 = new ScientificPaper( 1 );
			paper1.setTitle(
					"Fundamental Ideas of the General Theory of Relativity and the Application of this Theory in Astronomy" );
			ScientificPaper paper2 = new ScientificPaper( 2 );
			paper2.setTitle( "On the General Theory of Relativity" );
			paper2.getReferences().add( paper1 );
			ScientificPaper paper3 = new ScientificPaper( 3 );
			paper3.setTitle( "Explanation of the Perihelion Motion of Mercury from the General Theory of Relativity" );
			paper3.getReferences().add( paper1 );
			paper3.getReferences().add( paper2 );

			entityManager.persist( paper1 );
			entityManager.persist( paper2 );
			entityManager.persist( paper3 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			assertThat( searchSession.search( ScientificPaper.class )
					.where( f -> f.match().field( "referencedBy" )
							.matching( "Field Equations Gravitation" ) )
					.fetchHits( 20 )
			)
					.hasSize( 0 );
			assertThat( searchSession.search( ScientificPaper.class )
					.where( f -> f.match().field( "referencedBy" )
							.matching( "Perihelion Motion Mercury" ) )
					.fetchHits( 20 )
			)
					.hasSize( 2 );
			assertThat( searchSession.search( ScientificPaper.class )
					.where( f -> f.match().field( "referencedBy" )
							.matching( "Fundamental Ideas" ) )
					.fetchHits( 20 )
			)
					.hasSize( 0 );
		} );
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			ScientificPaper paper1 = entityManager.find( ScientificPaper.class, 1 );
			ScientificPaper paper2 = entityManager.find( ScientificPaper.class, 2 );
			ScientificPaper paper3 = entityManager.find( ScientificPaper.class, 3 );
			ScientificPaper paper4 = new ScientificPaper( 4 );
			paper4.setTitle( "The Field Equations of Gravitation" );
			paper4.getReferences().add( paper1 );
			paper4.getReferences().add( paper2 );
			paper4.getReferences().add( paper3 );

			entityManager.persist( paper4 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			assertThat( searchSession.search( ScientificPaper.class )
					.where( f -> f.match().field( "referencedBy" )
							.matching( "Field Equations Gravitation" ) )
					.fetchHits( 20 )
			)
					.hasSize( 3 );
			assertThat( searchSession.search( ScientificPaper.class )
					.where( f -> f.match().field( "referencedBy" )
							.matching( "Perihelion Motion Mercury" ) )
					.fetchHits( 20 )
			)
					.hasSize( 2 );
			assertThat( searchSession.search( ScientificPaper.class )
					.where( f -> f.match().field( "referencedBy" )
							.matching( "Fundamental Ideas" ) )
					.fetchHits( 20 )
			)
					.hasSize( 0 );
		} );
	}

}
