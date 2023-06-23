/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.object;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class InvoiceLineItemsDetailBinder implements PropertyBinder {

	//tag::bind[]
	@Override
	public void bind(PropertyBindingContext context) {
		context.dependencies()
				/* ... (declaration of dependencies, not relevant) ... */
				//end::bind[]
				.use( "category" )
				.use( "amount" );
		//tag::bind[]

		IndexSchemaElement schemaElement = context.indexSchemaElement();

		IndexSchemaObjectField lineItemsField =
				schemaElement.objectField( // <1>
						"lineItems", // <2>
						ObjectStructure.NESTED // <3>
				)
						.multiValued(); // <4>

		context.bridge( List.class, new Bridge(
				lineItemsField.toReference(), // <5>
				lineItemsField.field( "category", f -> f.asString() ) // <6>
						.toReference(),
				lineItemsField.field( "amount", f -> f.asBigDecimal().decimalScale( 2 ) ) // <7>
						.toReference()
		) );
	}
	//end::bind[]

	@SuppressWarnings("rawtypes")
	private static class Bridge implements PropertyBridge<List> {

		private final IndexObjectFieldReference lineItemsField;
		private final IndexFieldReference<String> categoryField;
		private final IndexFieldReference<BigDecimal> amountField;

		private Bridge(IndexObjectFieldReference lineItemsField,
				IndexFieldReference<String> categoryField,
				IndexFieldReference<BigDecimal> amountField) {
			this.lineItemsField = lineItemsField;
			this.categoryField = categoryField;
			this.amountField = amountField;
		}

		@Override
		public void write(DocumentElement target, List bridgedElement, PropertyBridgeWriteContext context) {
			@SuppressWarnings("unchecked")
			List<InvoiceLineItem> lineItems = (List<InvoiceLineItem>) bridgedElement;

			for ( InvoiceLineItem lineItem : lineItems ) {
				DocumentElement indexedLineItem = target.addObject( this.lineItemsField );
				indexedLineItem.addValue( this.categoryField, lineItem.getCategory().name() );
				indexedLineItem.addValue( this.amountField, lineItem.getAmount() );
			}
		}
	}
}
//end::bridge[]
