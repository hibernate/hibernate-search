/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.work;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PojoIndexingPlanSynchronizationStrategyIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock(
			MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectAnySchema( IndexedEntity.NAME );

		mapping = setupHelper.start()
				.withAnnotatedEntityType( IndexedEntity.class, IndexedEntity.NAME )
				.setup();
	}

	@Test
	public void overrideIndexingPlanSyncStrategy() {
		try ( SearchSession session = mapping.createSession() ) {
			IndexedEntity entity1 = new IndexedEntity();
			entity1.setId( 1 );
			entity1.setIndexedField( "initialValue" );

			session.indexingPlan().add( entity1 );

			// by default it should use the write-sync strategy:
			backendMock.expectWorks( IndexedEntity.NAME, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE )
					.add( "1", b -> b
							.field( "indexedField", entity1.getIndexedField() )
					);
		}

		// Note all kinds of overrides are already tested in depth in AbstractPojoIndexingOperationIT and subclasses
		try ( SearchSession session = mapping.createSession() ) {
			session.indexingPlanSynchronizationStrategy( IndexingPlanSynchronizationStrategy.sync() );

			IndexedEntity entity2 = new IndexedEntity();
			entity2.setId( 2 );
			entity2.setIndexedField( "initialValue" );

			session.indexingPlan().add( entity2 );

			// because of the override a sync strategy is used:
			backendMock.expectWorks( IndexedEntity.NAME, DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.FORCE )
					.add( "2", b -> b
							.field( "indexedField", entity2.getIndexedField() )
					);
		}
		backendMock.verifyExpectationsMet();
	}

	@Indexed(index = IndexedEntity.NAME)
	public static class IndexedEntity {

		static final String NAME = "IndexedEntity";

		@DocumentId
		private Integer id;

		@GenericField
		private String indexedField;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

	}
}
