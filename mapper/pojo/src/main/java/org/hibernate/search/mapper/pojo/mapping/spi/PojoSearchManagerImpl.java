/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManagerBuilder;
import org.hibernate.search.mapper.pojo.mapping.StreamPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoSessionContextImpl;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.dsl.SearchResultDefinitionContext;


/**
 * @author Yoann Rodiere
 */
public abstract class PojoSearchManagerImpl implements PojoSearchManager {

	private final PojoMappingDelegate mappingDelegate;
	private final PojoSessionContext sessionContext;
	private ChangesetPojoWorker changesetWorker;
	private StreamPojoWorker streamWorker;

	protected PojoSearchManagerImpl(Builder<? extends PojoSearchManager> builder) {
		this.mappingDelegate = builder.mappingDelegate;
		this.sessionContext = new PojoSessionContextImpl( builder.getProxyIntrospector(), builder.getTenantId() );
	}

	@Override
	public ChangesetPojoWorker getWorker() {
		if ( changesetWorker == null ) {
			changesetWorker = mappingDelegate.createWorker( sessionContext );
		}
		return changesetWorker;
	}

	@Override
	public StreamPojoWorker getStreamWorker() {
		if ( streamWorker == null ) {
			streamWorker = mappingDelegate.createStreamWorker( sessionContext );
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

	@Override
	public SearchResultDefinitionContext<PojoReference> search(Class<?>... targetedTypes) {
		PojoSearchTarget searchTarget = mappingDelegate.createPojoSearchTarget( targetedTypes );
		return searchTarget.search( sessionContext );
	}

	protected final PojoMappingDelegate getMappingDelegate() {
		return mappingDelegate;
	}

	protected final PojoSessionContext getSessionContext() {
		return sessionContext;
	}

	protected abstract static class Builder<T extends PojoSearchManager>
			implements PojoSearchManagerBuilder<T> {

		private final PojoMappingDelegate mappingDelegate;

		public Builder(PojoMappingDelegate mappingDelegate) {
			this.mappingDelegate = mappingDelegate;
		}

		protected abstract PojoProxyIntrospector getProxyIntrospector();

		protected abstract String getTenantId();

		@Override
		public abstract T build();

	}

}
