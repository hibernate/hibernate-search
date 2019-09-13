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
import org.hibernate.search.engine.search.query.dsl.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.loading.spi.ReferenceHitMapper;
import org.hibernate.search.mapper.javabean.common.EntityReference;
import org.hibernate.search.mapper.javabean.scope.SearchScope;
import org.hibernate.search.mapper.javabean.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.SearchSessionBuilder;
import org.hibernate.search.mapper.javabean.session.context.impl.JavaBeanBackendSessionContext;
import org.hibernate.search.mapper.javabean.work.SearchWorkPlan;
import org.hibernate.search.mapper.javabean.work.impl.SearchWorkPlanImpl;
import org.hibernate.search.mapper.javabean.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;
import org.hibernate.search.util.common.AssertionFailure;

public class JavaBeanSearchSession extends AbstractPojoSearchSession
		implements SearchSession, ReferenceHitMapper<EntityReference> {

	private final JavaBeanSearchSessionMappingContext mappingContext;
	private final JavaBeanSearchSessionTypeContextProvider typeContextProvider;

	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;
	private SearchWorkPlanImpl workPlan;

	private JavaBeanSearchSession(JavaBeanSearchSessionBuilder builder) {
		super( builder, builder.buildSessionContext() );
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.commitStrategy = builder.commitStrategy;
		this.refreshStrategy = builder.refreshStrategy;
	}

	@Override
	public void close() {
		if ( workPlan != null ) {
			CompletableFuture<?> future = workPlan.execute();
			future.join();
		}
	}

	@Override
	public SearchQueryHitTypeStep<?, EntityReference, ?, ?, ?> search(Collection<? extends Class<?>> types) {
		return search( scope( types ) );
	}

	@Override
	public SearchQueryHitTypeStep<?, EntityReference, ?, ?, ?> search(SearchScope scope) {
		return search( (SearchScopeImpl) scope );
	}

	@Override
	public SearchScopeImpl scope(Collection<? extends Class<?>> types) {
		return mappingContext.createScope( types );
	}

	@Override
	public SearchWorkPlan getMainWorkPlan() {
		if ( workPlan == null ) {
			workPlan = new SearchWorkPlanImpl( getDelegate().createIndexingPlan( commitStrategy, refreshStrategy ) );
		}
		return workPlan;
	}

	@Override
	public EntityReference fromDocumentReference(DocumentReference reference) {
		JavaBeanSessionIndexedTypeContext<?> typeContext =
				typeContextProvider.getByIndexName( reference.getIndexName() );
		if ( typeContext == null ) {
			throw new AssertionFailure(
					"Document reference " + reference + " refers to an unknown index"
			);
		}
		Object id = typeContext.getIdentifierMapping()
				.fromDocumentIdentifier( reference.getId(), getDelegate().getBackendSessionContext() );
		return new EntityReferenceImpl( typeContext.getJavaClass(), id );
	}

	private SearchQueryHitTypeStep<?, EntityReference, ?, ?, ?> search(SearchScopeImpl scope) {
		return ( (SearchScopeImpl) scope ).search(
				getDelegate().getBackendSessionContext(),
				this
		);
	}

	public static class JavaBeanSearchSessionBuilder extends AbstractBuilder<JavaBeanSearchSession>
			implements SearchSessionBuilder {
		private final JavaBeanSearchSessionMappingContext mappingContext;
		private final JavaBeanSearchSessionTypeContextProvider typeContextProvider;
		private String tenantId;
		private DocumentCommitStrategy commitStrategy = DocumentCommitStrategy.FORCE;
		private DocumentRefreshStrategy refreshStrategy = DocumentRefreshStrategy.NONE;

		public JavaBeanSearchSessionBuilder(PojoMappingDelegate mappingDelegate,
				JavaBeanSearchSessionMappingContext mappingContext,
				JavaBeanSearchSessionTypeContextProvider typeContextProvider) {
			super( mappingDelegate );
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
		}

		@Override
		public JavaBeanSearchSessionBuilder tenantId(String tenantId) {
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

		protected AbstractPojoBackendSessionContext buildSessionContext() {
			return new JavaBeanBackendSessionContext(
					mappingContext.getBackendMappingContext(),
					tenantId,
					PojoRuntimeIntrospector.noProxy()
			);
		}

		@Override
		public JavaBeanSearchSession build() {
			return new JavaBeanSearchSession( this );
		}
	}
}
