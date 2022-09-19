/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.entityindexmapping;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HibernateOrmIndexedIT {

	private static final String BACKEND_2 = "backend2";

	private static final BackendConfiguration DEFAULT_BACKEND_CONFIGURATION;
	private static final Map<String, BackendConfiguration> NAMED_BACKEND_CONFIGURATIONS;
	static {
		BackendConfiguration backendConfiguration = BackendConfigurations.simple();
		DEFAULT_BACKEND_CONFIGURATION = backendConfiguration;
		Map<String, BackendConfiguration> map = new HashMap<>();
		map.put( BACKEND_2, backendConfiguration );
		NAMED_BACKEND_CONFIGURATIONS = map;
	}

	@Parameterized.Parameters(name = "{0}")
	public static List<?> params() {
		return DocumentationSetupHelper.testParamsForBothAnnotationsAndProgrammatic(
				DEFAULT_BACKEND_CONFIGURATION, NAMED_BACKEND_CONFIGURATIONS,
				mapping -> {
					//tag::programmatic[]
					TypeMappingStep bookMapping = mapping.type( Book.class );
					bookMapping.indexed();
					TypeMappingStep authorMapping = mapping.type( Author.class );
					authorMapping.indexed().index( "AuthorIndex" );
					TypeMappingStep userMapping = mapping.type( User.class );
					userMapping.indexed().backend( "backend2" );
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
		entityManagerFactory = setupHelper.start().setup( Book.class, User.class, Author.class );
		initData();
	}

	@Test
	public void search_separateQueries() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Author> authorResult = searchSession.search( Author.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			assertThat( authorResult ).hasSize( 1 );

			List<User> userResult = searchSession.search( User.class )
					.where( f -> f.matchAll() )
					.fetchHits( 20 );
			assertThat( userResult ).hasSize( 1 );
		} );
	}

	@Test
	public void search_singleQuery() {
		assertThatThrownBy(
				() -> with( entityManagerFactory ).runInTransaction( entityManager -> {
					SearchSession searchSession = Search.session( entityManager );

					// tag::cross-backend-search[]
					// This will fail because Author and User are indexed in different backends
					searchSession.search( Arrays.asList( Author.class, User.class ) )
							.where( f -> f.matchAll() )
							.fetchHits( 20 );
					// end::cross-backend-search[]
				} )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid multi-index scope",
						"a scope cannot span multiple " );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Book book1 = new Book();
			book1.setTitle( "Some title" );
			Author author1 = new Author();
			author1.setFirstName( "Jane" );
			author1.setLastName( "Doe" );
			User user1 = new User();
			user1.setFirstName( "John" );
			user1.setLastName( "Smith" );

			entityManager.persist( book1 );
			entityManager.persist( author1 );
			entityManager.persist( user1 );
		} );
	}

}
