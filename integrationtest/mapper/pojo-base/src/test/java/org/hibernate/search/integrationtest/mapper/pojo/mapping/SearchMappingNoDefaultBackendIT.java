/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SearchMappingNoDefaultBackendIT {

	private static final String BACKEND_1_NAME = "stubBackend1";
	private static final String BACKEND_2_NAME = "stubBackend2";

	@RegisterExtension
	public static BackendMock backend1Mock = BackendMock.create();

	@RegisterExtension
	public static BackendMock backend2Mock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper;

	private SearchMapping mapping;

	public SearchMappingNoDefaultBackendIT() {
		Map<String, BackendMock> namedBackendMocks = new LinkedHashMap<>();
		namedBackendMocks.put( BACKEND_1_NAME, backend1Mock );
		namedBackendMocks.put( BACKEND_2_NAME, backend2Mock );
		setupHelper = StandalonePojoMappingSetupHelper.withBackendMocks( MethodHandles.lookup(),
				null, namedBackendMocks );
	}

	@BeforeEach
	void before() {
		backend1Mock.expectAnySchema( Person.INDEX_NAME );
		backend2Mock.expectAnySchema( Pet.ENTITY_NAME );
		mapping = setupHelper.start()
				.withAnnotatedTypes( Person.class, Pet.class, Toy.class )
				.setup();
		backend1Mock.verifyExpectationsMet();
		backend2Mock.verifyExpectationsMet();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4656")
	void backend_default_nonExisting() {
		assertThatThrownBy( () -> mapping.backend() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"No default backend",
						"Check that at least one entity is configured to target the default backend",
						"The following backends can be retrieved by name: [" + BACKEND_1_NAME + ", " + BACKEND_2_NAME + "]"
				);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4656")
	void backend_byName_invalidName() {
		assertThatThrownBy( () -> mapping.backend( "invalid" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"No backend with name 'invalid'",
						"Check that at least one entity is configured to target that backend",
						"The following backends can be retrieved by name: [" + BACKEND_1_NAME + ", " + BACKEND_2_NAME + "]",
						"The default backend cannot be retrieved, because no entity is mapped to that backend"
				);
	}

	@SearchEntity(name = Person.ENTITY_NAME)
	@Indexed(backend = BACKEND_1_NAME, index = Person.INDEX_NAME)
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

	@SearchEntity(name = Pet.ENTITY_NAME)
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

	@SearchEntity(name = Toy.ENTITY_NAME)
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
