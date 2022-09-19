/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.multi;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ProjectionBinderMultiIT {
	@Parameterized.Parameters(name = "{0}")
	public static List<?> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				BackendConfigurations.simple(),
				// Since we disable classpath scanning in tests for performance reasons,
				// we need to register annotated projection types explicitly.
				// This wouldn't be needed in a typical application.
				CollectionHelper.asSet( MyBookProjection.class ),
				mapping -> {
					TypeMappingStep bookMapping = mapping.type( Book.class );
					bookMapping.indexed();
					bookMapping.property( "tags" )
							.keywordField().projectable( Projectable.YES );
					//tag::programmatic[]
					TypeMappingStep myBookProjectionMapping = mapping.type( MyBookProjection.class );
					myBookProjectionMapping.mainConstructor().projectionConstructor();
					myBookProjectionMapping.mainConstructor().parameter( 0 )
							.projection( new MyFieldProjectionBinder() );
					//end::programmatic[]
				}
		);
	}

	@Parameterized.Parameter
	@Rule
	public DocumentationSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	public void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book = new Book();
			book.setTags( List.of( "robot", "crime" ) );
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
							.map( book -> new MyBookProjection( book.getTags() ) )
							.collect( Collectors.toList() ) );
		} );
	}

}
