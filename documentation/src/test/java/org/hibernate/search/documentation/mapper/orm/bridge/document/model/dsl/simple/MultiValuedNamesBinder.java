/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.document.model.dsl.simple;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;

public class MultiValuedNamesBinder implements TypeBinder {

	//tag::bind[]
	@Override
	public void bind(TypeBindingContext context) {
		context.getDependencies()
				/* ... (declaration of dependencies, not relevant) ... */
				//end::bind[]
				.use( "firstName" )
				.use( "lastName" );
		//tag::bind[]

		IndexSchemaElement schemaElement = context.getIndexSchemaElement();

		context.setBridge( new Bridge(
				schemaElement.field( "names", f -> f.asString().analyzer( "name" ) )
						.multiValued() // <1>
						.toReference()
		) );
	}
	//end::bind[]

	private static class Bridge implements TypeBridge {

		private final IndexFieldReference<String> namesField;

		private Bridge(IndexFieldReference<String> namesField) {
			this.namesField = namesField;
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
			Author author = (Author) bridgedElement;
			target.addValue( this.namesField, author.getFirstName() );
			target.addValue( this.namesField, author.getLastName() );
		}
	}
}
