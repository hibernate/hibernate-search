/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementType;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;
import org.hibernate.search.engine.bridge.builtin.spatial.GeoPoint;
import org.hibernate.search.engine.bridge.builtin.spatial.ImmutableGeoPoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
class GeoPointFieldModelContext extends AbstractScalarFieldModelContext<GeoPoint> {

	private final UnknownTypeJsonAccessor accessor;

	public GeoPointFieldModelContext(UnknownTypeJsonAccessor accessor) {
		this.accessor = accessor;
	}

	@Override
	protected PropertyMapping contribute(DeferredInitializationIndexFieldReference<GeoPoint> reference,
			ElasticsearchFieldModelCollector collector) {
		PropertyMapping mapping = super.contribute( reference, collector );

		ElasticsearchFieldModel model = new ElasticsearchFieldModel( GeoPointFieldFormatter.INSTANCE );

		reference.initialize( new ElasticsearchIndexFieldReference<>( accessor, model ) );
		mapping.setType( DataType.GEO_POINT );

		String absolutePath = accessor.getStaticAbsolutePath();
		collector.collect( absolutePath, model );

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
