/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManagerBuilder;
import org.hibernate.search.mapper.pojo.mapping.StreamPojoWorker;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;


/**
 * @author Yoann Rodiere
 */
public class PojoSearchManagerImpl implements PojoSearchManager {

	private final PojoProxyIntrospector introspector;
	private final Map<Class<?>, PojoTypeManager<?, ?, ?>> typeManagers;
	private final SessionContext context;
	private ChangesetPojoWorker changesetWorker;
	private StreamPojoWorker streamWorker;

	private PojoSearchManagerImpl(Builder builder) {
		this.introspector = builder.introspector;
		this.typeManagers = builder.typeManagers;
		this.context = new SessionContextImpl( builder.tenantId );
	}

	@Override
	public ChangesetPojoWorker getWorker() {
		if ( changesetWorker == null ) {
			changesetWorker = new ChangesetPojoWorkerImpl( introspector, typeManagers, context );
		}
		return changesetWorker;
	}

	@Override
	public StreamPojoWorker getStreamWorker() {
		if ( streamWorker == null ) {
			streamWorker = new StreamPojoWorkerImpl( introspector, typeManagers, context );
		}
		return streamWorker;
	}

	@Override
	public void close() {
		if ( changesetWorker != null ) {
			CompletableFuture<?> future = changesetWorker.execute();
			/*
			 * TODO decide whether we want the sync/async setting to be scoped per index,
			 * or per EntityManager/SearchManager, or both (with one scope overriding the other)
			 */
			future.join();
		}
	}

	protected static class Builder
			implements PojoSearchManagerBuilder<PojoSearchManager> {

		private final PojoProxyIntrospector introspector;
		private final Map<Class<?>, PojoTypeManager<?, ?, ?>> typeManagers;

		private String tenantId;

		public Builder(PojoProxyIntrospector introspector,
				Map<Class<?>, PojoTypeManager<?, ?, ?>> typeManagers) {
			this.introspector = introspector;
			this.typeManagers = typeManagers;
		}

		@Override
		public Builder tenantId(String tenantId) {
			this.tenantId = tenantId;
			return this;
		}

		@Override
		public PojoSearchManager build() {
			return new PojoSearchManagerImpl( this );
		}

	}

}
