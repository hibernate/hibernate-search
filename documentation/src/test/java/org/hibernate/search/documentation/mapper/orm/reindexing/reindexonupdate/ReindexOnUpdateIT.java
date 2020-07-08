/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.reindexing.reindexonupdate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ReindexOnUpdateIT {

	@Parameterized.Parameters(name = "{0}")
	public static List<?> params() {
		return DocumentationSetupHelper.testParamsWithSingleBackendForBothAnnotationsAndProgrammatic(
				BackendConfigurations.simple(),
				mapping -> {
					//tag::programmatic[]
					TypeMappingStep bookMapping = mapping.type( Book.class );
					bookMapping.indexed();
					bookMapping.property( "category" )
							.indexedEmbedded()
							.indexingDependency().reindexOnUpdate( ReindexOnUpdate.NO );
					TypeMappingStep bookCategoryMapping = mapping.type( BookCategory.class );
					bookCategoryMapping.property( "name" )
							.fullTextField().analyzer( "english" );
					//end::programmatic[]
				} );
	}

	@Parameterized.Parameter
	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class, BookCategory.class );
	}

	@Test
	public void noReindexing() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			BookCategory category = new BookCategory();
			category.setId( 1 );
			category.setName( "Science-fiction" );
			entityManager.persist( category );

			for ( int i = 0 ; i < 100 ; ++i ) {
				Book book = new Book();
				book.setId( i );
				book.setTitle( "Book " + i );
				book.setCategory( category );
				entityManager.persist( book );
			}
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			long hitCount = searchSession.search( Book.class )
					.where( f -> f.match().field( "category.name" ).matching( "science" ) )
					.fetchTotalHitCount();
			assertThat( hitCount ).isEqualTo( 100L );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			BookCategory category = entityManager.getReference( BookCategory.class, 1 );
			category.setName( "Anticipation" );
			entityManager.persist( category );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			long hitCount = searchSession.search( Book.class )
					.where( f -> f.match().field( "category.name" ).matching( "science" ) )
					.fetchTotalHitCount();
			// The books haven't been reindexed, as expected.
			assertThat( hitCount ).isEqualTo( 100L );
		} );

	}

}
