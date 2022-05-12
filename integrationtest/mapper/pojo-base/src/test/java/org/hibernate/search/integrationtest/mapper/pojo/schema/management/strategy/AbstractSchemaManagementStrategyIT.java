/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.schema.management.strategy;

import java.lang.invoke.MethodHandles;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.mapper.javabean.cfg.JavaBeanMapperSettings;
import org.hibernate.search.mapper.javabean.mapping.CloseableSearchMapping;
import org.hibernate.search.mapper.javabean.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.SchemaManagementWorkBehavior;

import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractSchemaManagementStrategyIT {

	@Rule
	public final BackendMock backendMock = new BackendMock();

	@Rule
	public final JavaBeanMappingSetupHelper setupHelper
			= JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	public void noIndexedType() {
		SchemaManagementStrategyName strategyName = getStrategyName();
		CloseableSearchMapping mapper = setupHelper.start()
				.withProperty( JavaBeanMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
						strategyName == null ? null : strategyName.externalRepresentation()
				)
				.setup();
		// Nothing should have happened
		backendMock.verifyExpectationsMet();

		mapper.close();
		// Nothing should have happened
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void success() {
		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, CompletableFuture.completedFuture( null ) );

		CloseableSearchMapping mapper = setup();
		backendMock.verifyExpectationsMet();

		expectOnClose( IndexedEntity1.NAME );
		expectOnClose( IndexedEntity2.NAME );

		mapper.close();
		// Nothing should have happened
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void exception_single() {
		RuntimeException exception = new RuntimeException( "My exception" );

		expectWork( IndexedEntity1.NAME, CompletableFuture.completedFuture( null ) );
		expectWork( IndexedEntity2.NAME, exceptionFuture( exception ) );

		assertThatThrownBy( this::setup )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity2.class.getName() )
						.failure( "My exception" ) );
	}

	@Test
	public void exception_multiple() {
		RuntimeException exception1 = new RuntimeException( "My exception 1" );
		RuntimeException exception2 = new RuntimeException( "My exception 2" );
		expectWork( IndexedEntity1.NAME, exceptionFuture( exception1 ) );
		expectWork( IndexedEntity2.NAME, exceptionFuture( exception2 ) );

		assertThatThrownBy( this::setup )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( IndexedEntity1.class.getName() )
						.failure( "My exception 1" )
						.typeContext( IndexedEntity2.class.getName() )
						.failure( "My exception 2" ) );
	}

	protected abstract SchemaManagementStrategyName getStrategyName();

	protected final void expectWork(String indexName, CompletableFuture<?> future) {
		expectWork( indexName, ignored -> future );
	}

	protected abstract void expectWork(String indexName, SchemaManagementWorkBehavior behavior);

	protected abstract void expectOnClose(String indexName);

	protected final CompletableFuture<?> exceptionFuture(RuntimeException exception) {
		CompletableFuture<?> future = new CompletableFuture<>();
		future.completeExceptionally( exception );
		return future;
	}

	protected final CloseableSearchMapping setup() {
		backendMock.expectAnySchema( IndexedEntity1.NAME );
		backendMock.expectAnySchema( IndexedEntity2.NAME );
		SchemaManagementStrategyName strategyName = getStrategyName();
		return (CloseableSearchMapping) setupHelper.start()
				.withProperty( JavaBeanMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
						strategyName == null ? null : strategyName.externalRepresentation()
				)
				.setup( IndexedEntity1.class, IndexedEntity2.class );
	}

	@Indexed(index = IndexedEntity1.NAME)
	static class IndexedEntity1 {

		static final String NAME = "indexed1";

		@DocumentId
		private Integer id;
	}

	@Indexed(index = IndexedEntity2.NAME)
	static class IndexedEntity2 {

		static final String NAME = "indexed2";

		@DocumentId
		private Integer id;
	}
}
