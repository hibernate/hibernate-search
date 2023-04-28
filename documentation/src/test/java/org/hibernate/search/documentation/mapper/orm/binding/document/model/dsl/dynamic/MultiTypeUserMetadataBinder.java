/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.document.model.dsl.dynamic;

import java.util.Map;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;

//tag::bind[]
public class MultiTypeUserMetadataBinder implements PropertyBinder {

	@Override
	public void bind(PropertyBindingContext context) {
		context.dependencies()
				/* ... (declaration of dependencies, not relevant) ... */
				//end::bind[]
				.useRootOnly();
		//tag::bind[]

		IndexSchemaElement schemaElement = context.indexSchemaElement();

		IndexSchemaObjectField userMetadataField =
				schemaElement.objectField( "multiTypeUserMetadata" ); // <1>

		userMetadataField.fieldTemplate( // <2>
				"userMetadataValueTemplate_int", // <3>
				f -> f.asInteger().sortable( Sortable.YES ) // <4>
		)
				.matchingPathGlob( "*_int" ); // <5>

		userMetadataField.fieldTemplate( // <6>
				"userMetadataValueTemplate_default",
				f -> f.asString().analyzer( "english" )
		);

		context.bridge( Map.class, new Bridge( userMetadataField.toReference() ) );
	}
	//end::bind[]

	//tag::write[]
	@SuppressWarnings("rawtypes")
	private static class Bridge implements PropertyBridge<Map> {

		private final IndexObjectFieldReference userMetadataFieldReference;

		private Bridge(IndexObjectFieldReference userMetadataFieldReference) {
			this.userMetadataFieldReference = userMetadataFieldReference;
		}

		@Override
		public void write(DocumentElement target, Map bridgedElement, PropertyBridgeWriteContext context) {
			@SuppressWarnings("unchecked")
			Map<String, Object> userMetadata = (Map<String, Object>) bridgedElement;

			DocumentElement indexedUserMetadata = target.addObject( userMetadataFieldReference ); // <1>

			for ( Map.Entry<String, Object> entry : userMetadata.entrySet() ) {
				String fieldName = entry.getKey();
				Object fieldValue = entry.getValue();
				indexedUserMetadata.addValue( fieldName, fieldValue ); // <2>
			}
		}
	}
	//end::write[]
	//tag::bind[]
}
//end::bind[]
