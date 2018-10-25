/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.test.util;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

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
public class StartupStubBridge
		implements TypeBridge, PropertyBridge, ValueBridge<Object, String>,
		RoutingKeyBridge, IdentifierBridge<Object> {
	public static class CounterKeys {
		public final StaticCounters.Key instance = StaticCounters.createKey();
		public final StaticCounters.Key runtimeUse = StaticCounters.createKey();
		public final StaticCounters.Key close = StaticCounters.createKey();

		private CounterKeys() {
		}
	}

	public static CounterKeys createKeys() {
		return new CounterKeys();
	}

	public final CounterKeys counterKeys;

	private boolean closed = false;

	public StartupStubBridge(CounterKeys counterKeys) {
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
	public void bind(PropertyBridgeBindingContext context) {
		// Add at least one field so that the bridge is not removed
		context.getIndexSchemaElement().field( "startupStubBridgeFieldFromPropertyBridge" )
				.asString().createAccessor();
	}

	@Override
	public void bind(TypeBridgeBindingContext context) {
		// Add at least one field so that the bridge is not removed
		context.getIndexSchemaElement().field( "startupStubBridgeFieldFromTypeBridge" )
				.asString().createAccessor();
	}

	@Override
	public void bind(RoutingKeyBridgeBindingContext context) {
		// Nothing to do
	}

	@Override
	public void write(DocumentElement target, PojoElement source, TypeBridgeWriteContext context) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public void write(DocumentElement target, PojoElement source, PropertyBridgeWriteContext context) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public Object cast(Object value) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public String toIndexedValue(Object value) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public String toRoutingKey(String tenantIdentifier, Object entityIdentifier, PojoElement source,
			RoutingKeyBridgeToRoutingKeyContext context) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public String toDocumentIdentifier(Object propertyValue,
			IdentifierBridgeToDocumentIdentifierContext context) {
		throw unexpectedRuntimeUse();
	}

	@Override
	public Object fromDocumentIdentifier(String documentIdentifier,
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
}
