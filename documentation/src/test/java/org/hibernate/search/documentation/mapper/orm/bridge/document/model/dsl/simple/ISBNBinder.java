/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.document.model.dsl.simple;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class ISBNBinder implements PropertyBinder<ISBNBinding> {

	//tag::bind[]
	@Override
	public void bind(PropertyBindingContext context) {
		context.getDependencies()
				/* ... (declaration of dependencies, not relevant) ... */
				//end::bind[]
				.useRootOnly();
		//tag::bind[]

		IndexSchemaElement schemaElement = context.getIndexSchemaElement(); // <1>

		IndexFieldReference<String> field =
				schemaElement.field( // <2>
						"isbn", // <3>
						f -> f.asString() // <4>
								.normalizer( "isbn" )
				)
						.toReference(); // <5>

		context.setBridge( new Bridge( field ) ); // <6>
	}
	//end::bind[]

	private static class Bridge implements PropertyBridge {

		private final IndexFieldReference<String> fieldReference;

		private Bridge(IndexFieldReference<String> fieldReference) {
			this.fieldReference = fieldReference;
		}

		//tag::write[]
		@Override
		public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
			String indexedValue = /* ... (extraction of data, not relevant) ... */
					//end::write[]
					( (ISBN) bridgedElement ).getStringValue();
			//tag::write[]
			target.addValue( this.fieldReference, indexedValue ); // <1>
		}
		//end::write[]
	}
}
