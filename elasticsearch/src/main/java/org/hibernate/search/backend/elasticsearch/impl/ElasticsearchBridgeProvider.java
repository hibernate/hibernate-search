/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.lang.reflect.AnnotatedElement;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.backend.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.impl.ExtendedBridgeProvider;
import org.hibernate.search.bridge.spi.IndexManagerTypeSpecificBridgeProvider;
import org.hibernate.search.indexes.spi.IndexManagerType;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Creates bridges specific to ES.
 *
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public class ElasticsearchBridgeProvider extends ExtendedBridgeProvider implements IndexManagerTypeSpecificBridgeProvider {
	private static final Log LOG = LoggerFactory.make();

	@Override
	public IndexManagerType getIndexManagerType() {
		return ElasticsearchIndexManagerType.INSTANCE;
	}

	@Override
	public FieldBridge provideFieldBridge(ExtendedBridgeProviderContext context) {
		AnnotatedElement annotatedElement = context.getAnnotatedElement();
		if ( Date.class.isAssignableFrom( context.getReturnType() ) ) {
			if ( annotatedElement.isAnnotationPresent( DateBridge.class ) ) {
				DateBridge dateBridgeAnnotation = annotatedElement.getAnnotation( DateBridge.class );

				return getDateFieldBridge( dateBridgeAnnotation.resolution() );
			}
			else {
				return getDateFieldBridge( Resolution.MILLISECOND );
			}
		}
		else if ( Calendar.class.isAssignableFrom( context.getReturnType() ) ) {
			if ( annotatedElement.isAnnotationPresent( DateBridge.class ) ) {
				DateBridge dateBridgeAnnotation = annotatedElement.getAnnotation( DateBridge.class );

				return getCalendarFieldBridge( dateBridgeAnnotation.resolution() );
			}
			else {
				return getCalendarFieldBridge( Resolution.MILLISECOND );
			}
		}

		return null;
	}

	private FieldBridge getDateFieldBridge(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return ElasticsearchDateBridge.DATE_YEAR;
			case MONTH:
				return ElasticsearchDateBridge.DATE_MONTH;
			case DAY:
				return ElasticsearchDateBridge.DATE_DAY;
			case HOUR:
				return ElasticsearchDateBridge.DATE_HOUR;
			case MINUTE:
				return ElasticsearchDateBridge.DATE_MINUTE;
			case SECOND:
				return ElasticsearchDateBridge.DATE_SECOND;
			case MILLISECOND:
				return ElasticsearchDateBridge.DATE_MILLISECOND;
			default:
				throw LOG.unknownResolution( resolution.toString() );
		}
	}

	private FieldBridge getCalendarFieldBridge(Resolution resolution) {
		switch ( resolution ) {
			case YEAR:
				return ElasticsearchCalendarBridge.DATE_YEAR;
			case MONTH:
				return ElasticsearchCalendarBridge.DATE_MONTH;
			case DAY:
				return ElasticsearchCalendarBridge.DATE_DAY;
			case HOUR:
				return ElasticsearchCalendarBridge.DATE_HOUR;
			case MINUTE:
				return ElasticsearchCalendarBridge.DATE_MINUTE;
			case SECOND:
				return ElasticsearchCalendarBridge.DATE_SECOND;
			case MILLISECOND:
				return ElasticsearchCalendarBridge.DATE_MILLISECOND;
			default:
				throw LOG.unknownResolution( resolution.toString() );
		}
	}
}
