/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping.impl;

import java.util.Collection;

import org.hibernate.search.mapper.javabean.JavaBeanSearchManagerBuilder;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchManager;
import org.hibernate.search.mapper.pojo.mapping.PojoSearchTarget;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchManagerImpl;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

class JavaBeanSearchManagerImpl extends PojoSearchManagerImpl {
	private JavaBeanSearchManagerImpl(Builder builder) {
		super( builder );
	}

	@Override
	public <T> PojoSearchTarget<?> search(Collection<? extends Class<? extends T>> targetedTypes) {
		return getMappingDelegate().createPojoSearchTarget( targetedTypes, getSessionContext() );
	}

	static class Builder extends PojoSearchManagerImpl.Builder<PojoSearchManager> implements JavaBeanSearchManagerBuilder {
		private String tenantId;

		public Builder(PojoMappingDelegate mappingDelegate) {
			super( mappingDelegate );
		}

		@Override
		public Builder tenantId(String tenantId) {
			this.tenantId = tenantId;
			return this;
		}

		@Override
		protected PojoRuntimeIntrospector getRuntimeIntrospector() {
			return PojoRuntimeIntrospector.noProxy();
		}

		@Override
		protected String getTenantId() {
			return tenantId;
		}

		@Override
		public PojoSearchManager build() {
			return new JavaBeanSearchManagerImpl( this );
		}
	}
}
