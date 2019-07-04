/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.dependency.PojoTypeIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoTypeIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class TypeBindingContextImpl<T> extends AbstractCompositeBindingContext
		implements TypeBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoModelTypeRootElement<T> bridgedElement;
	private final PojoTypeIndexingDependencyConfigurationContextImpl<T> dependencyContext;
	private final IndexFieldTypeFactory indexFieldTypeFactory;
	private final PojoIndexSchemaContributionListener listener;
	private final IndexSchemaElement indexSchemaElement;

	private PartialBinding<T> partialBinding;

	public TypeBindingContextImpl(BeanResolver beanResolver,
			IndexBindingContext indexBindingContext,
			PojoModelTypeRootElement<T> bridgedElement,
			PojoTypeIndexingDependencyConfigurationContextImpl<T> dependencyContext) {
		super( beanResolver );
		this.bridgedElement = bridgedElement;
		this.dependencyContext = dependencyContext;
		this.indexFieldTypeFactory = indexBindingContext.createTypeFactory();
		this.listener = new PojoIndexSchemaContributionListener();
		this.indexSchemaElement = indexBindingContext.getSchemaElement( listener );
	}

	@Override
	public void setBridge(TypeBridge bridge) {
		setBridge( BeanHolder.of( bridge ) );
	}

	@Override
	public void setBridge(BeanHolder<? extends TypeBridge> bridgeHolder) {
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

	@Override
	public IndexFieldTypeFactory getTypeFactory() {
		return indexFieldTypeFactory;
	}

	@Override
	public IndexSchemaElement getIndexSchemaElement() {
		return indexSchemaElement;
	}

	public Optional<BoundTypeBridge<T>> applyBinder(TypeBinder binder) {
		try {
			// This call should set the partial binding
			binder.bind( this );
			if ( partialBinding == null ) {
				throw log.missingBridgeForBinder( binder );
			}

			checkBridgeDependencies( bridgedElement, dependencyContext );

			// If all fields are filtered out, we should ignore the bridge
			if ( !listener.isAnySchemaContributed() ) {
				try ( Closer<RuntimeException> closer = new Closer<>() ) {
					partialBinding.abort( closer );
				}
				return Optional.empty();
			}

			return Optional.of( partialBinding.complete(
					bridgedElement, dependencyContext
			) );
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
		private final BeanHolder<? extends TypeBridge> bridgeHolder;

		private PartialBinding(BeanHolder<? extends TypeBridge> bridgeHolder) {
			this.bridgeHolder = bridgeHolder;
		}

		void abort(AbstractCloser<?, ?> closer) {
			closer.push( holder -> holder.get().close(), bridgeHolder );
			closer.push( BeanHolder::close, bridgeHolder );
		}

		BoundTypeBridge<T> complete(PojoModelTypeRootElement<T> bridgedElement,
				PojoTypeIndexingDependencyConfigurationContextImpl<T> dependencyContext) {
			return new BoundTypeBridge<>(
					bridgeHolder,
					bridgedElement,
					dependencyContext
			);
		}
	}

}
