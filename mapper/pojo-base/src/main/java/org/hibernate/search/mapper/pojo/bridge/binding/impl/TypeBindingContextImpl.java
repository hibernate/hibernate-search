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
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.mapper.pojo.model.dependency.PojoTypeIndexingDependencyConfigurationContext;
import org.hibernate.search.mapper.pojo.model.dependency.impl.PojoTypeIndexingDependencyConfigurationContextImpl;
import org.hibernate.search.mapper.pojo.model.impl.PojoModelTypeRootElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.common.impl.AbstractCloser;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class TypeBindingContextImpl<T> extends AbstractCompositeBindingContext
		implements TypeBindingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoBootstrapIntrospector introspector;
	private final PojoTypeModel<T> typeModel;
	private final PojoModelTypeRootElement<T> bridgedElement;
	private final PojoTypeIndexingDependencyConfigurationContextImpl<T> dependencyContext;
	private final IndexFieldTypeFactory indexFieldTypeFactory;
	private final PojoTreeContributionListener listener;
	private final IndexSchemaElement indexSchemaElement;

	private PartialBinding<T> partialBinding;

	public TypeBindingContextImpl(BeanResolver beanResolver,
			PojoBootstrapIntrospector introspector,
			PojoTypeModel<T> typeModel,
			IndexBindingContext indexBindingContext,
			PojoModelTypeRootElement<T> bridgedElement,
			PojoTypeIndexingDependencyConfigurationContextImpl<T> dependencyContext,
			Map<String, Object> params) {
		super( beanResolver, params );
		this.introspector = introspector;
		this.typeModel = typeModel;
		this.bridgedElement = bridgedElement;
		this.dependencyContext = dependencyContext;
		this.indexFieldTypeFactory = indexBindingContext.createTypeFactory();
		this.listener = new PojoTreeContributionListener();
		this.indexSchemaElement = indexBindingContext.schemaElement( listener );
	}

	@Override
	public <T2> void bridge(Class<T2> expectedEntityType, TypeBridge<T2> bridge) {
		bridge( expectedEntityType, BeanHolder.of( bridge ) );
	}

	@Override
	public <T2> void bridge(Class<T2> expectedEntityType, BeanHolder<? extends TypeBridge<T2>> bridgeHolder) {
		checkAndBind( bridgeHolder, introspector.typeModel( expectedEntityType ) );
	}

	@Override
	public PojoModelType bridgedElement() {
		return bridgedElement;
	}

	@Override
	public PojoTypeIndexingDependencyConfigurationContext dependencies() {
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

	@SuppressWarnings("resource") // For the eclipse-compiler: complains on bridge not bing closed
	private <T2> void checkAndBind(BeanHolder<? extends TypeBridge<T2>> bridgeHolder,
			PojoRawTypeModel<?> expectedPropertyTypeModel) {
		if ( !typeModel.rawType().isSubTypeOf( expectedPropertyTypeModel ) ) {
			throw log.invalidInputTypeForBridge( bridgeHolder.get(), typeModel, expectedPropertyTypeModel );
		}

		@SuppressWarnings("unchecked") // We check that T extends T2 explicitly using reflection (see above)
		BeanHolder<? extends TypeBridge<? super T>> castedBridgeHolder =
				(BeanHolder<? extends TypeBridge<? super T>>) bridgeHolder;

		this.partialBinding = new PartialBinding<>( castedBridgeHolder );
	}

	private static class PartialBinding<T> {
		private final BeanHolder<? extends TypeBridge<? super T>> bridgeHolder;

		private PartialBinding(BeanHolder<? extends TypeBridge<? super T>> bridgeHolder) {
			this.bridgeHolder = bridgeHolder;
		}

		void abort(AbstractCloser<?, ?> closer) {
			closer.push( TypeBridge::close, bridgeHolder, BeanHolder::get );
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
