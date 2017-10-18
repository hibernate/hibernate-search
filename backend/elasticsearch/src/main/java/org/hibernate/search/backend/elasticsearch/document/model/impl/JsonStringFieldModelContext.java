/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.model.spi.TerminalFieldModelContext;
import org.hibernate.search.engine.backend.document.spi.IndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class JsonStringFieldModelContext implements TerminalFieldModelContext<String>,
		ElasticsearchIndexModelNodeContributor<PropertyMapping> {

	private static final Gson GSON = new GsonBuilder().create();

	private DeferredInitializationIndexFieldReference<String> reference =
			new DeferredInitializationIndexFieldReference<>();

	private final UnknownTypeJsonAccessor accessor;

	private final String mappingJsonString;

	public JsonStringFieldModelContext(UnknownTypeJsonAccessor accessor, String mappingJsonString) {
		this.accessor = accessor;
		this.mappingJsonString = mappingJsonString;
	}

	@Override
	public IndexFieldReference<String> asReference() {
		return reference;
	}

	@Override
	public PropertyMapping contribute(ElasticsearchFieldModelCollector collector) {
		PropertyMapping mapping = GSON.fromJson( mappingJsonString, PropertyMapping.class );

		ElasticsearchFieldFormatter formatter = JsonStringFieldFormatter.INSTANCE;
		reference.initialize( new ElasticsearchIndexFieldReference<>( accessor, formatter ) );

		String absolutePath = accessor.getStaticAbsolutePath();
		ElasticsearchFieldModel model = new ElasticsearchFieldModel( formatter );
		collector.collect( absolutePath, model );

		return mapping;
	}

	private static final class JsonStringFieldFormatter implements ElasticsearchFieldFormatter {
		// Must be a singleton so that equals() works as required by the interface
		public static final JsonStringFieldFormatter INSTANCE = new JsonStringFieldFormatter();

		private JsonStringFieldFormatter() {
		}

		@Override
		public JsonElement format(Object object) {
			if ( object == null ) {
				return JsonNull.INSTANCE;
			}
			String jsonString = (String) object;
			return GSON.fromJson( jsonString, JsonElement.class );
		}

		@Override
		public Object parse(JsonElement element) {
			if ( element == null || element.isJsonNull() ) {
				return null;
			}
			return GSON.toJson( element );
		}
	}
}
