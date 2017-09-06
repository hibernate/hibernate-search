/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.backend.elasticsearch.document.impl.DeferredInitializationIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchIndexFieldReference;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.DataType;
import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;
import org.hibernate.search.engine.bridge.builtin.spatial.GeoPoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
class GeoPointFieldModelContext extends AbstractScalarFieldModelContext<GeoPoint> {

	private final UnknownTypeJsonAccessor accessor;

	public GeoPointFieldModelContext(UnknownTypeJsonAccessor accessor) {
		this.accessor = accessor;
	}

	@Override
	protected void contribute(DeferredInitializationIndexFieldReference<GeoPoint> reference, PropertyMapping mapping, ElasticsearchFieldModelCollector collector) {
		super.contribute( reference, mapping, collector );
		ElasticsearchFieldFormatter formatter = GeoPointFieldFormatter.INSTANCE;
		reference.initialize( new ElasticsearchIndexFieldReference<>( accessor, formatter ) );
		mapping.setType( DataType.GEO_POINT );

		String absolutePath = accessor.getStaticAbsolutePath();
		ElasticsearchFieldModel model = new ElasticsearchFieldModel( formatter );
		collector.collect( absolutePath, model );
	}

	private static final class GeoPointFieldFormatter implements ElasticsearchFieldFormatter {
		// Must be a singleton so that equals() works as required by the interface
		public static final GeoPointFieldFormatter INSTANCE = new GeoPointFieldFormatter();

		private GeoPointFieldFormatter() {
		}

		@Override
		public JsonElement format(Object object) {
			if ( object == null ) {
				return JsonNull.INSTANCE;
			}
			GeoPoint value = (GeoPoint) object;
			JsonObject result = new JsonObject();
			result.addProperty( "lat", value.getLatitude() );
			result.addProperty( "lon", value.getLongitude() );
			return result;
		}
	}
}
