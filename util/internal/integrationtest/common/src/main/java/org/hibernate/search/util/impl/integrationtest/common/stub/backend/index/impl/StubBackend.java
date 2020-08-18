/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendBehavior;

public class StubBackend implements BackendImplementor, Backend {

	private final EventContext eventContext;
	private final StubBackendBehavior behavior;
	private final TimingSource timingSource;

	StubBackend(EventContext eventContext, BackendBuildContext context, StubBackendBehavior behavior,
			TimingSource timingSource) {
		this.eventContext = eventContext;
		this.behavior = behavior;
		this.timingSource = timingSource;
		behavior.onCreateBackend( context );
	}

	@Override
	public String toString() {
		return StubBackend.class.getSimpleName() + "[" + eventContext + "]";
	}

	public EventContext eventContext() {
		return eventContext;
	}

	@Override
	public void start(BackendStartContext context) {
		// Nothing to do
	}

	@Override
	public CompletableFuture<?> preStop() {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void stop() {
		behavior.onStopBackend();
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		throw new AssertionFailure( getClass().getName() + " cannot be unwrapped" );
	}

	@Override
	public Backend toAPI() {
		return this;
	}

	public StubBackendBehavior getBehavior() {
		return behavior;
	}

	public TimingSource timingSource() {
		return timingSource;
	}

	@Override
	public IndexManagerBuilder createIndexManagerBuilder(String indexName,
			String mappedTypeName, boolean isMultiTenancyEnabled, BackendBuildContext context,
			ConfigurationPropertySource propertySource) {
		return new StubIndexManagerBuilder( this, indexName, mappedTypeName, mappedTypeName );
	}
}
