/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.injection;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import org.hibernate.search.test.integration.spring.injection.i18n.InternationalizedValue;
import org.hibernate.search.test.integration.spring.injection.model.EntityWithSpringAwareBridges;
import org.hibernate.search.test.integration.spring.injection.model.EntityWithSpringAwareBridgesDao;
import org.hibernate.search.test.integration.spring.injection.search.NonSpringBridge;
import org.hibernate.search.testsupport.TestForIssue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Yoann Rodiere
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringInjectionITApplicationConfiguration.class)
// Use @DirtiesContext to reinitialize the database (thanks to hbm2ddl.auto) between test methods
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@TestForIssue(jiraKey = "HSEARCH-1316")
public class SpringInjectionIT {

	@Inject
	private EntityWithSpringAwareBridgesDao dao;

	@Test
	public void injectedFieldBridge() {
		Function<String, List<EntityWithSpringAwareBridges>> search = dao::searchFieldBridge;

		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).isEmpty();

		EntityWithSpringAwareBridges entity = new EntityWithSpringAwareBridges();
		entity.setInternationalizedValue( InternationalizedValue.HELLO );
		dao.create( entity );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).isEmpty();

		EntityWithSpringAwareBridges entity2 = new EntityWithSpringAwareBridges();
		entity2.setInternationalizedValue( InternationalizedValue.GOODBYE );
		dao.create( entity2 );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).containsOnly( entity2.getId() );

		dao.delete( entity );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).containsOnly( entity2.getId() );
	}

	@Test
	public void injectedClassBridge() {
		Function<String, List<EntityWithSpringAwareBridges>> search = dao::searchClassBridge;

		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).isEmpty();

		EntityWithSpringAwareBridges entity = new EntityWithSpringAwareBridges();
		entity.setInternationalizedValue( InternationalizedValue.HELLO );
		dao.create( entity );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).isEmpty();

		EntityWithSpringAwareBridges entity2 = new EntityWithSpringAwareBridges();
		entity2.setInternationalizedValue( InternationalizedValue.GOODBYE );
		dao.create( entity2 );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).containsOnly( entity2.getId() );

		dao.delete( entity );
		assertThat( search.apply( "bonjour" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).onProperty( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).onProperty( "id" ).containsOnly( entity2.getId() );
	}

	@Test
	public void nonSpringFieldBridge() {
		Function<String, List<EntityWithSpringAwareBridges>> search = dao::searchNonSpringBridge;

		assertThat( search.apply( NonSpringBridge.PREFIX + InternationalizedValue.HELLO.name() ) ).onProperty( "id" ).isEmpty();

		EntityWithSpringAwareBridges entity = new EntityWithSpringAwareBridges();
		entity.setInternationalizedValue( InternationalizedValue.HELLO );
		dao.create( entity );
		assertThat( search.apply( NonSpringBridge.PREFIX + InternationalizedValue.HELLO.name() ) ).onProperty( "id" ).containsOnly( entity.getId() );
	}
}
