/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.typebridge.simple;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

//tag::binder[]
public class FullNameBinder implements TypeBinder { // <1>

	@Override
	public void bind(TypeBindingContext context) { // <2>
		context.dependencies() // <3>
				.use( "firstName" )
				.use( "lastName" );

		IndexFieldReference<String> fullNameField = context.indexSchemaElement() // <4>
				.field( "fullName", f -> f.asString().analyzer( "name" ) ) // <5>
				.toReference();

		context.bridge( // <6>
				Author.class, // <7>
				new Bridge( // <8>
						fullNameField // <9>
				)
		);
	}

	// ... class continues below
	//end::binder[]
	//tag::bridge[]
	// ... class FullNameBinder (continued)

	private static class Bridge // <1>
			implements TypeBridge<Author> { // <2>

		private final IndexFieldReference<String> fullNameField;

		private Bridge(IndexFieldReference<String> fullNameField) { // <3>
			this.fullNameField = fullNameField;
		}

		@Override
		public void write(
				DocumentElement target,
				Author author,
				TypeBridgeWriteContext context) { // <4>
			String fullName = author.getLastName() + " " + author.getFirstName(); // <5>
			target.addValue( this.fullNameField, fullName ); // <6>
		}
	}
}
//end::bridge[]
