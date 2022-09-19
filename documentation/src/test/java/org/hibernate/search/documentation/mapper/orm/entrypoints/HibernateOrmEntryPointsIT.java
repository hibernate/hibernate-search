/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.entrypoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.Arrays;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class HibernateOrmEntryPointsIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private SessionFactory theSessionFactory;

	@Before
	public void setup() {
		this.theSessionFactory = setupHelper.start()
				.setup( Book.class, Associate.class, Manager.class );
	}

	@Test
	public void searchMapping_fromSessionFactory() {
		// tag::searchMapping-fromSessionFactory[]
		SessionFactory sessionFactory = /* ... */ // <1>
				// end::searchMapping-fromSessionFactory[]
				theSessionFactory;
		// tag::searchMapping-fromSessionFactory[]
		SearchMapping searchMapping = Search.mapping( sessionFactory ); // <2>
		// end::searchMapping-fromSessionFactory[]
		assertThat( searchMapping ).isNotNull();
		assertThat( searchMapping.toOrmSessionFactory() ).isEqualTo( sessionFactory );
	}

	@Test
	public void searchMapping_fromEntityManagerFactory() {
		// tag::searchMapping-fromEntityManagerFactory[]
		EntityManagerFactory entityManagerFactory = /* ... */ // <1>
				// end::searchMapping-fromEntityManagerFactory[]
				theSessionFactory;
		// tag::searchMapping-fromEntityManagerFactory[]
		SearchMapping searchMapping = Search.mapping( entityManagerFactory ); // <2>
		// end::searchMapping-fromEntityManagerFactory[]
		assertThat( searchMapping ).isNotNull();
		assertThat( searchMapping.toEntityManagerFactory() ).isEqualTo( entityManagerFactory );
	}

	@Test
	public void searchSession_fromSession() {
		with( theSessionFactory ).runNoTransaction( theSession -> {
			// tag::searchSession-fromSession[]
			Session session = /* ... */ // <1>
					// end::searchSession-fromSession[]
					theSession;
			// tag::searchSession-fromSession[]
			SearchSession searchSession = Search.session( session ); // <2>
			// end::searchSession-fromSession[]
			assertThat( searchSession ).isNotNull();
			assertThat( searchSession.toOrmSession() ).isEqualTo( session );
		} );
	}

	@Test
	public void searchSession_fromEntityManager() {
		with( theSessionFactory ).runNoTransaction( theSession -> {
			// tag::searchSession-fromEntityManager[]
			EntityManager entityManager = /* ... */ // <1>
					// end::searchSession-fromEntityManager[]
					theSession;
			// tag::searchSession-fromEntityManager[]
			SearchSession searchSession = Search.session( entityManager ); // <2>
			// end::searchSession-fromEntityManager[]
			assertThat( searchSession ).isNotNull();
			assertThat( searchSession.toEntityManager() ).isEqualTo( entityManager );
		} );
	}

	@Test
	public void searchScope_fromSearchMapping() {
		// tag::searchScope-fromSearchMapping[]
		SearchMapping searchMapping = /* ... */ // <1>
				// end::searchScope-fromSearchMapping[]
				Search.mapping( theSessionFactory );
		// tag::searchScope-fromSearchMapping[]
		SearchScope<Book> bookScope = searchMapping.scope( Book.class ); // <2>
		SearchScope<Person> associateAndManagerScope = searchMapping.scope( Arrays.asList( Associate.class, Manager.class ) ); // <3>
		SearchScope<Person> personScope = searchMapping.scope( Person.class ); // <4>
		SearchScope<Person> personSubTypesScope = searchMapping.scope( Person.class,
				Arrays.asList( "Manager", "Associate" ) ); // <5>
		SearchScope<Object> allScope = searchMapping.scope( Object.class ); // <6>
		// end::searchScope-fromSearchMapping[]
		assertThat( bookScope.includedTypes() )
				.extracting( SearchIndexedEntity::jpaName )
				.containsExactlyInAnyOrder( "Book" );
		assertThat( associateAndManagerScope.includedTypes() )
				.extracting( SearchIndexedEntity::jpaName )
				.containsExactlyInAnyOrder( "Manager", "Associate" );
		assertThat( personScope.includedTypes() )
				.extracting( SearchIndexedEntity::jpaName )
				.containsExactlyInAnyOrder( "Manager", "Associate" );
		assertThat( personSubTypesScope.includedTypes() )
				.extracting( SearchIndexedEntity::jpaName )
				.containsExactlyInAnyOrder( "Manager", "Associate" );
		assertThat( allScope.includedTypes() )
				.extracting( SearchIndexedEntity::jpaName )
				.containsExactlyInAnyOrder( "Book", "Manager", "Associate" );
	}

	@Test
	public void searchScope_fromSearchSession() {
		with( theSessionFactory ).runNoTransaction( theSession -> {
			// tag::searchScope-fromSearchSession[]
			SearchSession searchSession = /* ... */ // <1>
					// end::searchScope-fromSearchSession[]
					Search.session( theSession );
			// tag::searchScope-fromSearchSession[]
			SearchScope<Book> bookScope = searchSession.scope( Book.class ); // <2>
			SearchScope<Person> associateAndManagerScope =
					searchSession.scope( Arrays.asList( Associate.class, Manager.class ) ); // <3>
			SearchScope<Person> personScope = searchSession.scope( Person.class ); // <4>
			SearchScope<Person> personSubTypesScope = searchSession.scope( Person.class,
					Arrays.asList( "Manager", "Associate" ) ); // <5>
			SearchScope<Object> allScope = searchSession.scope( Object.class ); // <6>
			// end::searchScope-fromSearchSession[]
			assertThat( bookScope.includedTypes() )
					.extracting( SearchIndexedEntity::jpaName )
					.containsExactlyInAnyOrder( "Book" );
			assertThat( associateAndManagerScope.includedTypes() )
					.extracting( SearchIndexedEntity::jpaName )
					.containsExactlyInAnyOrder( "Manager", "Associate" );
			assertThat( personScope.includedTypes() )
					.extracting( SearchIndexedEntity::jpaName )
					.containsExactlyInAnyOrder( "Manager", "Associate" );
			assertThat( personSubTypesScope.includedTypes() )
					.extracting( SearchIndexedEntity::jpaName )
					.containsExactlyInAnyOrder( "Manager", "Associate" );
			assertThat( allScope.includedTypes() )
					.extracting( SearchIndexedEntity::jpaName )
					.containsExactlyInAnyOrder( "Book", "Manager", "Associate" );
		} );
	}

}
