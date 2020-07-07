/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.dependencies.containers.fromotherentity;


import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DependenciesFromOtherEntityIT {
	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public DependenciesFromOtherEntityIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyNames.SYNC
				)
				.setup( ScientificPaper.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			ScientificPaper paper1 = new ScientificPaper( 1 );
			paper1.setTitle( "Fundamental Ideas of the General Theory of Relativity and the Application of this Theory in Astronomy" );
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

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
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
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
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

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
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
