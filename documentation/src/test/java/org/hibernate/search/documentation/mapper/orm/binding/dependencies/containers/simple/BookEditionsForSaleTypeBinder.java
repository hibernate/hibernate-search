/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.dependencies.containers.simple;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;

//tag::include[]
public class BookEditionsForSaleTypeBinder implements TypeBinder {

	@Override
	public void bind(TypeBindingContext context) {
		context.dependencies()
				.use( PojoModelPath.builder() // <1>
						.property( "priceByEdition" ) // <2>
						.value( BuiltinContainerExtractors.MAP_KEY ) // <3>
						.property( "label" ) // <4>
						.toValuePath() ); // <5>

		IndexFieldReference<String> editionsForSaleField = context.indexSchemaElement()
				.field( "editionsForSale", f -> f.asString().analyzer( "english" ) )
				.multiValued()
				.toReference();

		context.bridge( Book.class, new Bridge( editionsForSaleField ) );
	}

	private static class Bridge implements TypeBridge<Book> {

		private final IndexFieldReference<String> editionsForSaleField;

		private Bridge(IndexFieldReference<String> editionsForSaleField) {
			this.editionsForSaleField = editionsForSaleField;
		}

		@Override
		public void write(DocumentElement target, Book book, TypeBridgeWriteContext context) {
			for ( BookEdition edition : book.getPriceByEdition().keySet() ) { // <6>
				target.addValue( editionsForSaleField, edition.getLabel() );
			}
		}
	}
}
//end::include[]
