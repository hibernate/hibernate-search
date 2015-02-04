/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.lang.reflect.AnnotatedElement;
import java.util.Date;

import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericEncodingDateBridge;
import org.hibernate.search.bridge.builtin.StringEncodingDateBridge;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Built-in {@link org.hibernate.search.bridge.spi.BridgeProvider} handling date bridging when {@code @DateBridge} is involved.
 *
 * As built-in provider, no Service Loader file is used: the {@code BridgeFactory} does access it
 * after the custom bridge providers found.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
class DateBridgeProvider extends ExtendedBridgeProvider {

	private static final Log LOG = LoggerFactory.make();

	@Override
	public FieldBridge provideFieldBridge(ExtendedBridgeProviderContext context) {
		AnnotatedElement annotatedElement = context.getAnnotatedElement();
		if ( Date.class.isAssignableFrom( context.getReturnType() ) ) {
			if ( annotatedElement.isAnnotationPresent( org.hibernate.search.annotations.DateBridge.class ) ) {
				DateBridge dateBridgeAnnotation = annotatedElement.getAnnotation(
						org.hibernate.search.annotations.DateBridge.class
				);

				Resolution resolution = dateBridgeAnnotation.resolution();
				EncodingType encodingType = dateBridgeAnnotation.encoding();
				return getDateFieldBridge( resolution, encodingType );
			}
			else {
				return getDateFieldBridge( Resolution.MILLISECOND, EncodingType.NUMERIC );
			}
		}
		return null;
	}

	private FieldBridge getDateFieldBridge(Resolution resolution, EncodingType encodingType) {
		switch ( encodingType ) {
			case NUMERIC: {
				switch ( resolution ) {
					case YEAR:
						return NumericEncodingDateBridge.DATE_YEAR;
					case MONTH:
						return NumericEncodingDateBridge.DATE_MONTH;
					case DAY:
						return NumericEncodingDateBridge.DATE_DAY;
					case HOUR:
						return NumericEncodingDateBridge.DATE_HOUR;
					case MINUTE:
						return NumericEncodingDateBridge.DATE_MINUTE;
					case SECOND:
						return NumericEncodingDateBridge.DATE_SECOND;
					case MILLISECOND:
						return NumericEncodingDateBridge.DATE_MILLISECOND;
					default:
						throw LOG.unknownResolution( resolution.toString() );
				}
			}
			case STRING: {
				switch ( resolution ) {
					case YEAR:
						return StringEncodingDateBridge.DATE_YEAR;
					case MONTH:
						return StringEncodingDateBridge.DATE_MONTH;
					case DAY:
						return StringEncodingDateBridge.DATE_DAY;
					case HOUR:
						return StringEncodingDateBridge.DATE_HOUR;
					case MINUTE:
						return StringEncodingDateBridge.DATE_MINUTE;
					case SECOND:
						return StringEncodingDateBridge.DATE_SECOND;
					case MILLISECOND:
						return StringEncodingDateBridge.DATE_MILLISECOND;
					default:
						throw LOG.unknownResolution( resolution.toString() );
				}
			}
			default: {
				throw LOG.unknownEncodingType( encodingType.name() );
			}
		}
	}
}
