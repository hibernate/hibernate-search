/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.scope;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.singletype.BasicIndexedEntity;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ScopeExtensionIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		mapping = setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void test() {
		IndexScope indexScope = mapping.scope( IndexedEntity.class ).extension( original -> original );
		assertThat( indexScope ).isInstanceOf( StubIndexScope.class );
	}

	@Indexed(index = BasicIndexedEntity.NAME)
	public static final class IndexedEntity {
		public static final String NAME = "indexed";

		@DocumentId
		private Integer id;
		@GenericField
		private String value;
	}
}
