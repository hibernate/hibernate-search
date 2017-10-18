/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.index.spi.SearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManagerBuilder;
import org.hibernate.search.mapper.pojo.mapping.StreamPojoWorker;
import org.hibernate.search.mapper.pojo.model.spi.PojoProxyIntrospector;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.spi.SearchResultDefinitionContext;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class PojoSearchManagerImpl implements PojoSearchManager {

	private final PojoProxyIntrospector introspector;
	private final PojoTypeManagerContainer typeManagers;
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
		private final PojoTypeManagerContainer typeManagers;

		private String tenantId;

		public Builder(PojoProxyIntrospector introspector,
				PojoTypeManagerContainer typeManagers) {
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

	@Override
	public SearchResultDefinitionContext<PojoReference> search(Class<?>... indexedTypes) {
		List<Class<?>> targetedTypes = Arrays.asList( indexedTypes );
		Stream<PojoTypeManager<?, ?, ?>> targetedTypeManagers;
		if ( indexedTypes == null || indexedTypes.length == 0 ) {
			targetedTypeManagers = typeManagers.getAll();
		}
		else {
			targetedTypeManagers = targetedTypes.stream()
					.flatMap( t -> typeManagers.getAllBySuperType( t )
							.orElseThrow( () -> new SearchException( "Type " + t + " is not indexed and hasn't any indexed supertype." ) ) );
		}
		SearchTarget searchTarget = targetedTypeManagers.map( PojoTypeManager::createSearchTarget )
				.reduce( (a, b) -> {
					a.add( b );
					return a;
				} )
				// If we get here, there is at least one type manager
				.get();
		return searchTarget.search( context, this::toPojoReference );
	}

	private PojoReference toPojoReference(DocumentReference documentReference) {
		PojoTypeManager<?, ?, ?> typeManager = typeManagers.getByIndexName( documentReference.getIndexName() )
				.orElseThrow( () -> new AssertionFailure(
						"Document reference " + documentReference + " could not be converted to a PojoReference" ) );
		// TODO error handling if typeManager is null
		Object id = typeManager.getIdentifierMapping().fromDocumentId( documentReference.getId() );
		return new PojoReferenceImpl( typeManager.getEntityType(), id );
	}

}
