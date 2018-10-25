/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import org.hibernate.search.engine.backend.document.converter.FromIndexFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.FromIndexFieldValueConvertContext;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalysisConfigurer;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public class ISBNBridge implements ValueBridge<ISBN, String> {

	private static final ISBNFromIndexFieldValueConverter FROM_INDEX_FIELD_VALUE_CONVERTER =
			new ISBNFromIndexFieldValueConverter();

	@Override
	public StandardIndexSchemaFieldTypedContext<?, String> bind(ValueBridgeBindingContext<ISBN> context) {
		return context.getIndexSchemaFieldContext().asString()
				.normalizer( LibraryAnalysisConfigurer.NORMALIZER_ISBN )
				.projectionConverter( FROM_INDEX_FIELD_VALUE_CONVERTER );
	}

	@Override
	public String toIndexedValue(ISBN value,
			ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.getStringValue();
	}

	@Override
	public ISBN cast(Object value) {
		return (ISBN) value;
	}

	@Override
	public boolean isCompatibleWith(ValueBridge<?, ?> other) {
		return getClass().equals( other.getClass() );
	}

	private static class ISBNFromIndexFieldValueConverter implements FromIndexFieldValueConverter<String, ISBN> {

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( ISBN.class );
		}

		@Override
		public ISBN convert(String indexedValue, FromIndexFieldValueConvertContext context) {
			return indexedValue == null ? null : new ISBN( indexedValue );
		}
	}
}
