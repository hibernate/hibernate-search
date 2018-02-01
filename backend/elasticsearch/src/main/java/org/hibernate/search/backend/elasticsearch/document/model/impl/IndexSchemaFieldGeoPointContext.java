/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldAccessor;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementType;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.engine.backend.spatial.ImmutableGeoPoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class IndexSchemaFieldGeoPointContext extends AbstractScalarFieldTypedContext<GeoPoint> {

	private final String relativeName;

	public IndexSchemaFieldGeoPointContext(String relativeName) {
		super( relativeName, DataType.GEO_POINT );
		this.relativeName = relativeName;
	}

	@Override
	protected PropertyMapping contribute(DeferredInitializationIndexFieldAccessor<GeoPoint> reference,
			ElasticsearchIndexSchemaNodeCollector collector,
			ElasticsearchIndexSchemaObjectNode parentNode) {
		PropertyMapping mapping = super.contribute( reference, collector, parentNode );

		ElasticsearchIndexSchemaFieldNode node = new ElasticsearchIndexSchemaFieldNode( parentNode, GeoPointFieldFormatter.INSTANCE );

		JsonAccessor<JsonElement> jsonAccessor = JsonAccessor.root().property( relativeName );
		reference.initialize( new ElasticsearchIndexFieldAccessor<>( jsonAccessor, node ) );

		String absolutePath = parentNode.getAbsolutePath( relativeName );
		collector.collect( absolutePath, node );

		return mapping;
	}

	private static final class GeoPointFieldFormatter implements ElasticsearchFieldFormatter {
		// Must be a singleton so that equals() works as required by the interface
		public static final GeoPointFieldFormatter INSTANCE = new GeoPointFieldFormatter();

		private static final JsonAccessor<Double> LATITUDE_ACCESSOR =
				JsonAccessor.root().property( "lat" ).asDouble();
		private static final JsonAccessor<Double> LONGITUDE_ACCESSOR =
				JsonAccessor.root().property( "lon" ).asDouble();

		private GeoPointFieldFormatter() {
		}

		@Override
		public JsonElement format(Object object) {
			if ( object == null ) {
				return JsonNull.INSTANCE;
			}
			GeoPoint value = (GeoPoint) object;
			JsonObject result = new JsonObject();
			LATITUDE_ACCESSOR.set( result, value.getLatitude() );
			LONGITUDE_ACCESSOR.set( result, value.getLongitude() );
			return result;
		}

		@Override
		public Object parse(JsonElement element) {
			if ( element == null || element.isJsonNull() ) {
				return null;
			}
			JsonObject object = JsonElementType.OBJECT.fromElement( element );
			double latitude = LATITUDE_ACCESSOR.get( object ).get();
			double longitude = LONGITUDE_ACCESSOR.get( object ).get();
			return new ImmutableGeoPoint( latitude, longitude );
		}
	}

}
