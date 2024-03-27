/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
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
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PropertyBindingContextImpl<P> extends AbstractCompositeBindingContext
		implements PropertyBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoBootstrapIntrospector introspector;
	private final PojoTypeModel<?> propertyTypeModel;
	private final PojoModelPropertyRootElement<P> bridgedElement;
	private final PojoPropertyIndexingDependencyConfigurationContextImpl<P> dependencyContext;
	private final IndexFieldTypeFactory indexFieldTypeFactory;
	private final PojoTreeContributionListener listener;
	private final IndexSchemaElement indexSchemaElement;

	private PartialBinding<P> partialBinding;

	public PropertyBindingContextImpl(BeanResolver beanResolver,
			PojoBootstrapIntrospector introspector,
			PojoTypeModel<P> propertyTypeModel,
			IndexBindingContext indexBindingContext,
			PojoModelPropertyRootElement<P> bridgedElement,
			PojoPropertyIndexingDependencyConfigurationContextImpl<P> dependencyContext,
			Map<String, Object> params) {
		super( beanResolver, params );
		this.introspector = introspector;
		this.propertyTypeModel = propertyTypeModel;
		this.bridgedElement = bridgedElement;
		this.dependencyContext = dependencyContext;
		this.indexFieldTypeFactory = indexBindingContext.createTypeFactory();
		this.listener = new PojoTreeContributionListener();
		this.indexSchemaElement = indexBindingContext.schemaElement( listener );
	}

	@Override
	public <P2> void bridge(Class<P2> expectedPropertyType, PropertyBridge<P2> bridge) {
		bridge( expectedPropertyType, BeanHolder.of( bridge ) );
	}

	@Override
	public <P2> void bridge(Class<P2> expectedPropertyType, BeanHolder<? extends PropertyBridge<P2>> bridgeHolder) {
		checkAndBind( bridgeHolder, introspector.typeModel( expectedPropertyType ) );
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

	@SuppressWarnings("resource") // For the eclipse-compiler: complains on bridge not bing closed
	private <P2> void checkAndBind(BeanHolder<? extends PropertyBridge<P2>> bridgeHolder,
			PojoRawTypeModel<?> expectedPropertyTypeModel) {
		if ( !propertyTypeModel.rawType().isSubTypeOf( expectedPropertyTypeModel ) ) {
			throw log.invalidInputTypeForBridge( bridgeHolder.get(), propertyTypeModel, expectedPropertyTypeModel );
		}

		@SuppressWarnings("unchecked") // We check that P extends P2 explicitly using reflection (see above)
		BeanHolder<? extends PropertyBridge<? super P>> castedBridgeHolder =
				(BeanHolder<? extends PropertyBridge<? super P>>) bridgeHolder;

		this.partialBinding = new PartialBinding<>( castedBridgeHolder );
	}

	private static class PartialBinding<P> {
		private final BeanHolder<? extends PropertyBridge<? super P>> bridgeHolder;

		private PartialBinding(BeanHolder<? extends PropertyBridge<? super P>> bridgeHolder) {
			this.bridgeHolder = bridgeHolder;
		}

		void abort(AbstractCloser<?, ?> closer) {
			closer.push( PropertyBridge::close, bridgeHolder, BeanHolder::get );
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
