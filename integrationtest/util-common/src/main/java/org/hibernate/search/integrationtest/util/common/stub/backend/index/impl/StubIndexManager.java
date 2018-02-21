/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.index.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.ChangesetIndexWorker;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTargetBuilder;
import org.hibernate.search.engine.backend.index.spi.StreamIndexWorker;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.util.common.rule.StaticCounters;
import org.hibernate.search.integrationtest.util.common.stub.backend.index.StubIndexWork;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.impl.StubDocumentElement;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.model.StubIndexSchemaNode;

public class StubIndexManager implements IndexManager<StubDocumentElement> {

	public static final StaticCounters.Key INSTANCE_COUNTER_KEY = StaticCounters.createKey();
	public static final StaticCounters.Key CLOSE_COUNTER_KEY = StaticCounters.createKey();

	private final StubBackend backend;
	private final String name;

	StubIndexManager(StubBackend backend, String name, StubIndexSchemaNode rootSchemaNode) {
		StaticCounters.get().increment( INSTANCE_COUNTER_KEY );
		this.backend = backend;
		this.name = name;
		backend.getBehavior().pushSchema( name, rootSchemaNode );
	}

	@Override
	public void close() {
		StaticCounters.get().increment( CLOSE_COUNTER_KEY );
	}

	@Override
	public String toString() {
		return StubIndexManager.class.getSimpleName() + "[" + name + "]";
	}

	@Override
	public ChangesetIndexWorker<StubDocumentElement> createWorker(SessionContext context) {
		return new StubChangesetIndexWorker( this, context );
	}

	@Override
	public StreamIndexWorker<StubDocumentElement> createStreamWorker(SessionContext context) {
		return new StubStreamIndexWorker( this, context );
	}

	@Override
	public IndexSearchTargetBuilder createSearchTarget() {
		return new StubIndexSearchTarget.Builder( backend, name );
	}

	@Override
	public void addToSearchTarget(IndexSearchTargetBuilder searchTargetBuilder) {
		((StubIndexSearchTarget.Builder)searchTargetBuilder).add( backend, name );
	}

	void prepare(List<StubIndexWork> works) {
		backend.getBehavior().prepareWorks( name, works );
	}

	CompletableFuture<?> execute(List<StubIndexWork> works) {
		return backend.getBehavior().executeWorks( name, works );
	}
}
