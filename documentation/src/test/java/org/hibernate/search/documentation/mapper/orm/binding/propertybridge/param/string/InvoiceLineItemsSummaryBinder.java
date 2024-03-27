/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.propertybridge.param.string;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.search.documentation.mapper.orm.binding.propertybridge.param.annotation.InvoiceLineItem;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

//tag::include[]
public class InvoiceLineItemsSummaryBinder implements PropertyBinder {

	@Override
	public void bind(PropertyBindingContext context) {
		context.dependencies()
				.use( "category" )
				.use( "amount" );

		String fieldName = context.param( "fieldName", String.class ); // <1>
		IndexSchemaObjectField summaryField = context.indexSchemaElement()
				.objectField( fieldName ); // <2>

		IndexFieldType<BigDecimal> amountFieldType = context.typeFactory()
				.asBigDecimal().decimalScale( 2 ).toIndexFieldType();

		context.bridge( List.class, new Bridge(
				summaryField.toReference(),
				summaryField.field( "total", amountFieldType ).toReference(),
				summaryField.field( "books", amountFieldType ).toReference(),
				summaryField.field( "shipping", amountFieldType ).toReference()
		) );
	}

	@SuppressWarnings("rawtypes")
	private static class Bridge implements PropertyBridge<List> {

		/* ... same implementation as before ... */

		//end::include[]
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

		@Override
		public void write(DocumentElement target, List bridgedElement, PropertyBridgeWriteContext context) {
			@SuppressWarnings("unchecked")
			List<InvoiceLineItem> lineItems = (List<InvoiceLineItem>) bridgedElement;

			BigDecimal total = BigDecimal.ZERO;
			BigDecimal books = BigDecimal.ZERO;
			BigDecimal shipping = BigDecimal.ZERO;
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

			DocumentElement summary = target.addObject( this.summaryField );
			summary.addValue( this.totalField, total );
			summary.addValue( this.booksField, books );
			summary.addValue( this.shippingField, shipping );
		}
		//tag::include[]
	}
}
//end::include[]
