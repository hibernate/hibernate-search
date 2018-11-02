/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import org.hibernate.search.mapper.pojo.mapping.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;


/**
 * @author Yoann Rodiere
 */
public abstract class PojoSearchManagerImpl {

	private final PojoMappingDelegate mappingDelegate;
	private final PojoSessionContextImplementor sessionContext;

	protected PojoSearchManagerImpl(AbstractBuilder<? extends PojoSearchManagerImpl> builder) {
		this.mappingDelegate = builder.mappingDelegate;
		this.sessionContext = builder.buildSessionContext();
	}

	public PojoWorkPlan createWorkPlan() {
		return mappingDelegate.createWorkPlan( sessionContext );
	}

	protected final PojoMappingDelegate getMappingDelegate() {
		return mappingDelegate;
	}

	protected final PojoSessionContextImplementor getSessionContext() {
		return sessionContext;
	}

	protected abstract static class AbstractBuilder<T extends PojoSearchManagerImpl> {

		private final PojoMappingDelegate mappingDelegate;

		public AbstractBuilder(PojoMappingDelegate mappingDelegate) {
			this.mappingDelegate = mappingDelegate;
		}

		protected abstract PojoSessionContextImplementor buildSessionContext();

		public abstract T build();

	}

}
