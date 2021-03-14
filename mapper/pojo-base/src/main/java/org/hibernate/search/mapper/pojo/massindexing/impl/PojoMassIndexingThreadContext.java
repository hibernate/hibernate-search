/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;
import org.hibernate.search.mapper.pojo.massindexing.loader.MassIndexingThreadContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingSessionContext;

public class PojoMassIndexingThreadContext implements MassIndexingThreadContext {

	private final LoadingInvocationContext invocationContext;
	private final PojoMassIndexingIndexedTypeGroup typeGroup;

	public PojoMassIndexingThreadContext(LoadingInvocationContext invocationContext,
			PojoMassIndexingIndexedTypeGroup typeGroup) {
		this.invocationContext = invocationContext;
		this.typeGroup = typeGroup;
	}

	@Override
	public boolean active() {
		return invocationContext.active().test();
	}

	@Override
	public Object options() {
		return invocationContext.options();
	}

	@Override
	public Object context(Class contextType) {
		return invocationContext.contextData().get( contextType );
	}

	@Override
	public String includedEntityNames() {
		return typeGroup.includedEntityNames();
	}

	@Override
	public Object entityIdentifier(Object entity) {
		MassIndexingSessionContext sessionContext = (MassIndexingSessionContext) context( MassIndexingSessionContext.class );
		return typeGroup.entityIdentifier( sessionContext, entity );
	}

	@Override
	public Class commonSuperType() {
		return typeGroup.commonSuperType().javaClass();
	}

	@Override
	public String commonSuperEntityName() {
		return typeGroup.commonSuperTypeEntityName();
	}

}
