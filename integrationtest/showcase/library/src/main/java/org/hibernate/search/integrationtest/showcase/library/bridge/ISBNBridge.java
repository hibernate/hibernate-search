/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalyzers;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBindingContext;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBridgeBuilder;
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
	public ISBN cast(Object value) {
		return (ISBN) value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	public static class Builder implements ValueBridgeBuilder {
		@Override
		public void bind(ValueBindingContext<?> context) {
			context.setBridge(
					ISBN.class, new ISBNBridge(),
					context.getTypeFactory().asString().normalizer( LibraryAnalyzers.NORMALIZER_ISBN )
			);
		}
	}

}
