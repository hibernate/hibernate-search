/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.scope;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.integrationtest.mapper.pojo.search.loading.model.singletype.BasicIndexedEntity;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubIndexScope;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ScopeExtensionIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@BeforeEach
	void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );
		mapping = setupHelper.start().setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void test() {
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
