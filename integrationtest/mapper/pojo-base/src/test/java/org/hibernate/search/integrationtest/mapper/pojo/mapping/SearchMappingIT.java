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
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
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

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void before() {
		backendMock.expectAnySchema( Person.INDEX_NAME );
		backendMock.expectAnySchema( Pet.ENTITY_NAME );
		mapping = setupHelper.start()
				.withAnnotatedEntityType( Person.class, Person.ENTITY_NAME )
				.withAnnotatedEntityType( Pet.class, Pet.ENTITY_NAME )
				.withAnnotatedEntityType( Toy.class, Toy.ENTITY_NAME )
				.setup();
		backendMock.verifyExpectationsMet();
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
				.hasMessageContaining( "Entity type 'invalid' does not exist or is not indexed" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byName_notIndexed() {
		assertThatThrownBy( () -> mapping.indexedEntity( Toy.ENTITY_NAME ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Entity type '" + Toy.ENTITY_NAME + "' does not exist or is not indexed" );
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
				.hasMessageContaining(
						"Type '" + String.class.getName() + "' is not an entity type, or this entity type is not indexed"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3589")
	public void indexedEntity_byJavaClass_notIndexed() {
		assertThatThrownBy( () -> mapping.indexedEntity( Toy.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Type '" + Toy.class.getName() + "' is not an entity type, or this entity type is not indexed"
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

	@Indexed
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
