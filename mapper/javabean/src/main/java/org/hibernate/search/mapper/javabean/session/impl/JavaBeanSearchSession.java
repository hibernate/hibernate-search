/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.javabean.scope.SearchScope;
import org.hibernate.search.mapper.javabean.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.SearchSessionBuilder;
import org.hibernate.search.mapper.javabean.work.SearchIndexer;
import org.hibernate.search.mapper.javabean.work.SearchIndexingPlan;
import org.hibernate.search.mapper.javabean.work.impl.SearchIndexerImpl;
import org.hibernate.search.mapper.javabean.work.impl.SearchIndexingPlanImpl;
import org.hibernate.search.mapper.javabean.common.impl.EntityReferenceImpl;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;

public class JavaBeanSearchSession extends AbstractPojoSearchSession<EntityReference>
		implements SearchSession, DocumentReferenceConverter<EntityReference>,
		EntityReferenceFactory<EntityReference> {

	private final JavaBeanSearchSessionMappingContext mappingContext;
	private final JavaBeanSearchSessionTypeContextProvider typeContextProvider;

	private final String tenantId;

	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;
	private SearchIndexingPlanImpl indexingPlan;
	private SearchIndexer indexer;

	private JavaBeanSearchSession(Builder builder) {
		super( builder.mappingContext );
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.tenantId = builder.tenantId;
		this.commitStrategy = builder.commitStrategy;
		this.refreshStrategy = builder.refreshStrategy;
	}

	@Override
	public void close() {
		if ( indexingPlan != null ) {
			CompletableFuture<?> future = indexingPlan.execute();
			Futures.unwrappedExceptionJoin( future );
		}
	}

	@Override
	public String getTenantIdentifier() {
		return tenantId;
	}

	@Override
	public PojoRuntimeIntrospector getRuntimeIntrospector() {
		return PojoRuntimeIntrospector.simple();
	}

	@Override
	public SearchQuerySelectStep<?, EntityReference, ?, ?, ?, ?> search(Collection<? extends Class<?>> types) {
		return search( scope( types ) );
	}

	@Override
	public SearchQuerySelectStep<?, EntityReference, ?, ?, ?, ?> search(SearchScope scope) {
		return search( (SearchScopeImpl) scope );
	}

	@Override
	public SearchScopeImpl scope(Collection<? extends Class<?>> types) {
		return mappingContext.createScope( types );
	}

	@Override
	public SearchIndexingPlan indexingPlan() {
		if ( indexingPlan == null ) {
			indexingPlan = new SearchIndexingPlanImpl(
					getRuntimeIntrospector(),
					createIndexingPlan( commitStrategy, refreshStrategy )
			);
		}
		return indexingPlan;
	}

	@Override
	public SearchIndexer indexer() {
		if ( indexer == null ) {
			indexer = new SearchIndexerImpl(
					getRuntimeIntrospector(),
					createIndexer(),
					commitStrategy, refreshStrategy
			);
		}
		return indexer;
	}

	@Override
	public EntityReference fromDocumentReference(DocumentReference reference) {
		JavaBeanSessionIndexedTypeContext<?> typeContext =
				typeContextProvider.getIndexedByEntityName( reference.typeName() );
		if ( typeContext == null ) {
			throw new AssertionFailure(
					"Document reference " + reference + " refers to an unknown type"
			);
		}
		Object id = typeContext.getIdentifierMapping()
				.fromDocumentIdentifier( reference.id(), this );
		return new EntityReferenceImpl( typeContext.getTypeIdentifier(), typeContext.getEntityName(), id );
	}

	@Override
	public EntityReferenceFactory<EntityReference> getEntityReferenceFactory() {
		return this;
	}

	@Override
	public EntityReference createEntityReference(String typeName, Object identifier) {
		JavaBeanSessionIndexedTypeContext<?> typeContext =
				typeContextProvider.getIndexedByEntityName( typeName );
		if ( typeContext == null ) {
			throw new AssertionFailure(
					"Type name " + typeName + " refers to an unknown type"
			);
		}
		return new EntityReferenceImpl( typeContext.getTypeIdentifier(), typeContext.getEntityName(), identifier );
	}

	private SearchQuerySelectStep<?, EntityReference, ?, ?, ?, ?> search(SearchScopeImpl scope) {
		return ( (SearchScopeImpl) scope ).search( this, this );
	}

	public static class Builder
			implements SearchSessionBuilder {
		private final JavaBeanSearchSessionMappingContext mappingContext;
		private final JavaBeanSearchSessionTypeContextProvider typeContextProvider;
		private String tenantId;
		private DocumentCommitStrategy commitStrategy = DocumentCommitStrategy.FORCE;
		private DocumentRefreshStrategy refreshStrategy = DocumentRefreshStrategy.NONE;

		public Builder(JavaBeanSearchSessionMappingContext mappingContext,
				JavaBeanSearchSessionTypeContextProvider typeContextProvider) {
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
		}

		@Override
		public Builder tenantId(String tenantId) {
			this.tenantId = tenantId;
			return this;
		}

		@Override
		public SearchSessionBuilder commitStrategy(DocumentCommitStrategy commitStrategy) {
			this.commitStrategy = commitStrategy;
			return this;
		}

		@Override
		public SearchSessionBuilder refreshStrategy(DocumentRefreshStrategy refreshStrategy) {
			this.refreshStrategy = refreshStrategy;
			return this;
		}

		@Override
		public JavaBeanSearchSession build() {
			return new JavaBeanSearchSession( this );
		}
	}
}
