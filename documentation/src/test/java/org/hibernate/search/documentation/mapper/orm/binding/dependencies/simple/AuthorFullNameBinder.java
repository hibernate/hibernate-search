/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.dependencies.simple;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

//tag::binder[]
public class AuthorFullNameBinder implements TypeBinder {

	@Override
	public void bind(TypeBindingContext context) {
		context.dependencies() // <1>
				.use( "author.firstName" ) // <2>
				.use( "author.lastName" ); // <3>

		IndexFieldReference<String> authorFullNameField = context.indexSchemaElement()
				.field( "authorFullName", f -> f.asString().analyzer( "name" ) )
				.toReference();

		context.bridge( Book.class, new Bridge( authorFullNameField ) );
	}

	private static class Bridge implements TypeBridge<Book> {

		// ...
		//end::binder[]

		private final IndexFieldReference<String> authorFullNameField;

		private Bridge(IndexFieldReference<String> authorFullNameField) { // <2>
			this.authorFullNameField = authorFullNameField;
		}

		@Override
		public void write(DocumentElement target, Book book, TypeBridgeWriteContext context) {
			Author author = book.getAuthor();

			String fullName = author.getLastName() + " " + author.getFirstName();

			target.addValue( this.authorFullNameField, fullName );
		}
		//tag::binder[]
	}
}
//end::binder[]
