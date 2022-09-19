/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexScope;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ScopeExtensionIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( Author.NAME );
		sessionFactory = ormSetupHelper.start().setup( Author.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void test() {
		with( sessionFactory ).runNoTransaction( session -> {
			IndexScope indexScope = Search.session( session ).scope( Author.class )
					.extension( original -> original );
			assertThat( indexScope ).isInstanceOf( StubIndexScope.class );
		} );
	}

	@Entity(name = Author.NAME)
	@Indexed(index = Author.NAME)
	public static class Author {
		public static final String NAME = "Author";

		@Id
		private Integer id;
		@GenericField
		private String name;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
