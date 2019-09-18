/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.spatial.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelCompositeElement;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.impl.StreamHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public class GeoPointBridge implements TypeBridge, PropertyBridge {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Function<Object, GeoPoint> coordinatesExtractor;
	private final IndexFieldReference<GeoPoint> indexFieldReference;

	/**
	 * Private constructor, use {@link GeoPointBinder#create()} instead.
	 */
	private GeoPointBridge(Function<Object, GeoPoint> coordinatesExtractor,
			IndexFieldReference<GeoPoint> indexFieldReference) {
		this.coordinatesExtractor = coordinatesExtractor;
		this.indexFieldReference = indexFieldReference;
	}

	@Override
	public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
		doWrite( target, bridgedElement );
	}

	@Override
	public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
		doWrite( target, bridgedElement );
	}

	private void doWrite(DocumentElement target, Object bridgedElement) {
		GeoPoint coordinates = coordinatesExtractor.apply( bridgedElement );
		target.addValue( indexFieldReference, coordinates );
	}

	@Override
	public void close() {
		// Nothing to do
	}

	public static class Binder implements GeoPointBinder {

		private String fieldName;
		private Projectable projectable = Projectable.DEFAULT;
		private Sortable sortable = Sortable.DEFAULT;
		private String markerSet;

		@Override
		public void initialize(
				GeoPointBinding annotation) {
			fieldName( annotation.fieldName() );
			markerSet( annotation.markerSet() );
			projectable( annotation.projectable() );
			sortable( annotation.sortable() );
		}

		@Override
		public Binder fieldName(String fieldName) {
			this.fieldName = fieldName;
			return this;
		}

		@Override
		public Binder projectable(Projectable projectable) {
			this.projectable = projectable;
			return this;
		}

		@Override
		public Binder sortable(Sortable sortable) {
			this.sortable = sortable;
			return this;
		}

		@Override
		public Binder markerSet(String markerSet) {
			this.markerSet = markerSet;
			return this;
		}

		@Override
		public void bind(TypeBindingContext context) {
			if ( fieldName == null || fieldName.isEmpty() ) {
				throw log.missingFieldNameForGeoPointBridgeOnType( context.getBridgedElement().toString() );
			}

			GeoPointBridge bridge = doBind(
					fieldName,
					context.getTypeFactory(),
					context.getIndexSchemaElement(),
					context.getBridgedElement()
			);
			context.setBridge( bridge );
		}

		@Override
		public void bind(PropertyBindingContext context) {
			String defaultedFieldName;
			if ( fieldName != null && !fieldName.isEmpty() ) {
				defaultedFieldName = fieldName;
			}
			else {
				defaultedFieldName = context.getBridgedElement().getName();
			}

			GeoPointBridge bridge = doBind(
					defaultedFieldName,
					context.getTypeFactory(),
					context.getIndexSchemaElement(),
					context.getBridgedElement()
			);
			context.setBridge( bridge );
		}

		private GeoPointBridge doBind(String defaultedFieldName, IndexFieldTypeFactory typeFactory,
				IndexSchemaElement indexSchemaElement,
				PojoModelCompositeElement bridgedPojoModelElement) {
			IndexFieldReference<GeoPoint> indexFieldReference = indexSchemaElement.field(
					defaultedFieldName,
					typeFactory.asGeoPoint().projectable( projectable ).sortable( sortable ).toIndexFieldType()
			)
					.toReference();

			Function<Object, GeoPoint> coordinatesExtractor;
			PojoElementAccessor<Double> latitudeAccessor = bridgedPojoModelElement.properties()
					.filter( model -> model.markers( LatitudeMarker.class )
							.anyMatch( m -> Objects.equals( markerSet, m.getMarkerSet() ) ) )
					.collect( singleMarkedProperty( "latitude", defaultedFieldName, markerSet ) )
					.createAccessor( Double.class );
			PojoElementAccessor<Double> longitudeAccessor = bridgedPojoModelElement.properties()
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

			return new GeoPointBridge(
					coordinatesExtractor,
					indexFieldReference
			);
		}

		private static Collector<PojoModelCompositeElement, ?, PojoModelCompositeElement> singleMarkedProperty(
				String markerName, String fieldName, String markerSet) {
			return StreamHelper.singleElement(
					() -> log.propertyMarkerNotFound( markerName, fieldName, markerSet ),
					() -> log.multiplePropertiesForMarker( markerName, fieldName, markerSet )
			);
		}
	}
}
