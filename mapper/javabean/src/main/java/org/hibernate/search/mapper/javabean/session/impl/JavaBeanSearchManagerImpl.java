/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.session.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanMappingContext;
import org.hibernate.search.mapper.javabean.search.JavaBeanSearchTarget;
import org.hibernate.search.mapper.javabean.search.impl.JavaBeanSearchTargetImpl;
import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManager;
import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManagerBuilder;
import org.hibernate.search.mapper.javabean.session.context.impl.JavaBeanSessionContext;
import org.hibernate.search.mapper.javabean.work.JavaBeanWorkPlan;
import org.hibernate.search.mapper.javabean.work.impl.JavaBeanWorkPlanImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchManager;

public class JavaBeanSearchManagerImpl extends AbstractPojoSearchManager implements JavaBeanSearchManager {
	private JavaBeanWorkPlanImpl workPlan;

	private JavaBeanSearchManagerImpl(JavaBeanSearchManagerBuilderImpl builder) {
		super( builder );
	}

	@Override
	public void close() {
		if ( workPlan != null ) {
			CompletableFuture<?> future = workPlan.execute();
			future.join();
		}
	}

	@Override
	public <T> JavaBeanSearchTarget search(Collection<? extends Class<? extends T>> targetedTypes) {
		return new JavaBeanSearchTargetImpl(
				getDelegate().createPojoSearchTarget( targetedTypes )
		);
	}

	@Override
	public JavaBeanWorkPlan getMainWorkPlan() {
		if ( workPlan == null ) {
			workPlan = new JavaBeanWorkPlanImpl( getDelegate().createWorkPlan() );
		}
		return workPlan;
	}

	public static class JavaBeanSearchManagerBuilderImpl extends AbstractBuilder<JavaBeanSearchManagerImpl> implements JavaBeanSearchManagerBuilder {
		private final JavaBeanMappingContext mappingContext;
		private String tenantId;

		public JavaBeanSearchManagerBuilderImpl(PojoMappingDelegate mappingDelegate, JavaBeanMappingContext mappingContext) {
			super( mappingDelegate );
			this.mappingContext = mappingContext;
		}

		@Override
		public JavaBeanSearchManagerBuilderImpl tenantId(String tenantId) {
			this.tenantId = tenantId;
			return this;
		}

		@Override
		protected AbstractPojoSessionContextImplementor buildSessionContext() {
			return new JavaBeanSessionContext( mappingContext, tenantId, PojoRuntimeIntrospector.noProxy() );
		}

		@Override
		public JavaBeanSearchManagerImpl build() {
			return new JavaBeanSearchManagerImpl( this );
		}
	}
}
