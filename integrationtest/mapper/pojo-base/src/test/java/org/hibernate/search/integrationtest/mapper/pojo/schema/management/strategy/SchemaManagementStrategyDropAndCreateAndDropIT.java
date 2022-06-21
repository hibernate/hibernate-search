/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.strategy;

import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.hibernate.search.mapper.pojo.standalone.mapping.CloseableSearchMapping;

import org.hibernate.search.util.impl.integrationtest.common.rule.SchemaManagementWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubSchemaManagementWork;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.util.common.SearchException;
import org.junit.Test;

public class SchemaManagementStrategyDropAndCreateAndDropIT extends AbstractSchemaManagementStrategyIT {

	@Override
	protected SchemaManagementStrategyName getStrategyName() {
		return SchemaManagementStrategyName.DROP_AND_CREATE_AND_DROP;
	}

	@Override
	protected void expectWork(String indexName, SchemaManagementWorkBehavior behavior) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.DROP_AND_CREATE, behavior );
	}

	@Override
	protected void expectOnClose(String indexName) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.DROP_IF_EXISTING );
	}

	protected void expectOnClose(String indexName, CompletableFuture<?> future) {
		backendMock.expectSchemaManagementWorks( indexName )
				.work( StubSchemaManagementWork.Type.DROP_IF_EXISTING, future );
	}

	@Test
	public void close_drop_exception_single() {
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );
		CloseableSearchMapping mapping = setup();
		backendMock.verifyExpectationsMet();
		RuntimeException exception = new RuntimeException( "My exception" );
		expectOnClose( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectOnClose( IndexedEntity2.NAME, exceptionFuture( exception ) );
		assertThatThrownBy( mapping::close )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Hibernate Search encountered failures during shutdown",
						IndexedEntity2.class.getName()
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void close_drop_exception_multiple() {
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );
		CloseableSearchMapping mapping = setup();
		backendMock.verifyExpectationsMet();
		RuntimeException exception1 = new RuntimeException( "My exception 1" );
		RuntimeException exception2 = new RuntimeException( "My exception 2" );
		expectOnClose( IndexedEntity1.NAME, exceptionFuture( exception1 ) );
		expectOnClose( IndexedEntity2.NAME, exceptionFuture( exception2 ) );
		assertThatThrownBy( mapping::close )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Hibernate Search encountered failures during shutdown",
						IndexedEntity1.class.getName(),
						IndexedEntity2.class.getName()
				);
		mapping.close();
		backendMock.verifyExpectationsMet();
	}
}
