/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.types.dsl.dslconverter;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.ToDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class ISBNBinder implements PropertyBinder<ISBNBinding> {

	@Override
	public void bind(PropertyBindingContext context) {
		context.getDependencies().useRootOnly();

		//tag::include[]
		IndexFieldType<String> type = context.getTypeFactory()
				.asString() // <1>
				.normalizer( "isbn" )
				.sortable( Sortable.YES )
				.dslConverter( new ToDocumentFieldValueConverter<ISBN, String>() { // <2>
					@Override
					public boolean isValidInputType(Class<?> inputTypeCandidate) { // <3>
						return ISBN.class.isAssignableFrom( inputTypeCandidate );
					}

					@Override
					public String convert(ISBN value, ToDocumentFieldValueConvertContext context) { // <4>
						return value.getStringValue();
					}

					@Override
					public String convertUnknown(Object value, ToDocumentFieldValueConvertContext context) { // <5>
						return convert( (ISBN) value, context );
					}
				} )
				.toIndexFieldType();
		//end::include[]

		context.setBridge( new Bridge(
				context.getIndexSchemaElement()
						.field( "isbn", type )
						.toReference()
		) );
	}

	private static class Bridge implements PropertyBridge {
		private final IndexFieldReference<String> isbnField;

		private Bridge(IndexFieldReference<String> isbnField) {
			this.isbnField = isbnField;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement,
				PropertyBridgeWriteContext context) {
			target.addValue( isbnField, ((ISBN) bridgedElement).getStringValue() );
		}
	}
}
