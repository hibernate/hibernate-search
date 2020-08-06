/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.spi;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.SessionBasedBridgeOperationContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;


public abstract class AbstractPojoSearchSession<R> implements PojoWorkSessionContext<R> {

	private final PojoSearchSessionMappingContext mappingContext;

	private final SessionBasedBridgeOperationContext sessionBasedBridgeOperationContext;

	protected AbstractPojoSearchSession(PojoSearchSessionMappingContext mappingContext) {
		this.mappingContext = mappingContext;
		this.sessionBasedBridgeOperationContext = new SessionBasedBridgeOperationContext( this );
	}

	@Override
	public PojoSearchSessionMappingContext mappingContext() {
		return mappingContext;
	}

	@Override
	public final IdentifierBridgeFromDocumentIdentifierContext identifierBridgeFromDocumentIdentifierContext() {
		return sessionBasedBridgeOperationContext;
	}

	@Override
	public RoutingBridgeRouteContext routingBridgeRouteContext() {
		return sessionBasedBridgeOperationContext;
	}

	@Override
	public final TypeBridgeWriteContext typeBridgeWriteContext() {
		return sessionBasedBridgeOperationContext;
	}

	@Override
	public final PropertyBridgeWriteContext propertyBridgeWriteContext() {
		return sessionBasedBridgeOperationContext;
	}

	@Override
	public final ValueBridgeFromIndexedValueContext valueBridgeFromIndexedValueContext() {
		return sessionBasedBridgeOperationContext;
	}

	protected PojoIndexingPlan<R> createIndexingPlan(DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		return mappingContext.createIndexingPlan( this, commitStrategy, refreshStrategy );
	}

	protected PojoIndexer createIndexer() {
		return mappingContext.createIndexer( this );
	}

}
