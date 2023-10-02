/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalyzers;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public class ISBNBridge implements ValueBridge<ISBN, String> {

	private ISBNBridge() {
		// Private, use the builder instead
	}

	@Override
	public String toIndexedValue(ISBN value,
			ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.getStringValue();
	}

	@Override
	public ISBN fromIndexedValue(String value, ValueBridgeFromIndexedValueContext context) {
		return value == null ? null : new ISBN( value );
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	public static class Binder implements ValueBinder {
		@Override
		public void bind(ValueBindingContext<?> context) {
			context.bridge(
					ISBN.class, new ISBNBridge(),
					context.typeFactory().asString().normalizer( LibraryAnalyzers.NORMALIZER_ISBN )
			);
		}
	}

}
