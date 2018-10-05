/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.showcase.library.bridge;

import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.integrationtest.showcase.library.analysis.LibraryAnalysisConfigurer;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.ValueBridgeBindingContext;
import org.hibernate.search.integrationtest.showcase.library.model.ISBN;

public class ISBNBridge implements ValueBridge<ISBN, String> {

	@Override
	public StandardIndexSchemaFieldTypedContext<?, String> bind(ValueBridgeBindingContext context) {
		return context.getIndexSchemaFieldContext().asString()
				.normalizer( LibraryAnalysisConfigurer.NORMALIZER_ISBN )
				.projectionConverter( this::fromIndexedValue );
	}

	@Override
	public String toIndexedValue(ISBN value) {
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

	private Object fromIndexedValue(String indexedValue) {
		return indexedValue == null ? null : new ISBN( indexedValue );
	}
}
