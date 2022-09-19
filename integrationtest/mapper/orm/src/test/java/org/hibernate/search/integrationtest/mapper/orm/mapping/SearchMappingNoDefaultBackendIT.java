/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.ReusableOrmSetupHolder;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

public class SearchMappingNoDefaultBackendIT {

	private static final String BACKEND_1_NAME = "stubBackend1";
	private static final String BACKEND_2_NAME = "stubBackend2";

	@ClassRule
	public static BackendMock backend1Mock = new BackendMock();

	@ClassRule
	public static BackendMock backend2Mock = new BackendMock();

	@ClassRule
	public static ReusableOrmSetupHolder setupHolder;
	static {
		Map<String, BackendMock> namedBackendMocks = new LinkedHashMap<>();
		namedBackendMocks.put( BACKEND_1_NAME, backend1Mock );
		namedBackendMocks.put( BACKEND_2_NAME, backend2Mock );
		setupHolder = ReusableOrmSetupHolder.withBackendMocks( null, namedBackendMocks );
	}

	@Rule
	public MethodRule setupHolderMethodRule = setupHolder.methodRule();

	private SearchMapping mapping;

	@ReusableOrmSetupHolder.Setup
	public void setup(OrmSetupHelper.SetupContext setupContext) {
		backend1Mock.expectAnySchema( Person.INDEX_NAME );
		backend2Mock.expectAnySchema( Pet.JPA_ENTITY_NAME );
		setupContext.withAnnotatedTypes( Person.class, Pet.class, Toy.class );
	}

	@Before
	public void before() {
		mapping = Search.mapping( setupHolder.sessionFactory() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4656")
	public void backend_default_nonExisting() {
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
	public void backend_byName_invalidName() {
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
