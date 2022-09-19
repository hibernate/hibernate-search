/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

/**
 * Check that a failing boot correctly propagates exceptions,
 * despite the complex asynchronous code used during boot.
 */
public class BootstrapFailureIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	public void propagateException() {
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
