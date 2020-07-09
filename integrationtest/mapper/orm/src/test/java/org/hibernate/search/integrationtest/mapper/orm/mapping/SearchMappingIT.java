/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;

public class SearchMappingIT {

	private static final String BACKEND_1_NAME = "stubBackend1";
	private static final String BACKEND_2_NAME = "stubBackend2";

	@Rule
	public BackendMock backendMock1 = new BackendMock( BACKEND_1_NAME );

	@Rule
	public BackendMock backendMock2 = new BackendMock( BACKEND_2_NAME );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMocks( backendMock1, backendMock2 );

	private SearchMapping mapping;

	@Before
	public void before() {
		backendMock1.expectAnySchema( Person.INDEX_NAME );
		backendMock2.expectAnySchema( Pet.JPA_ENTITY_NAME );
		SessionFactory sessionFactory = ormSetupHelper.start().setup( Person.class, Pet.class, Toy.class );
		backendMock1.verifyExpectationsMet();
		backendMock2.verifyExpectationsMet();
		mapping = Search.mapping( sessionFactory );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byName() {
		SearchIndexedEntity entity = mapping.indexedEntity( Person.JPA_ENTITY_NAME );
		assertThat( entity )
				.isNotNull()
				.returns( Person.JPA_ENTITY_NAME, SearchIndexedEntity::jpaName )
				.returns( Person.class, SearchIndexedEntity::javaClass );
		checkIndexManager( Person.INDEX_NAME, entity.indexManager() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byName_notEntity() {
		assertThatThrownBy( () -> mapping.indexedEntity( "invalid" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown entity name: 'invalid'",
						"Available entity names:",
						Person.class.getName(),
						Person.JPA_ENTITY_NAME,
						Pet.class.getName(),
						Pet.JPA_ENTITY_NAME,
						Toy.class.getName(),
						Toy.JPA_ENTITY_NAME
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byName_notIndexed() {
		assertThatThrownBy( () -> mapping.indexedEntity( Toy.JPA_ENTITY_NAME ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Entity '" + Toy.JPA_ENTITY_NAME + "' is not indexed"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byJavaClass() {
		SearchIndexedEntity entity = mapping.indexedEntity( Person.class );
		assertThat( entity )
				.isNotNull()
				.returns( Person.JPA_ENTITY_NAME, SearchIndexedEntity::jpaName )
				.returns( Person.class, SearchIndexedEntity::javaClass );
		checkIndexManager( Person.INDEX_NAME, entity.indexManager() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byJavaClass_notEntity() {
		assertThatThrownBy( () -> mapping.indexedEntity( String.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Type '" + String.class.getName() + "' is not an entity type, or the entity is not indexed"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byJavaClass_notIndexed() {
		assertThatThrownBy( () -> mapping.indexedEntity( Toy.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Type '" + Toy.class.getName() + "' is not an entity type, or the entity is not indexed"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void allIndexedEntities() {
		Collection<? extends SearchIndexedEntity> entities = mapping.allIndexedEntities();
		Assertions.<SearchIndexedEntity>assertThat( entities )
				.extracting( SearchIndexedEntity::jpaName )
				.containsExactlyInAnyOrder(
						Person.JPA_ENTITY_NAME,
						Pet.JPA_ENTITY_NAME
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3640")
	public void indexManager_customIndexName() {
		IndexManager indexManager = mapping.indexManager( Person.INDEX_NAME );
		checkIndexManager( Person.INDEX_NAME, indexManager );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3640")
	public void indexManager_defaultIndexName() {
		IndexManager indexManager = mapping.indexManager( Pet.JPA_ENTITY_NAME );
		checkIndexManager( Pet.JPA_ENTITY_NAME, indexManager );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3640", "HSEARCH-3950" })
	@Ignore // TODO enable once we actually define backend 1 as an unnamed, default backend
	public void backend_default() {
		Backend backend = mapping.backend();
		checkBackend( EventContexts.defaultBackend(), backend );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3640")
	public void backend_byName() {
		Backend backend = mapping.backend( BACKEND_1_NAME );
		checkBackend( EventContexts.fromBackendName( BACKEND_1_NAME ), backend );

		backend = mapping.backend( BACKEND_2_NAME );
		checkBackend( EventContexts.fromBackendName( BACKEND_2_NAME ), backend );
	}

	private void checkIndexManager(String expectedIndexName, IndexManager indexManager) {
		assertThat( indexManager )
				.asInstanceOf( InstanceOfAssertFactories.type( StubIndexManager.class ) )
				.returns( expectedIndexName, StubIndexManager::getName );
	}

	private void checkBackend(EventContext expectedEventContext, Backend indexManager) {
		assertThat( indexManager )
				.asInstanceOf( InstanceOfAssertFactories.type( StubBackend.class ) )
				.returns( expectedEventContext, StubBackend::eventContext );
	}

	@Entity(name = Person.JPA_ENTITY_NAME)
	@Indexed(index = Person.INDEX_NAME, backend = BACKEND_1_NAME)
	private static class Person {
		public static final String JPA_ENTITY_NAME = "PersonEntity";
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
		@OneToMany(mappedBy = "owner")
		private List<Toy> toys;

		public Pet() {
		}

		public Integer getId() {
			return id;
		}
		public String getNickname() {
			return nickname;
		}
		public List<Toy> getToys() {
			return toys;
		}
	}

	@Entity(name = Toy.JPA_ENTITY_NAME)
	private static class Toy {
		public static final String JPA_ENTITY_NAME = "Toy";

		@Id
		private Integer id;
		@GenericField
		private String name;
		@ManyToOne
		private Pet owner;

		public Toy() {
		}

		public Integer getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public Pet getOwner() {
			return owner;
		}
	}
}
