/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.object;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

public class InvoiceLineItemsSummaryBinder implements PropertyBinder {

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

		IndexSchemaObjectField summaryField =
				schemaElement.objectField( "summary" ); // <1>

		IndexFieldType<BigDecimal> amountFieldType = context.typeFactory()
				.asBigDecimal().decimalScale( 2 )
				.toIndexFieldType();

		context.bridge( List.class, new Bridge(
				summaryField.toReference(), // <2>
				summaryField.field( "total", amountFieldType ) // <3>
						.toReference(),
				summaryField.field( "books", amountFieldType ) // <3>
						.toReference(),
				summaryField.field( "shipping", amountFieldType ) // <3>
						.toReference()
		) );
	}
	//end::bind[]

	@SuppressWarnings("rawtypes")
	private static class Bridge implements PropertyBridge<List> {

		private final IndexObjectFieldReference summaryField;
		private final IndexFieldReference<BigDecimal> totalField;
		private final IndexFieldReference<BigDecimal> booksField;
		private final IndexFieldReference<BigDecimal> shippingField;

		private Bridge(IndexObjectFieldReference summaryField,
				IndexFieldReference<BigDecimal> totalField,
				IndexFieldReference<BigDecimal> booksField,
				IndexFieldReference<BigDecimal> shippingField) {
			this.summaryField = summaryField;
			this.totalField = totalField;
			this.booksField = booksField;
			this.shippingField = shippingField;
		}

		//tag::write[]
		@Override
		public void write(DocumentElement target, List bridgedElement, PropertyBridgeWriteContext context) {
			@SuppressWarnings("unchecked")
			List<InvoiceLineItem> lineItems = (List<InvoiceLineItem>) bridgedElement;

			BigDecimal total = BigDecimal.ZERO;
			BigDecimal books = BigDecimal.ZERO;
			BigDecimal shipping = BigDecimal.ZERO;
			/* ... (computation of amounts, not relevant) ... */
			//end::write[]
			for ( InvoiceLineItem lineItem : lineItems ) {
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
			//tag::write[]

			DocumentElement summary = target.addObject( this.summaryField ); // <1>
			summary.addValue( this.totalField, total ); // <2>
			summary.addValue( this.booksField, books ); // <2>
			summary.addValue( this.shippingField, shipping ); // <2>
		}
		//end::write[]
	}
}
//end::bridge[]
