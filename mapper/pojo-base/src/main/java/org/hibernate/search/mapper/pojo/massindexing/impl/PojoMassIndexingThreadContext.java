/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingThreadContext;

public class PojoMassIndexingThreadContext<O> implements MassIndexingThreadContext<O> {

	private final LoadingInvocationContext<O> invocationContext;

	public PojoMassIndexingThreadContext(LoadingInvocationContext<O> invocationContext) {
		this.invocationContext = invocationContext;
	}

	@Override
	public O options() {
		return invocationContext.options();
	}

	@Override
	public <T> T context(Class<T> contextType) {
		return invocationContext.context( contextType );
	}
}
