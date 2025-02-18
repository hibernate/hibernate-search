/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.injection;

import org.hibernate.search.documentation.testsupport.data.ISBN;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

//tag::bind[]
public class ISBNBinder implements PropertyBinder {

	@KeywordField(normalizer = "isbn")
	IndexFieldReference<String> isbn;

	IndexFieldReference<String> isbnNoAnnotation;

	@Override
	public void bind(PropertyBindingContext context) {
		context.dependencies()
				.useRootOnly();

		context.bridge( // <6>
				ISBN.class, // <7>
				new ISBNBridge( isbn ) // <8>
		);
	}
	//end::bind[]

	//tag::write[]
	private static class ISBNBridge implements PropertyBridge<ISBN> {

		private final IndexFieldReference<String> fieldReference;

		private ISBNBridge(IndexFieldReference<String> fieldReference) {
			this.fieldReference = fieldReference;
		}

		@Override
		public void write(DocumentElement target, ISBN bridgedElement, PropertyBridgeWriteContext context) {
			String indexedValue = /* ... (extraction of data, not relevant) ... */
					//end::write[]
					bridgedElement.getStringValue();
			//tag::write[]
			target.addValue( this.fieldReference, indexedValue ); // <1>
		}
	}
	//end::write[]
	//tag::bind[]
}
//end::bind[]
