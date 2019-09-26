/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.mapping;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

@TestForIssue( jiraKey = "HSEARCH-3640" )
public class SearchMappingIT {

	private static final String BACKEND_1_NAME = "stubBackend1";
	private static final String BACKEND_2_NAME = "stubBackend2";

	@Rule
	public BackendMock backendMock1 = new BackendMock( BACKEND_1_NAME );

	@Rule
	public BackendMock backendMock2 = new BackendMock( BACKEND_2_NAME );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMocks( backendMock1, backendMock2 );

	private SessionFactory sessionFactory;

	@Before
	public void before() {
		backendMock1.expectAnySchema( Person.INDEX_NAME );
		backendMock2.expectAnySchema( Pet.JPA_ENTITY_NAME );
		sessionFactory = ormSetupHelper.start().setup( Person.class, Pet.class );
		backendMock1.verifyExpectationsMet();
		backendMock2.verifyExpectationsMet();
	}

	@Test
	public void getIndexManager_customIndexName() {
		SearchMapping target = Search.mapping( sessionFactory );
		IndexManager indexManager = target.getIndexManager( Person.INDEX_NAME );
		Assertions.assertThat( indexManager ).isNotNull();
	}

	@Test
	public void getIndexManager_defaultIndexName() {
		SearchMapping target = Search.mapping( sessionFactory );
		IndexManager indexManager = target.getIndexManager( Pet.JPA_ENTITY_NAME );
		Assertions.assertThat( indexManager ).isNotNull();
	}

	@Test
	public void getBackendByName() {
		SearchMapping target = Search.mapping( sessionFactory );

		Object backend = target.getBackend( BACKEND_1_NAME );
		Assertions.assertThat( backend ).isNotNull();

		backend = target.getBackend( BACKEND_2_NAME );
		Assertions.assertThat( backend ).isNotNull();
	}

	@Entity(name = "PersonEntity")
	@Indexed(index = Person.INDEX_NAME, backend = BACKEND_1_NAME)
	private static class Person {
		public static final String INDEX_NAME = "Person";

		@Id
		private Integer id;
		@GenericField
		private String name;

		public Person() {
		}

		public Integer getId() {
			return id;
		}
		public String getName() {
			return name;
		}
	}

	@Entity(name = Pet.JPA_ENTITY_NAME)
	@Indexed(backend = BACKEND_2_NAME)
	private static class Pet {
		public static final String JPA_ENTITY_NAME = "Pet";

		@Id
		private Integer id;
		@GenericField
		private String nickname;

		public Pet() {
		}

		public Integer getId() {
			return id;
		}
		public String getNickname() {
			return nickname;
		}
	}
}
