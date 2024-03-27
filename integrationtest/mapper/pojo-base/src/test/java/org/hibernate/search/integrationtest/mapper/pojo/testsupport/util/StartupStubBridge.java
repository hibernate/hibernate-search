/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.util;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingBridgeRouteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.route.DocumentRoutes;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.test.extension.StaticCounters;

/**
 * A stub bridge for use in tests where the bridge is only used on startup.
 * <p>
 * This is useful if we know the bridge will be filtered out, or simply if we don't test runtime at all.
 * <p>
 * This bridge contributes fields to the index schema where possible.
 * <p>
 * Any runtime use of this bridge will simply increment a counter and throw an exception.
 * <p>
 * For our own convenience, all bridge types are implemented in the same class.
 */
public class StartupStubBridge<T>
		implements TypeBridge<T>, PropertyBridge<T>, ValueBridge<T, String>,
		RoutingBridge<T>, IdentifierBridge<T> {
	public static class CounterKeys {
		public final StaticCounters.Key instance = StaticCounters.createKey();
		public final StaticCounters.Key runtimeUse = StaticCounters.createKey();
		public final StaticCounters.Key close = StaticCounters.createKey();
		public final StaticCounters.Key holderClose = StaticCounters.createKey();

		private CounterKeys() {
		}
	}

	public static CounterKeys createKeys() {
		return new CounterKeys();
	}

	public static Binder<Object> binder(CounterKeys counterKeys) {
		return new Binder<>( Object.class, counterKeys );
	}

	public static <T> Binder<T> binder(Class<T> bridgeInputType, CounterKeys counterKeys) {
		return new Binder<>( bridgeInputType, counterKeys );
	}

	private final CounterKeys counterKeys;

	private boolean closed = false;

	private StartupStubBridge(CounterKeys counterKeys) {
		StaticCounters.get().increment( counterKeys.instance );
		this.counterKeys = counterKeys;
	}

	@Override
	public void close() {
		/*
		 * This is important so that multiple calls to close on a single bridge
		 * won't be interpreted as closing multiple objects in test assertions.
		 */
		if ( closed ) {
			return;
		}
		StaticCounters.get().increment( counterKeys.close );
		closed = true;
	}

	@Override
	public void write(DocumentElement target, T bridgedElement, TypeBridgeWriteContext context) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public void write(DocumentElement target, T bridgedElement, PropertyBridgeWriteContext context) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public String toIndexedValue(Object value,
			ValueBridgeToIndexedValueContext context) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public void route(DocumentRoutes routes, Object entityIdentifier, T indexedEntity,
			RoutingBridgeRouteContext context) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public void previousRoutes(DocumentRoutes routes, Object entityIdentifier, T indexedEntity,
			RoutingBridgeRouteContext context) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public String toDocumentIdentifier(Object propertyValue,
			IdentifierBridgeToDocumentIdentifierContext context) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public T fromDocumentIdentifier(String documentIdentifier,
			IdentifierBridgeFromDocumentIdentifierContext context) {
		throw unexpectedRuntimeUse();
	}

	private AssertionFailure unexpectedRuntimeUse() {
		StaticCounters.get().increment( counterKeys.runtimeUse );
		return new AssertionFailure(
				"Instances of " + getClass().getSimpleName() + " are not supposed to be used at runtime,"
						+ " they should only be used to test the startup process."
		);
	}

	public static class Binder<T>
			implements RoutingBinder, TypeBinder, PropertyBinder, IdentifierBinder, ValueBinder {
		private final Class<T> bridgeInputType;
		private final StartupStubBridge.CounterKeys counterKeys;

		private Binder(Class<T> bridgeInputType, StartupStubBridge.CounterKeys counterKeys) {
			this.bridgeInputType = bridgeInputType;
			this.counterKeys = counterKeys;
		}

		@Override
		public void bind(RoutingBindingContext context) {
			context.dependencies().useRootOnly();
			context.bridge( bridgeInputType, build() );
		}

		@Override
		public void bind(TypeBindingContext context) {
			context.dependencies().useRootOnly();
			// Add at least one field so that the bridge is not removed
			context.indexSchemaElement().field(
					"startupStubBridgeFieldFromTypeBridge",
					f -> f.asString()
			)
					.toReference();
			context.bridge( bridgeInputType, build() );
		}

		@Override
		public void bind(PropertyBindingContext context) {
			context.dependencies().useRootOnly();
			// Add at least one field so that the bridge is not removed
			context.indexSchemaElement().field(
					"startupStubBridgeFieldFromPropertyBridge",
					f -> f.asString()
			)
					.toReference();
			context.bridge( bridgeInputType, build() );
		}

		@Override
		public void bind(IdentifierBindingContext<?> context) {
			context.bridge( bridgeInputType, build() );
		}

		@Override
		public void bind(ValueBindingContext<?> context) {
			context.bridge( bridgeInputType, build(), null );
		}

		private BeanHolder<StartupStubBridge<T>> build() {
			StartupStubBridge<T> bridge = new StartupStubBridge<>( counterKeys );
			return new CloseCountingBeanHolder<>( bridge, counterKeys.holderClose );
		}
	}
}
