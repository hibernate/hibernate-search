/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.lang.reflect.AnnotatedElement;

import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.annotations.SpatialMode;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.spatial.SpatialFieldBridgeByHash;
import org.hibernate.search.spatial.SpatialFieldBridgeByRange;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Built-in {@link org.hibernate.search.bridge.spi.BridgeProvider} handling spatial index bridging
 * when {@code @Spatial} is involved.
 * As built-in provider, no Service Loader file is used: the {@code BridgeFactory} does access it
 * after the custom bridge providers found.
 *
 * @author Emmanuel Bernard
 */
class SpatialBridgeProvider extends ExtendedBridgeProvider {

	private static final Log LOG = LoggerFactory.make();

	@Override
	public FieldBridge provideFieldBridge(ExtendedBridgeProviderContext context) {
		FieldBridge bridge = null;
		AnnotatedElement annotatedElement = context.getAnnotatedElement();
		if ( annotatedElement.isAnnotationPresent( Spatial.class ) ) {
			Spatial spatialAnn = annotatedElement.getAnnotation( Spatial.class );
			try {
				bridge = buildSpatialBridge( spatialAnn, null, null );
			}
			catch (Exception e) {
				throw LOG.unableToInstantiateSpatial( context.getMemberName(), e );
			}
			if ( bridge == null ) {
				throw LOG.unableToInstantiateSpatial( context.getMemberName(), null );
			}
		}
		return bridge;
	}

	/**
	 * This instantiates the SpatialFieldBridge from a {@code Spatial} annotation.
	 *
	 * @param spatial the {@code Spatial} annotation
	 * @return Returns the {@code SpatialFieldBridge} instance
	 * @param latitudeField a {@link java.lang.String} object.
	 * @param longitudeField a {@link java.lang.String} object.
	 */
	public static FieldBridge buildSpatialBridge(Spatial spatial, String latitudeField, String longitudeField) {
		FieldBridge bridge = null;
		if ( spatial != null ) {
			if ( spatial.spatialMode() == SpatialMode.HASH ) {
				if ( latitudeField != null && longitudeField != null ) {
					bridge = new SpatialFieldBridgeByHash( spatial.topSpatialHashLevel(), spatial.bottomSpatialHashLevel(), latitudeField, longitudeField );
				}
				else {
					bridge = new SpatialFieldBridgeByHash( spatial.topSpatialHashLevel(), spatial.bottomSpatialHashLevel() );
				}
			}
			else {
				if ( latitudeField != null && longitudeField != null ) {
					bridge = new SpatialFieldBridgeByRange( latitudeField, longitudeField );
				}
				else {
					bridge = new SpatialFieldBridgeByRange();
				}
			}
		}

		return bridge;
	}
}
