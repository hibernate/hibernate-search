/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalyzers;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public class ISBNBridge implements ValueBridge<ISBN, String> {

	@Override
	public StandardIndexFieldTypeOptionsStep<?, String> bind(ValueBridgeBindingContext<ISBN> context) {
		return context.getTypeFactory().asString()
				.normalizer( LibraryAnalyzers.NORMALIZER_ISBN )
				.projectionConverter( ISBNFromDocumentFieldValueConverter.INSTANCE );
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

	private static class ISBNFromDocumentFieldValueConverter implements FromDocumentFieldValueConverter<String, ISBN> {

		private static final ISBNFromDocumentFieldValueConverter INSTANCE =
				new ISBNFromDocumentFieldValueConverter();

		@Override
		public boolean isConvertedTypeAssignableTo(Class<?> superTypeCandidate) {
			return superTypeCandidate.isAssignableFrom( ISBN.class );
		}

		@Override
		public ISBN convert(String indexedValue, FromDocumentFieldValueConvertContext context) {
			return indexedValue == null ? null : new ISBN( indexedValue );
		}

		@Override
		public boolean isCompatibleWith(FromDocumentFieldValueConverter<?, ?> other) {
			return INSTANCE.equals( other );
		}
	}
}
