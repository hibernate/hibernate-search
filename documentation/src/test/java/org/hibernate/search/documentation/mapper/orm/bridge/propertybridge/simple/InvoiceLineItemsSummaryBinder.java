/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.propertybridge.simple;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

//tag::binder[]
public class InvoiceLineItemsSummaryBinder implements PropertyBinder { // <1>

	@Override
	public void bind(PropertyBindingContext context) { // <2>
		context.dependencies() // <3>
				.use( "category" )
				.use( "amount" );

		IndexSchemaObjectField summaryField = context.indexSchemaElement() // <4>
				.objectField( "summary" );

		IndexFieldType<BigDecimal> amountFieldType = context.typeFactory() // <5>
				.asBigDecimal().decimalScale( 2 ).toIndexFieldType();

		context.bridge( List.class, new Bridge( // <6>
				summaryField.toReference(), // <7>
				summaryField.field( "total", amountFieldType ).toReference(), // <8>
				summaryField.field( "books", amountFieldType ).toReference(), // <8>
				summaryField.field( "shipping", amountFieldType ).toReference() // <8>
		) );
	}

	// ... class continues below
	//end::binder[]
	//tag::bridge[]
	// ... class InvoiceLineItemsSummaryBinder (continued)

	@SuppressWarnings("rawtypes")
	private static class Bridge implements PropertyBridge<List> { // <1>

		private final IndexObjectFieldReference summaryField;
		private final IndexFieldReference<BigDecimal> totalField;
		private final IndexFieldReference<BigDecimal> booksField;
		private final IndexFieldReference<BigDecimal> shippingField;

		private Bridge(IndexObjectFieldReference summaryField, // <2>
				IndexFieldReference<BigDecimal> totalField,
				IndexFieldReference<BigDecimal> booksField,
				IndexFieldReference<BigDecimal> shippingField) {
			this.summaryField = summaryField;
			this.totalField = totalField;
			this.booksField = booksField;
			this.shippingField = shippingField;
		}

		@Override
		public void write(DocumentElement target, List bridgedElement, PropertyBridgeWriteContext context) { // <3>
			@SuppressWarnings("unchecked")
			List<InvoiceLineItem> lineItems = (List<InvoiceLineItem>) bridgedElement;

			BigDecimal total = BigDecimal.ZERO;
			BigDecimal books = BigDecimal.ZERO;
			BigDecimal shipping = BigDecimal.ZERO;
			for ( InvoiceLineItem lineItem : lineItems ) { // <4>
				BigDecimal amount = lineItem.getAmount();
				total = total.add( amount );
				switch ( lineItem.getCategory() ) {
					case BOOK:
						books = books.add( amount );
						break;
					case SHIPPING:
						shipping = shipping.add( amount );
						break;
				}
			}

			DocumentElement summary = target.addObject( this.summaryField ); // <5>
			summary.addValue( this.totalField, total ); // <6>
			summary.addValue( this.booksField, books ); // <6>
			summary.addValue( this.shippingField, shipping ); // <6>
		}
	}
}
//end::bridge[]
