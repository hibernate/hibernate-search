/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.Projectable;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelCompositeElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.common.StreamHelper;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * @author Yoann Rodiere
 */
public class GeoPointBridge implements TypeBridge, PropertyBridge {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	public static class Builder implements
			AnnotationBridgeBuilder<GeoPointBridge, org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.GeoPointBridge> {

		private String fieldName;
		private Projectable projectable = Projectable.DEFAULT;
		private String markerSet;

		@Override
		public void initialize(
				org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.GeoPointBridge annotation) {
			fieldName( annotation.fieldName() );
			markerSet( annotation.markerSet() );
			projectable( annotation.projectable() );
		}

		public Builder fieldName(String fieldName) {
			this.fieldName = fieldName;
			return this;
		}

		public Builder projectable(Projectable projectable) {
			this.projectable = projectable;
			return this;
		}

		public Builder markerSet(String markerSet) {
			this.markerSet = markerSet;
			return this;
		}

		@Override
		public GeoPointBridge build(BridgeBuildContext buildContext) {
			return new GeoPointBridge( fieldName, projectable, markerSet );
		}

	}

	private final String fieldName;
	private final Projectable projectable;
	private final String markerSet;

	private IndexFieldAccessor<GeoPoint> fieldAccessor;
	private Function<PojoElement, GeoPoint> coordinatesExtractor;

	/*
	 * Private constructor, use the Builder instead.
	 */
	private GeoPointBridge(String fieldName, Projectable projectable, String markerSet) {
		this.fieldName = fieldName;
		this.projectable = projectable;
		this.markerSet = markerSet;
	}

	@Override
	public void bind(TypeBridgeBindingContext context) {
		if ( fieldName == null || fieldName.isEmpty() ) {
			throw log.missingFieldNameForGeoPointBridgeOnType( context.getBridgedElement().toString() );
		}

		bind( fieldName, context.getIndexSchemaElement(), context.getBridgedElement() );
	}

	@Override
	public void bind(PropertyBridgeBindingContext context) {
		String defaultedFieldName;
		if ( fieldName != null && !fieldName.isEmpty() ) {
			defaultedFieldName = fieldName;
		}
		else {
			defaultedFieldName = context.getBridgedElement().getName();
		}

		bind( defaultedFieldName, context.getIndexSchemaElement(), context.getBridgedElement() );
	}

	private void bind(String defaultedFieldName, IndexSchemaElement indexSchemaElement,
			PojoModelCompositeElement bridgedPojoModelElement) {
		fieldAccessor = indexSchemaElement.field( defaultedFieldName ).asGeoPoint().projectable( projectable ).createAccessor();

		if ( bridgedPojoModelElement.isAssignableTo( GeoPoint.class ) ) {
			PojoModelElementAccessor<GeoPoint> sourceAccessor = bridgedPojoModelElement.createAccessor( GeoPoint.class );
			coordinatesExtractor = sourceAccessor::read;
		}
		else {
			PojoModelElementAccessor<Double> latitudeAccessor = bridgedPojoModelElement.properties()
					.filter( model -> model.markers( LatitudeMarker.class )
							.anyMatch( m -> Objects.equals( markerSet, m.getMarkerSet() ) ) )
					.collect( singleMarkedProperty( "latitude", defaultedFieldName, markerSet ) )
					.createAccessor( Double.class );
			PojoModelElementAccessor<Double> longitudeAccessor = bridgedPojoModelElement.properties()
					.filter( model -> model.markers( LongitudeMarker.class )
							.anyMatch( m -> Objects.equals( markerSet, m.getMarkerSet() ) ) )
					.collect( singleMarkedProperty( "longitude", defaultedFieldName, markerSet ) )
					.createAccessor( Double.class );

			coordinatesExtractor = bridgedElement -> {
				Double latitude = latitudeAccessor.read( bridgedElement );
				Double longitude = longitudeAccessor.read( bridgedElement );

				if ( latitude == null || longitude == null ) {
					return null;
				}

				return GeoPoint.of( latitude, longitude );
			};
		}
	}

	private static Collector<PojoModelCompositeElement, ?, PojoModelCompositeElement> singleMarkedProperty(
			String markerName, String fieldName, String markerSet) {
		return StreamHelper.singleElement(
				() -> log.propertyMarkerNotFound( markerName, fieldName, markerSet ),
				() -> log.multiplePropertiesForMarker( markerName, fieldName, markerSet )
				);
	}

	@Override
	public void write(DocumentElement target, PojoElement source) {
		GeoPoint coordinates = coordinatesExtractor.apply( source );
		fieldAccessor.write( target, coordinates );
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
