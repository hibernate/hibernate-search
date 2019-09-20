/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.typebridge.simple;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

//tag::binder[]
public class FullNameBinder implements TypeBinder<FullNameBinding> { // <1>

	@Override
	public void bind(TypeBindingContext context) { // <2>
		context.getDependencies() // <3>
				.use( "firstName" )
				.use( "lastName" );

		IndexFieldReference<String> fullNameField = context.getIndexSchemaElement() // <4>
				.field( "fullName", f -> f.asString().analyzer( "name" ) ) // <5>
				.toReference();

		context.setBridge( new Bridge( // <6>
				fullNameField // <7>
		) );
	}

	// ... class continues below
	//end::binder[]
	//tag::bridge[]
	// ... class FullNameBinder (continued)

	private static class Bridge implements TypeBridge { // <1>

		private final IndexFieldReference<String> fullNameField;

		private Bridge(IndexFieldReference<String> fullNameField) { // <2>
			this.fullNameField = fullNameField;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) { // <3>
			Author author = (Author) bridgedElement; // <4>

			String fullName = author.getLastName() + " " + author.getFirstName(); // <5>

			target.addValue( this.fullNameField, fullName ); // <6>
		}
	}
}
//end::bridge[]
