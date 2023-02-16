/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl.StubIndexModel;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

public class StubIndexManager implements IndexManagerImplementor, IndexManager {

	public static final StaticCounters.Key INSTANCE_COUNTER_KEY = StaticCounters.createKey();
	public static final StaticCounters.Key STOP_COUNTER_KEY = StaticCounters.createKey();

	private final StubBackend backend;
	private final String name;
	private final StubIndexModel model;

	private State state = State.STOPPED;

	StubIndexManager(StubBackend backend, String name, StubIndexModel model) {
		StaticCounters.get().increment( INSTANCE_COUNTER_KEY );
		this.backend = backend;
		this.name = name;
		this.model = model;
		backend.getBehavior().defineSchema( name, model );
	}

	@Override
	public void start(IndexManagerStartContext context) {
		this.state = State.STARTED;
		// Nothing to do
	}

	@Override
	public CompletableFuture<?> preStop() {
		this.state = State.STOPPING;
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void stop() {
		/*
		 * This is important so that multiple calls to close on a single index manager
		 * won't be interpreted as closing multiple objects in test assertions.
		 */
		if ( State.STOPPED.equals( state ) ) {
			return;
		}
		StaticCounters.get().increment( STOP_COUNTER_KEY );
		state = State.STOPPED;
	}

	@Override
	public String toString() {
		return StubIndexManager.class.getSimpleName() + "[" + name + "]";
	}

	public String getName() {
		return name;
	}

	@Override
	public IndexSchemaManager schemaManager() {
		checkStarted();
		return new StubIndexSchemaManager( name, backend.getBehavior() );
	}

	@Override
	public IndexIndexingPlan createIndexingPlan(BackendSessionContext context,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		checkStarted();
		return new StubIndexIndexingPlan(
				name, model.mappedTypeName(), backend.getBehavior(),
				context, commitStrategy, refreshStrategy
		);
	}

	@Override
	public IndexIndexer createIndexer(BackendSessionContext context) {
		checkStarted();
		return new StubIndexIndexer( name, backend.getBehavior(), context );
	}

	@Override
	public IndexWorkspace createWorkspace(BackendMappingContext mappingContext, Set<String> tenantIds) {
		checkStarted();
		return new StubIndexWorkspace( name, backend.getBehavior(), tenantIds );
	}

	@Override
	public IndexScopeBuilder createScopeBuilder(BackendMappingContext mappingContext) {
		checkStarted();
		return new StubIndexScope.Builder( backend, mappingContext, model );
	}

	@Override
	public void addTo(IndexScopeBuilder builder) {
		checkStarted();
		((StubIndexScope.Builder) builder ).add( backend, model );
	}

	@Override
	public IndexManager toAPI() {
		return this;
	}

	@Override
	public Backend backend() {
		return backend;
	}

	@Override
	public IndexDescriptor descriptor() {
		throw new UnsupportedOperationException( "Metamodel not supported in the stub backend" );
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( IndexManager.class ) ) {
			return (T) this;
		}
		throw new SearchException( "Cannot unwrap " + this + " to " + clazz );
	}

	private void checkStarted() {
		assertThat( state ).isEqualTo( State.STARTED );
	}

	private enum State {
		STOPPED,
		STARTED,
		STOPPING
	}
}
