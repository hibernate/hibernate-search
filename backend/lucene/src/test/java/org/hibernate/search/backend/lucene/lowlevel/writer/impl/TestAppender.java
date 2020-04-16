/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;

public class TestAppender extends WriterAppender {

	private final Map<String, List<LoggingEvent>> eventsByLogger = new LinkedHashMap<>();

	@Override
	public void append(LoggingEvent event) {
		synchronized (this) {
			eventsByLogger.computeIfAbsent( event.getLoggerName(), ignored -> new ArrayList<>() )
					.add( event );
		}
	}

	public List<LoggingEvent> searchByLoggerAndMessage(String logger, String contents) {
		ArrayList<LoggingEvent> results = new ArrayList<>();
		synchronized (this) {
			Collection<LoggingEvent> collection = eventsByLogger.get( logger );
			if ( collection == null ) {
				return results;
			}
			for ( LoggingEvent loggingEvent : collection ) {
				if ( loggingEvent.getRenderedMessage().contains( contents ) ) {
					results.add( loggingEvent );
				}
			}
		}
		return results;
	}
}
