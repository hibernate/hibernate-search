/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

public class StubIndexManager implements IndexManagerImplementor, IndexManager {

	public static final StaticCounters.Key INSTANCE_COUNTER_KEY = StaticCounters.createKey();
	public static final StaticCounters.Key STOP_COUNTER_KEY = StaticCounters.createKey();

	private final StubBackend backend;
	private final String name;
	private final String mappedTypeName;
	private final StubIndexSchemaNode rootSchemaNode;

	private boolean running = true;

	StubIndexManager(StubBackend backend, String name, String mappedTypeName,
			StubIndexSchemaNode rootSchemaNode) {
		StaticCounters.get().increment( INSTANCE_COUNTER_KEY );
		this.backend = backend;
		this.name = name;
		this.mappedTypeName = mappedTypeName;
		this.rootSchemaNode = rootSchemaNode;
		backend.getBehavior().pushSchema( name, rootSchemaNode );
	}

	@Override
	public CompletableFuture<?> start(IndexManagerStartContext context) {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public CompletableFuture<?> preStop() {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void stop() {
		/*
		 * This is important so that multiple calls to close on a single index manager
		 * won't be interpreted as closing multiple objects in test assertions.
		 */
		if ( !running ) {
			return;
		}
		StaticCounters.get().increment( STOP_COUNTER_KEY );
		running = false;
	}

	@Override
	public String toString() {
		return StubIndexManager.class.getSimpleName() + "[" + name + "]";
	}

	@Override
	public <R> IndexIndexingPlan<R> createIndexingPlan(BackendSessionContext context,
			EntityReferenceFactory<R> entityReferenceFactory,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new StubIndexIndexingPlan<>(
				name, mappedTypeName, backend.getBehavior(),
				context, entityReferenceFactory, commitStrategy, refreshStrategy
		);
	}

	@Override
	public IndexIndexer createIndexer(BackendSessionContext context,
			DocumentCommitStrategy commitStrategy) {
		return new StubIndexIndexer( name, backend.getBehavior(), context, commitStrategy );
	}

	@Override
	public IndexWorkspace createWorkspace(DetachedBackendSessionContext sessionContext) {
		return new StubIndexWorkspace( name, backend.getBehavior(), sessionContext );
	}

	@Override
	public IndexScopeBuilder createScopeBuilder(BackendMappingContext mappingContext) {
		return new StubIndexScope.Builder( backend, mappingContext, name, rootSchemaNode );
	}

	@Override
	public void addTo(IndexScopeBuilder builder) {
		((StubIndexScope.Builder) builder ).add( backend, name, rootSchemaNode );
	}

	@Override
	public IndexManager toAPI() {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( IndexManager.class ) ) {
			return (T) this;
		}
		throw new SearchException( "Cannot unwrap " + this + " to " + clazz );
	}

}
