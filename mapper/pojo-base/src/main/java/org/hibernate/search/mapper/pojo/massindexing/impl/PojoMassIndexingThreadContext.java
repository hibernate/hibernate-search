/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingThreadContext;

public class PojoMassIndexingThreadContext implements MassIndexingThreadContext {

	private final LoadingInvocationContext invocationContext;
	private final PojoMassIndexingIndexedTypeGroup typeGroup;

	public PojoMassIndexingThreadContext(LoadingInvocationContext invocationContext,
			PojoMassIndexingIndexedTypeGroup typeGroup) {
		this.invocationContext = invocationContext;
		this.typeGroup = typeGroup;
	}

	@Override
	public Object options() {
		return invocationContext.options();
	}

	@Override
	public Object context(Class contextType) {
		return invocationContext.context( contextType );
	}
}
