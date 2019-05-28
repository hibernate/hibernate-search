/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.DocumentRefreshStrategy;
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanMappingContext;
import org.hibernate.search.mapper.javabean.scope.SearchScope;
import org.hibernate.search.mapper.javabean.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.SearchSessionBuilder;
import org.hibernate.search.mapper.javabean.session.context.impl.JavaBeanSessionContext;
import org.hibernate.search.mapper.javabean.work.SearchWorkPlan;
import org.hibernate.search.mapper.javabean.work.impl.SearchWorkPlanImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;

public class JavaBeanSearchSession extends AbstractPojoSearchSession implements SearchSession {
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;
	private SearchWorkPlanImpl workPlan;

	private JavaBeanSearchSession(JavaBeanSearchSessionBuilder builder) {
		super( builder );
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
	public SearchScope scope(Collection<? extends Class<?>> targetedTypes) {
		return new SearchScopeImpl(
				getDelegate().createPojoSearchScope( targetedTypes )
		);
	}

	@Override
	public SearchWorkPlan getMainWorkPlan() {
		if ( workPlan == null ) {
			workPlan = new SearchWorkPlanImpl( getDelegate().createWorkPlan( commitStrategy, refreshStrategy ) );
		}
		return workPlan;
	}

	public static class JavaBeanSearchSessionBuilder extends AbstractBuilder<JavaBeanSearchSession>
			implements SearchSessionBuilder {
		private final JavaBeanMappingContext mappingContext;
		private String tenantId;
		private DocumentCommitStrategy commitStrategy = DocumentCommitStrategy.FORCE;
		private DocumentRefreshStrategy refreshStrategy = DocumentRefreshStrategy.NONE;

		public JavaBeanSearchSessionBuilder(PojoMappingDelegate mappingDelegate, JavaBeanMappingContext mappingContext) {
			super( mappingDelegate );
			this.mappingContext = mappingContext;
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

		@Override
		protected AbstractPojoSessionContextImplementor buildSessionContext() {
			return new JavaBeanSessionContext( mappingContext, tenantId, PojoRuntimeIntrospector.noProxy() );
		}

		@Override
		public JavaBeanSearchSession build() {
			return new JavaBeanSearchSession( this );
		}
	}
}
