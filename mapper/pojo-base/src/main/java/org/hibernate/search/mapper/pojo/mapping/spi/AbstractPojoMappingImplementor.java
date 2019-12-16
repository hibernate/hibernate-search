/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.IdentifierBridgeToDocumentIdentifierContextImpl;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.ValueBridgeToIndexedValueContextImpl;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionMappingContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.impl.Closer;

public abstract class AbstractPojoMappingImplementor<M>
		implements MappingImplementor<M>, PojoScopeMappingContext, PojoSearchSessionMappingContext {

	private final PojoMappingDelegate delegate;

	private final List<CloseDelegate> closeDelegates = new ArrayList<>();
	private boolean closed = false;

	private final IdentifierBridgeToDocumentIdentifierContext toDocumentIdentifierContext;
	private final ValueBridgeToIndexedValueContext toIndexedValueContext;

	public AbstractPojoMappingImplementor(PojoMappingDelegate delegate) {
		this.delegate = delegate;
		this.toDocumentIdentifierContext = new IdentifierBridgeToDocumentIdentifierContextImpl( this );
		this.toIndexedValueContext = new ValueBridgeToIndexedValueContextImpl( this );
	}

	@Override
	public void close() {
		if ( !closed ) {
			// Make sure to avoid infinite recursion when one of the delegates calls this.close()
			closed = true;
			try ( Closer<RuntimeException> closer = new Closer<>() ) {
				closer.push( PojoMappingDelegate::close, delegate );
				closer.pushAll( CloseDelegate::close, closeDelegates );
			}
		}
	}

	@Override
	public final IdentifierBridgeToDocumentIdentifierContext getIdentifierBridgeToDocumentIdentifierContext() {
		return toDocumentIdentifierContext;
	}

	@Override
	public ValueBridgeToIndexedValueContext getValueBridgeToIndexedValueContext() {
		return toIndexedValueContext;
	}

	@Override
	public PojoIndexingPlan createIndexingPlan(PojoWorkSessionContext context, DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy) {
		return delegate.createIndexingPlan( context, commitStrategy, refreshStrategy );
	}

	@Override
	public PojoIndexer createIndexer(PojoWorkSessionContext context, DocumentCommitStrategy commitStrategy) {
		return delegate.createIndexer( context, commitStrategy );
	}

	protected final PojoMappingDelegate getDelegate() {
		return delegate;
	}

	public void onClose(CloseDelegate closeable) {
		closeDelegates.add( closeable );
	}

	public interface CloseDelegate extends AutoCloseable {
		@Override
		void close();
	}
}
