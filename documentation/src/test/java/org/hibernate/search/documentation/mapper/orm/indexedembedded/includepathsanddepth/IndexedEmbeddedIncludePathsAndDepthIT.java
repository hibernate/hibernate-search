/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.includepathsanddepth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.Assertions;

@RunWith(Parameterized.class)
public class IndexedEmbeddedIncludePathsAndDepthIT {

	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public IndexedEmbeddedIncludePathsAndDepthIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = DocumentationSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyNames.SYNC
				)
				.setup( Human.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
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

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Human> result = searchSession.search( Human.class )
					.where( f -> f.bool()
							.must( f.match().field( "name" ).matching( "fourth" ) )
							.must( f.match().field( "nickname" ).matching( "babe" ) )
							.must( f.match().field( "parents.name" ).matching( "third" ) )
							.must( f.match().field( "parents.nickname" ).matching( "young" ) )
							.must( f.match().field( "parents.parents.name" ).matching( "junior" ) )
							.must( f.match().field( "parents.parents.nickname" ).matching( "old" ) )
							.must( f.match().field( "parents.parents.parents.name" ).matching( "senior" ) )
					)
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 );
		} );

		SearchMapping searchMapping = Search.mapping( entityManagerFactory );

		Assertions.assertThatThrownBy(
				() -> {
					searchMapping.scope( Human.class ).predicate()
							.match().field( "parents.parents.parents.nickname" );
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" );
		Assertions.assertThatThrownBy(
				() -> {
					searchMapping.scope( Human.class ).predicate()
							.match().field( "parents.parents.parents.parents.name" );
				}
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" );
	}

}
