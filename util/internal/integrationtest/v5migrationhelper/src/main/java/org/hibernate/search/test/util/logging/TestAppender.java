/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util.logging;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestAppender extends WriterAppender {

	private final MultiValueMap eventsByLogger = new MultiValueMap();

	@Override
	public void append(LoggingEvent event) {
		synchronized (this) {
			eventsByLogger.put( event.getLoggerName(), event );
		}
	}

	public List<LoggingEvent> searchByLoggerAndMessage(String logger, String contents) {
		ArrayList<LoggingEvent> results = new ArrayList<>();
		synchronized (this) {
			Collection collection = eventsByLogger.getCollection( logger );
			if ( collection == null ) {
				return results;
			}
			for ( Object event : collection ) {
				LoggingEvent loggingEvent = (LoggingEvent) event;
				if ( loggingEvent.getRenderedMessage().contains( contents ) ) {
					results.add( loggingEvent );
				}
			}
		}
		return results;
	}
}
