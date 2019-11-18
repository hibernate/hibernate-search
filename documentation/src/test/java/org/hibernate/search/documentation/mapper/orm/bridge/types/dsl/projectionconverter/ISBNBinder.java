/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.types.dsl.projectionconverter;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class ISBNBinder implements PropertyBinder {

	@Override
	public void bind(PropertyBindingContext context) {
		context.getDependencies().useRootOnly();

		//tag::include[]
		IndexFieldType<String> type = context.getTypeFactory()
				.asString() // <1>
				.projectable( Projectable.YES )
				.projectionConverter( // <2>
						ISBN.class, // <3>
						(value, convertContext) -> ISBN.parse( value ) // <4>
				)
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
