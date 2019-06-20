/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.impl;

import java.util.Collection;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ReferenceHitMapper;
import org.hibernate.search.mapper.pojo.mapping.impl.PojoReferenceImpl;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeContainedTypeContextProvider;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeDelegateImpl;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContext;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContextProvider;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionDelegate;
import org.hibernate.search.mapper.pojo.work.impl.PojoSessionWorkExecutorImpl;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkPlanImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.util.common.AssertionFailure;

public final class PojoSearchSessionDelegateImpl implements PojoSearchSessionDelegate, ReferenceHitMapper<PojoReference> {

	private final PojoScopeIndexedTypeContextProvider indexedTypeContextProvider;
	private final PojoScopeContainedTypeContextProvider containedTypeContextProvider;
	private final AbstractPojoSessionContextImplementor sessionContext;

	public PojoSearchSessionDelegateImpl(PojoScopeIndexedTypeContextProvider indexedTypeContextProvider,
			PojoScopeContainedTypeContextProvider containedTypeContextProvider,
			AbstractPojoSessionContextImplementor sessionContext) {
		this.indexedTypeContextProvider = indexedTypeContextProvider;
		this.containedTypeContextProvider = containedTypeContextProvider;
		this.sessionContext = sessionContext;
	}

	@Override
	public AbstractPojoSessionContextImplementor getSessionContext() {
		return sessionContext;
	}

	@Override
	public <E, E2, C> PojoScopeDelegate<E2, C> createPojoScope(Collection<? extends Class<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider) {
		return PojoScopeDelegateImpl.create(
				indexedTypeContextProvider,
				containedTypeContextProvider,
				targetedTypes,
				indexedTypeExtendedContextProvider,
				sessionContext
		);
	}

	@Override
	public PojoWorkPlan createWorkPlan(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new PojoWorkPlanImpl(
				indexedTypeContextProvider, containedTypeContextProvider, sessionContext,
				commitStrategy, refreshStrategy
		);
	}

	@Override
	public PojoSessionWorkExecutor createSessionWorkExecutor(DocumentCommitStrategy commitStrategy) {
		return new PojoSessionWorkExecutorImpl( indexedTypeContextProvider, sessionContext, commitStrategy );
	}

	@Override
	public ReferenceHitMapper<PojoReference> getReferenceHitMapper() {
		return this;
	}

	@Override
	public PojoReference fromDocumentReference(DocumentReference documentReference) {
		PojoScopeIndexedTypeContext<?, ?, ?> typeContext =
				indexedTypeContextProvider.getByIndexName( documentReference.getIndexName() )
						.orElseThrow( () -> new AssertionFailure(
								"Document reference " + documentReference + " could not be converted to a PojoReference"
						) );
		Object id = typeContext.getIdentifierMapping()
				.fromDocumentIdentifier( documentReference.getId(), sessionContext );
		return new PojoReferenceImpl( typeContext.getJavaClass(), id );
	}

}
