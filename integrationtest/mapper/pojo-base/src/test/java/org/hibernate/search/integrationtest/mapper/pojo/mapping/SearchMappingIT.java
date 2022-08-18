/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackend;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexManager;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.InstanceOfAssertFactories;

public class SearchMappingIT {

	private static final String BACKEND_2_NAME = "stubBackend2";

	@Rule
	public BackendMock defaultBackendMock = new BackendMock();

	@Rule
	public BackendMock backend2Mock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper;

	private SearchMapping mapping;

	public SearchMappingIT() {
		Map<String, BackendMock> namedBackendMocks = new LinkedHashMap<>();
		namedBackendMocks.put( BACKEND_2_NAME, backend2Mock );
		setupHelper = StandalonePojoMappingSetupHelper.withBackendMocks( MethodHandles.lookup(),
				defaultBackendMock, namedBackendMocks );
	}

	@Before
	public void before() {
		defaultBackendMock.expectAnySchema( Person.INDEX_NAME );
		backend2Mock.expectAnySchema( Pet.ENTITY_NAME );
		mapping = setupHelper.start()
				.withAnnotatedEntityType( Person.class, Person.ENTITY_NAME )
				.withAnnotatedEntityType( Pet.class, Pet.ENTITY_NAME )
				.withAnnotatedEntityType( Toy.class, Toy.ENTITY_NAME )
				.setup();
		defaultBackendMock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byName() {
		SearchIndexedEntity<?> entity = mapping.indexedEntity( Person.ENTITY_NAME );
		assertThat( entity )
				.isNotNull()
				.returns( Person.ENTITY_NAME, SearchIndexedEntity::name )
				.returns( Person.class, SearchIndexedEntity::javaClass );
		checkIndexManager( Person.INDEX_NAME, entity.indexManager() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byName_notEntity() {
		assertThatThrownBy( () -> mapping.indexedEntity( "invalid" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"No matching indexed entity type for name 'invalid'",
						"Either this is not the name of an entity type, or the entity type is not indexed in Hibernate Search",
						"Valid names for indexed entity types are: ["
								+ Person.ENTITY_NAME + ", "
								+ Pet.ENTITY_NAME
								// This should NOT include Toy, which is not an indexed entity type
								+ "]"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byName_notIndexed() {
		assertThatThrownBy( () -> mapping.indexedEntity( Toy.ENTITY_NAME ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"No matching indexed entity type for name '" + Toy.ENTITY_NAME + "'",
						"Either this is not the name of an entity type, or the entity type is not indexed in Hibernate Search",
						"Valid names for indexed entity types are: ["
								+ Person.ENTITY_NAME + ", "
								+ Pet.ENTITY_NAME
								// This should NOT include Toy, which is not an indexed entity type
								+ "]"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byJavaClass() {
		SearchIndexedEntity<Person> entity = mapping.indexedEntity( Person.class );
		assertThat( entity )
				.isNotNull()
				.returns( Person.ENTITY_NAME, SearchIndexedEntity::name )
				.returns( Person.class, SearchIndexedEntity::javaClass );
		checkIndexManager( Person.INDEX_NAME, entity.indexManager() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byJavaClass_notEntity() {
		assertThatThrownBy( () -> mapping.indexedEntity( String.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"No matching indexed entity type for class '" + String.class.getName() + "'",
						"Either this class is not an entity type, or the entity type is not indexed in Hibernate Search",
						"Valid classes for indexed entity types are: ["
								+ Person.class.getName() + ", "
								+ Pet.class.getName()
								// This should NOT include Toy, which is not an indexed entity type
								+ "]"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byJavaClass_notIndexed() {
		assertThatThrownBy( () -> mapping.indexedEntity( Toy.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"No matching indexed entity type for class '" + Toy.class.getName() + "'",
						"Either this class is not an entity type, or the entity type is not indexed in Hibernate Search",
						"Valid classes for indexed entity types are: ["
								+ Person.class.getName() + ", "
								+ Pet.class.getName()
								// This should NOT include Toy, which is not an indexed entity type
								+ "]"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void allIndexedEntities() {
		Collection<? extends SearchIndexedEntity<?>> entities = mapping.allIndexedEntities();
		assertThat( entities )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder(
						Person.ENTITY_NAME,
						Pet.ENTITY_NAME
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3994")
	public void scope_indexedEntities() {
		SearchScope<Object> objectScope = mapping.scope( Object.class );
		Set<? extends SearchIndexedEntity<?>> objectEntities = objectScope.includedTypes();
		assertThat( objectEntities )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder(
						Person.ENTITY_NAME,
						Pet.ENTITY_NAME
				);

		SearchScope<Person> personScope = mapping.scope( Person.class );
		Set<? extends SearchIndexedEntity<? extends Person>> personEntities = personScope.includedTypes();
		assertThat( personEntities )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder(
						Person.ENTITY_NAME
				);

		SearchScope<Pet> petScope = mapping.scope( Pet.class );
		Set<? extends SearchIndexedEntity<? extends Pet>> petEntities = petScope.includedTypes();
		assertThat( petEntities )
				.extracting( SearchIndexedEntity::name )
				.containsExactlyInAnyOrder(
						Pet.ENTITY_NAME
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
		IndexManager indexManager = mapping.indexManager( Pet.ENTITY_NAME );
		checkIndexManager( Pet.ENTITY_NAME, indexManager );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4656")
	public void indexManager_invalidName() {
		assertThatThrownBy( () -> mapping.indexManager( "invalid" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"No index manager with name 'invalid'",
						"Check that at least one entity is configured to target that index",
						"The following indexes can be retrieved by name: [" + Person.INDEX_NAME + ", " + Pet.ENTITY_NAME + "]"
				);
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-3640", "HSEARCH-3950" })
	public void backend_default() {
		Backend backend = mapping.backend();
		checkBackend( EventContexts.defaultBackend(), backend );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3640")
	public void backend_byName() {
		Backend backend = mapping.backend( BACKEND_2_NAME );
		checkBackend( EventContexts.fromBackendName( BACKEND_2_NAME ), backend );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4656")
	public void backend_byName_invalidName() {
		assertThatThrownBy( () -> mapping.backend( "invalid" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"No backend with name 'invalid'",
						"Check that at least one entity is configured to target that backend",
						"The following backends can be retrieved by name: [" + BACKEND_2_NAME + "]",
						"The default backend can be retrieved"
				);
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

	@Indexed(index = Person.INDEX_NAME)
	private static class Person {
		public static final String ENTITY_NAME = "PersonEntity";
		public static final String INDEX_NAME = "Person";

		@DocumentId
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

	@Indexed(backend = BACKEND_2_NAME)
	private static class Pet {
		public static final String ENTITY_NAME = "Pet";

		@DocumentId
		private Integer id;
		@GenericField
		private String nickname;
		private List<Toy> toys;

		public Pet() {
		}
	}

	private static class Toy {
		public static final String ENTITY_NAME = "Toy";

		private Integer id;
		@GenericField
		private String name;
		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "toys")))
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
