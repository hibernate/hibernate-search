/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.dependency.PojoTypeIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoTypeIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class RoutingKeyBindingContextImpl<T> extends AbstractCompositeBindingContext
		implements RoutingKeyBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexedEntityBindingContext indexedEntityBindingContext;
	private final PojoModelTypeRootElement<T> bridgedElement;
	private final PojoTypeIndexingDependencyConfigurationContextImpl<T> dependencyContext;

	private PartialBinding<T> partialBinding;

	public RoutingKeyBindingContextImpl(BeanResolver beanResolver,
			IndexedEntityBindingContext indexedEntityBindingContext,
			PojoModelTypeRootElement<T> bridgedElement,
			PojoTypeIndexingDependencyConfigurationContextImpl<T> dependencyContext) {
		super( beanResolver );
		this.indexedEntityBindingContext = indexedEntityBindingContext;
		this.bridgedElement = bridgedElement;
		this.dependencyContext = dependencyContext;
	}

	@Override
	public void setBridge(RoutingKeyBridge bridge) {
		setBridge( BeanHolder.of( bridge ) );
	}

	@Override
	public void setBridge(BeanHolder<? extends RoutingKeyBridge> bridgeHolder) {
		this.partialBinding = new PartialBinding<>( bridgeHolder );
	}

	@Override
	public PojoModelType getBridgedElement() {
		return bridgedElement;
	}

	@Override
	public PojoTypeIndexingDependencyConfigurationContext getDependencies() {
		return dependencyContext;
	}

	public BoundRoutingKeyBridge<T> applyBinder(RoutingKeyBinder binder) {
		try {
			// This call should set the partial binding
			binder.bind( this );
			if ( partialBinding == null ) {
				throw log.missingBridgeForBinder( binder );
			}

			checkBridgeDependencies( bridgedElement, dependencyContext );

			return partialBinding.complete(
					indexedEntityBindingContext, bridgedElement, dependencyContext
			);
		}
		catch (RuntimeException e) {
			if ( partialBinding != null ) {
				partialBinding.abort( new SuppressingCloser( e ) );
			}
			throw e;
		}
		finally {
			partialBinding = null;
		}
	}

	private static class PartialBinding<T> {
		private final BeanHolder<? extends RoutingKeyBridge> bridgeHolder;

		private PartialBinding(BeanHolder<? extends RoutingKeyBridge> bridgeHolder) {
			this.bridgeHolder = bridgeHolder;
		}

		void abort(AbstractCloser<?, ?> closer) {
			closer.push( holder -> holder.get().close(), bridgeHolder );
			closer.push( BeanHolder::close, bridgeHolder );
		}

		BoundRoutingKeyBridge<T> complete(IndexedEntityBindingContext indexedEntityBindingContext,
				PojoModelTypeRootElement<T> bridgedElement,
				PojoTypeIndexingDependencyConfigurationContextImpl<T> dependencyContext) {
			indexedEntityBindingContext.explicitRouting();

			return new BoundRoutingKeyBridge<>(
					bridgeHolder,
					bridgedElement,
					dependencyContext
			);
		}
	}
}
