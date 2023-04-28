/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.binding.dependencies.containers.property;

import java.util.Map;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.extractor.builtin.BuiltinContainerExtractors;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;

//tag::include[]
public class BookEditionsForSalePropertyBinder implements PropertyBinder {

	@Override
	public void bind(PropertyBindingContext context) {
		context.dependencies()
				.use( ContainerExtractorPath.explicitExtractor( BuiltinContainerExtractors.MAP_KEY ), // <1>
						"label" ); // <2>

		IndexFieldReference<String> editionsForSaleField = context.indexSchemaElement()
				.field( "editionsForSale", f -> f.asString().analyzer( "english" ) )
				.multiValued()
				.toReference();

		context.bridge( Map.class, new Bridge( editionsForSaleField ) );
	}

	@SuppressWarnings("rawtypes")
	private static class Bridge implements PropertyBridge<Map> {

		private final IndexFieldReference<String> editionsForSaleField;

		private Bridge(IndexFieldReference<String> editionsForSaleField) {
			this.editionsForSaleField = editionsForSaleField;
		}

		@Override
		public void write(DocumentElement target, Map bridgedElement, PropertyBridgeWriteContext context) {
			@SuppressWarnings("unchecked")
			Map<BookEdition, ?> priceByEdition = (Map<BookEdition, ?>) bridgedElement;

			for ( BookEdition edition : priceByEdition.keySet() ) { // <3>
				target.addValue( editionsForSaleField, edition.getLabel() );
			}
		}
	}
}
//end::include[]
