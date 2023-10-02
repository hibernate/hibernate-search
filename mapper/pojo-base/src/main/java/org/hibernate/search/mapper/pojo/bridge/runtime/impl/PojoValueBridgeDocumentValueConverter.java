/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.runtime.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContextExtension;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContextExtension;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeMappingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

public final class PojoValueBridgeDocumentValueConverter<V, F>
		implements ToDocumentValueConverter<V, F>, FromDocumentValueConverter<F, V> {

	private final ValueBridge<V, F> bridge;

	public PojoValueBridgeDocumentValueConverter(ValueBridge<V, F> bridge) {
		this.bridge = bridge;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + bridge + "]";
	}

	@Override
	public F toDocumentValue(V value, ToDocumentValueConvertContext context) {
		return bridge.toIndexedValue( value, context.extension( ContextExtension.INSTANCE ) );
	}

	@Override
	public V fromDocumentValue(F value, FromDocumentValueConvertContext context) {
		return bridge.fromIndexedValue( value, context.extension( ContextExtension.INSTANCE ) );
	}

	@Override
	public boolean isCompatibleWith(ToDocumentValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoValueBridgeDocumentValueConverter<?, ?> castedOther =
				(PojoValueBridgeDocumentValueConverter<?, ?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}

	@Override
	public boolean isCompatibleWith(FromDocumentValueConverter<?, ?> other) {
		if ( other == null || !getClass().equals( other.getClass() ) ) {
			return false;
		}
		PojoValueBridgeDocumentValueConverter<?, ?> castedOther =
				(PojoValueBridgeDocumentValueConverter<?, ?>) other;
		return bridge.isCompatibleWith( castedOther.bridge );
	}

	private static final class ContextExtension
			implements ToDocumentValueConvertContextExtension<ValueBridgeToIndexedValueContext>,
			FromDocumentValueConvertContextExtension<ValueBridgeFromIndexedValueContext> {
		private static final ContextExtension INSTANCE = new ContextExtension();

		@Override
		public Optional<ValueBridgeToIndexedValueContext> extendOptional(ToDocumentValueConvertContext original,
				BackendMappingContext mappingContext) {
			if ( mappingContext instanceof BridgeMappingContext ) {
				BridgeMappingContext pojoMappingContext = (BridgeMappingContext) mappingContext;
				return Optional.of( pojoMappingContext.valueBridgeToIndexedValueContext() );
			}
			else {
				return Optional.empty();
			}
		}

		@Override
		public Optional<ValueBridgeFromIndexedValueContext> extendOptional(FromDocumentValueConvertContext original,
				BackendSessionContext sessionContext) {
			if ( sessionContext instanceof BridgeSessionContext ) {
				BridgeSessionContext pojoSessionContext = (BridgeSessionContext) sessionContext;
				return Optional.of( pojoSessionContext.valueBridgeFromIndexedValueContext() );
			}
			else {
				return Optional.empty();
			}
		}
	}
}
