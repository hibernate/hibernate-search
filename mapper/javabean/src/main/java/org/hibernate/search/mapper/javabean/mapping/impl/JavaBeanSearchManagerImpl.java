/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManagerBuilder;
import org.hibernate.search.mapper.javabean.mapping.context.impl.JavaBeanMappingContextImpl;
import org.hibernate.search.mapper.javabean.search.JavaBeanSearchTarget;
import org.hibernate.search.mapper.javabean.search.impl.JavaBeanSearchTargetImpl;
import org.hibernate.search.mapper.javabean.session.JavaBeanSearchManager;
import org.hibernate.search.mapper.javabean.session.context.impl.JavaBeanSessionContextImpl;
import org.hibernate.search.mapper.pojo.mapping.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchManagerImpl;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

public class JavaBeanSearchManagerImpl extends PojoSearchManagerImpl implements JavaBeanSearchManager {
	private PojoWorkPlan workPlan;

	private JavaBeanSearchManagerImpl(Builder builder) {
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
				getMappingDelegate().createPojoSearchTarget( targetedTypes, getSessionContext() )
		);
	}

	@Override
	public PojoWorkPlan getMainWorkPlan() {
		if ( workPlan == null ) {
			workPlan = createWorkPlan();
		}
		return workPlan;
	}

	public static class Builder extends AbstractBuilder<JavaBeanSearchManagerImpl> implements JavaBeanSearchManagerBuilder {
		private final JavaBeanMappingContextImpl mappingContext;
		private String tenantId;

		public Builder(PojoMappingDelegate mappingDelegate, JavaBeanMappingContextImpl mappingContext) {
			super( mappingDelegate );
			this.mappingContext = mappingContext;
		}

		@Override
		public Builder tenantId(String tenantId) {
			this.tenantId = tenantId;
			return this;
		}

		@Override
		protected PojoSessionContextImplementor buildSessionContext() {
			return new JavaBeanSessionContextImpl( mappingContext, tenantId, PojoRuntimeIntrospector.noProxy() );
		}

		@Override
		public JavaBeanSearchManagerImpl build() {
			return new JavaBeanSearchManagerImpl( this );
		}
	}
}
