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
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.GeoPointBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;
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

	public static class Builder implements GeoPointBridgeBuilder<GeoPointBridge>,
			AnnotationBridgeBuilder<GeoPointBridge, org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.GeoPointBridge> {

		private String fieldName;
		private Projectable projectable = Projectable.DEFAULT;
		private Sortable sortable = Sortable.DEFAULT;
		private String markerSet;

		@Override
		public void initialize(
				org.hibernate.search.mapper.pojo.bridge.builtin.spatial.annotation.GeoPointBridge annotation) {
			fieldName( annotation.fieldName() );
			markerSet( annotation.markerSet() );
			projectable( annotation.projectable() );
			sortable( annotation.sortable() );
		}

		@Override
		public Builder fieldName(String fieldName) {
			this.fieldName = fieldName;
			return this;
		}

		@Override
		public Builder projectable(Projectable projectable) {
			this.projectable = projectable;
			return this;
		}

		@Override
		public Builder sortable(Sortable sortable) {
			this.sortable = sortable;
			return this;
		}

		@Override
		public Builder markerSet(String markerSet) {
			this.markerSet = markerSet;
			return this;
		}

		@Override
		public BeanHolder<? extends GeoPointBridge> build(BridgeBuildContext buildContext) {
			return BeanHolder.of( new GeoPointBridge( fieldName, projectable, sortable, markerSet ) );
		}
	}

	private final String fieldName;
	private final Projectable projectable;
	private final Sortable sortable;
	private final String markerSet;

	private IndexFieldReference<GeoPoint> indexFieldReference;
	private Function<Object, GeoPoint> coordinatesExtractor;

	/**
	 * Private constructor, use {@link GeoPointBridgeBuilder#forType()} or {@link GeoPointBridgeBuilder#forProperty()} instead.
	 */
	private GeoPointBridge(String fieldName, Projectable projectable, Sortable sortable, String markerSet) {
		this.fieldName = fieldName;
		this.projectable = projectable;
		this.sortable = sortable;
		this.markerSet = markerSet;
	}

	@Override
	public void bind(TypeBridgeBindingContext context) {
		if ( fieldName == null || fieldName.isEmpty() ) {
			throw log.missingFieldNameForGeoPointBridgeOnType( context.getBridgedElement().toString() );
		}

		bind( fieldName, context.getTypeFactory(), context.getIndexSchemaElement(), context.getBridgedElement() );
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

		bind( defaultedFieldName, context.getTypeFactory(), context.getIndexSchemaElement(), context.getBridgedElement() );
	}

	private void bind(String defaultedFieldName, IndexFieldTypeFactoryContext typeFactoryContext,
			IndexSchemaElement indexSchemaElement,
			PojoModelCompositeElement bridgedPojoModelElement) {
		indexFieldReference = indexSchemaElement.field(
				defaultedFieldName,
				typeFactoryContext.asGeoPoint().projectable( projectable ).sortable( sortable ).toIndexFieldType()
		)
				.toReference();

		if ( bridgedPojoModelElement.isAssignableTo( GeoPoint.class ) ) {
			PojoElementAccessor<GeoPoint> sourceAccessor = bridgedPojoModelElement.createAccessor( GeoPoint.class );
			coordinatesExtractor = sourceAccessor::read;
		}
		else {
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
}
