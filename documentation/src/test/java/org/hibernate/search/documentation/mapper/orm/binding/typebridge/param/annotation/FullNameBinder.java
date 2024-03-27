/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.typebridge.param.annotation;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

//tag::include[]
public class FullNameBinder implements TypeBinder {

	private boolean sortField;

	public FullNameBinder sortField(boolean sortField) { // <1>
		this.sortField = sortField;
		return this;
	}

	@Override
	public void bind(TypeBindingContext context) {
		context.dependencies()
				.use( "firstName" )
				.use( "lastName" );

		IndexFieldReference<String> fullNameField = context.indexSchemaElement()
				.field( "fullName", f -> f.asString().analyzer( "name" ) )
				.toReference();

		IndexFieldReference<String> fullNameSortField = null;
		if ( this.sortField ) { // <2>
			fullNameSortField = context.indexSchemaElement()
					.field(
							"fullName_sort",
							f -> f.asString().normalizer( "name" ).sortable( Sortable.YES )
					)
					.toReference();
		}

		context.bridge( Author.class, new Bridge(
				fullNameField,
				fullNameSortField
		) );
	}

	private static class Bridge implements TypeBridge<Author> {

		private final IndexFieldReference<String> fullNameField;
		private final IndexFieldReference<String> fullNameSortField;

		private Bridge(IndexFieldReference<String> fullNameField,
				IndexFieldReference<String> fullNameSortField) { // <2>
			this.fullNameField = fullNameField;
			this.fullNameSortField = fullNameSortField;
		}

		@Override
		public void write(
				DocumentElement target,
				Author author,
				TypeBridgeWriteContext context) {
			String fullName = author.getLastName() + " " + author.getFirstName();

			target.addValue( this.fullNameField, fullName );
			if ( this.fullNameSortField != null ) {
				target.addValue( this.fullNameSortField, fullName );
			}
		}
	}
}
//end::include[]
