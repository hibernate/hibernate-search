/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.injection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;
import javax.inject.Inject;

import org.hibernate.search.test.integration.spring.injection.i18n.InternationalizedValue;
import org.hibernate.search.test.integration.spring.injection.model.EntityWithSpringAwareBridges;
import org.hibernate.search.test.integration.spring.injection.model.EntityWithSpringAwareBridgesDao;
import org.hibernate.search.test.integration.spring.injection.search.NonSpringBridge;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Yoann Rodiere
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringInjectionITApplicationConfiguration.class)
@TestForIssue(jiraKey = "HSEARCH-1316")
public class SpringInjectionIT {

	@Inject
	private EntityWithSpringAwareBridgesDao dao;

	@After
	public void cleanUpData() {
		// we're cleaning the data manually,
		// in order to have a class level application context,
		// to support the job of ExpectedLog4jLog
		dao.purge();
	}

	@Test
	public void injectedFieldBridge() {
		Function<String, List<EntityWithSpringAwareBridges>> search = dao::searchFieldBridge;

		assertThat( search.apply( "bonjour" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).extracting( "id" ).isEmpty();

		EntityWithSpringAwareBridges entity = new EntityWithSpringAwareBridges();
		entity.setInternationalizedValue( InternationalizedValue.HELLO );
		dao.create( entity );
		assertThat( search.apply( "bonjour" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).extracting( "id" ).isEmpty();

		EntityWithSpringAwareBridges entity2 = new EntityWithSpringAwareBridges();
		entity2.setInternationalizedValue( InternationalizedValue.GOODBYE );
		dao.create( entity2 );
		assertThat( search.apply( "bonjour" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).extracting( "id" ).containsOnly( entity2.getId() );

		dao.delete( entity );
		assertThat( search.apply( "bonjour" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).extracting( "id" ).containsOnly( entity2.getId() );
	}

	@Test
	public void injectedClassBridge() {
		Function<String, List<EntityWithSpringAwareBridges>> search = dao::searchClassBridge;

		assertThat( search.apply( "bonjour" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).extracting( "id" ).isEmpty();

		EntityWithSpringAwareBridges entity = new EntityWithSpringAwareBridges();
		entity.setInternationalizedValue( InternationalizedValue.HELLO );
		dao.create( entity );
		assertThat( search.apply( "bonjour" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).extracting( "id" ).isEmpty();

		EntityWithSpringAwareBridges entity2 = new EntityWithSpringAwareBridges();
		entity2.setInternationalizedValue( InternationalizedValue.GOODBYE );
		dao.create( entity2 );
		assertThat( search.apply( "bonjour" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hello" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "hallo" ) ).extracting( "id" ).containsOnly( entity.getId() );
		assertThat( search.apply( "au revoir" ) ).extracting( "id" ).containsOnly( entity2.getId() );

		dao.delete( entity );
		assertThat( search.apply( "bonjour" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "hello" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "hallo" ) ).extracting( "id" ).isEmpty();
		assertThat( search.apply( "au revoir" ) ).extracting( "id" ).containsOnly( entity2.getId() );
	}

	@Test
	public void nonSpringFieldBridge() {
		Function<String, List<EntityWithSpringAwareBridges>> search = dao::searchNonSpringBridge;

		assertThat( search.apply( NonSpringBridge.PREFIX + InternationalizedValue.HELLO.name() ) ).extracting( "id" ).isEmpty();

		EntityWithSpringAwareBridges entity = new EntityWithSpringAwareBridges();
		entity.setInternationalizedValue( InternationalizedValue.HELLO );
		dao.create( entity );
		assertThat( search.apply( NonSpringBridge.PREFIX + InternationalizedValue.HELLO.name() ) ).extracting( "id" ).containsOnly( entity.getId() );
	}
}
