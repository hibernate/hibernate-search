/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Check that a failing boot correctly propagates exceptions,
 * despite the complex asynchronous code used during boot.
 */
class BootstrapFailureIT {

	private static final String INDEX_NAME = "IndexName";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	void propagateException() {
		assertThatThrownBy( () -> ormSetupHelper.start()
				.setup( FailingIndexedEntity.class, ContainedEntity.class )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"No default value bridge implementation",
						"Use a custom bridge",
						ContainedEntity.class.getName()
				);
	}

	@Entity(name = "failingindexed")
	@Indexed(index = INDEX_NAME)
	private static class FailingIndexedEntity {
		@Id
		private Integer id;

		@OneToOne
		@GenericField // This should fail, because there isn't any bridge for ContainedEntity
		private ContainedEntity field;
	}

	@Entity(name = "contained")
	private static class ContainedEntity {
		@Id
		private Integer id;
	}
}
