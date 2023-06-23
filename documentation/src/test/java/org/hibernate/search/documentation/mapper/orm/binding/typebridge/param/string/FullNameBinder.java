/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.typebridge.param.string;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

//tag::include[]
public class FullNameBinder implements TypeBinder {

	@Override
	public void bind(TypeBindingContext context) {
		context.dependencies()
				.use( "firstName" )
				.use( "lastName" );

		IndexFieldReference<String> fullNameField = context.indexSchemaElement()
				.field( "fullName", f -> f.asString().analyzer( "name" ) )
				.toReference();

		IndexFieldReference<String> fullNameSortField = null;
		String sortField = context.param( "sortField", String.class ); // <1>
		if ( "true".equalsIgnoreCase( sortField ) ) { // <2>
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
