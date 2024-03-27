/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.simple;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

public class SingleValuedNamesBinder implements TypeBinder {

	//tag::bind[]
	@Override
	public void bind(TypeBindingContext context) {
		context.dependencies()
				/* ... (declaration of dependencies, not relevant) ... */
				//end::bind[]
				.use( "firstName" )
				.use( "lastName" );
		//tag::bind[]

		IndexSchemaElement schemaElement = context.indexSchemaElement();

		IndexFieldType<String> nameType = context.typeFactory() // <1>
				.asString() // <2>
				.analyzer( "name" )
				.toIndexFieldType(); // <3>

		context.bridge( Author.class, new Bridge(
				schemaElement.field( "firstName", nameType ) // <4>
						.toReference(),
				schemaElement.field( "lastName", nameType ) // <4>
						.toReference(),
				schemaElement.field( "fullName", nameType ) // <4>
						.toReference()
		) );
	}
	//end::bind[]

	private static class Bridge implements TypeBridge<Author> {

		private final IndexFieldReference<String> firstNameField;
		private final IndexFieldReference<String> lastNameField;
		private final IndexFieldReference<String> fullNameField;

		private Bridge(IndexFieldReference<String> firstNameField,
				IndexFieldReference<String> lastNameField,
				IndexFieldReference<String> fullNameField) {
			this.firstNameField = firstNameField;
			this.lastNameField = lastNameField;
			this.fullNameField = fullNameField;
		}

		@Override
		public void write(DocumentElement target, Author author, TypeBridgeWriteContext context) {
			target.addValue( this.firstNameField, author.getFirstName() );
			target.addValue( this.lastNameField, author.getLastName() );
			target.addValue( this.fullNameField, author.getLastName() + " " + author.getFirstName() );
		}
	}
}
