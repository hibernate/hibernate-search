/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchMappingNoDefaultBackendIT {

	private static final String BACKEND_1_NAME = "stubBackend1";
	private static final String BACKEND_2_NAME = "stubBackend2";

	@RegisterExtension
	public static BackendMock backend1Mock = BackendMock.create();

	@RegisterExtension
	public static BackendMock backend2Mock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMocks( null,
			Map.of( BACKEND_1_NAME, backend1Mock,
					BACKEND_2_NAME, backend2Mock )
	);

	private SearchMapping mapping;
	private SessionFactory sessionFactory;

	@BeforeAll
	void setup() {
		backend1Mock.expectAnySchema( Person.INDEX_NAME );
		backend2Mock.expectAnySchema( Pet.JPA_ENTITY_NAME );
		sessionFactory = ormSetupHelper.start().withAnnotatedTypes( Person.class, Pet.class, Toy.class ).setup();
	}

	@BeforeEach
	public void before() {
		mapping = Search.mapping( sessionFactory );
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

	@Entity(name = Person.JPA_ENTITY_NAME)
	@Indexed(backend = BACKEND_1_NAME, index = Person.INDEX_NAME)
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
