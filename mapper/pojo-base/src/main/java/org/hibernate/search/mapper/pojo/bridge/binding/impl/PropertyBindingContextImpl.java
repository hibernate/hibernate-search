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
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;
import org.hibernate.search.mapper.pojo.model.dependency.PojoPropertyIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoPropertyIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelPropertyRootElement;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PropertyBindingContextImpl<P> extends AbstractCompositeBindingContext
		implements PropertyBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoModelPropertyRootElement<P> bridgedElement;
	private final PojoPropertyIndexingDependencyConfigurationContextImpl<P> dependencyContext;
	private final IndexFieldTypeFactory indexFieldTypeFactory;
	private final PojoIndexSchemaContributionListener listener;
	private final IndexSchemaElement indexSchemaElement;

	private PartialBinding<P> partialBinding;

	public PropertyBindingContextImpl(BeanResolver beanResolver,
			IndexBindingContext indexBindingContext,
			PojoModelPropertyRootElement<P> bridgedElement,
			PojoPropertyIndexingDependencyConfigurationContextImpl<P> dependencyContext) {
		super( beanResolver );
		this.bridgedElement = bridgedElement;
		this.dependencyContext = dependencyContext;
		this.indexFieldTypeFactory = indexBindingContext.createTypeFactory();
		this.listener = new PojoIndexSchemaContributionListener();
		this.indexSchemaElement = indexBindingContext.schemaElement( listener );
	}

	@Override
	public void bridge(PropertyBridge bridge) {
		bridge( BeanHolder.of( bridge ) );
	}

	@Override
	public void bridge(BeanHolder<? extends PropertyBridge> bridgeHolder) {
		this.partialBinding = new PartialBinding<>( bridgeHolder );
	}

	@Override
	public PojoModelProperty bridgedElement() {
		return bridgedElement;
	}

	@Override
	public PojoPropertyIndexingDependencyConfigurationContext dependencies() {
		return dependencyContext;
	}

	@Override
	public IndexFieldTypeFactory typeFactory() {
		return indexFieldTypeFactory;
	}

	@Override
	public IndexSchemaElement indexSchemaElement() {
		return indexSchemaElement;
	}

	public Optional<BoundPropertyBridge<P>> applyBinder(PropertyBinder binder) {
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

	private static class PartialBinding<P> {
		private final BeanHolder<? extends PropertyBridge> bridgeHolder;

		private PartialBinding(BeanHolder<? extends PropertyBridge> bridgeHolder) {
			this.bridgeHolder = bridgeHolder;
		}

		void abort(AbstractCloser<?, ?> closer) {
			closer.push( holder -> holder.get().close(), bridgeHolder );
			closer.push( BeanHolder::close, bridgeHolder );
		}

		BoundPropertyBridge<P> complete(PojoModelPropertyRootElement<P> bridgedElement,
				PojoPropertyIndexingDependencyConfigurationContextImpl<P> dependencyContext) {
			return new BoundPropertyBridge<>(
					bridgeHolder,
					bridgedElement,
					dependencyContext
			);
		}
	}
}
