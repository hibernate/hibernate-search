/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

public class TestAppender extends AbstractAppender {

	private final Map<String, List<String>> eventMessagesByLogger = new LinkedHashMap<>();

	public TestAppender(String name) {
		super( name, null, null, true, Property.EMPTY_ARRAY );
	}

	@Override
	public void append(LogEvent event) {
		synchronized (this) {
			eventMessagesByLogger.computeIfAbsent( event.getLoggerName(), ignored -> new ArrayList<>() )
					.add( event.getMessage().getFormattedMessage() );
		}
	}

	public List<String> searchByLoggerAndMessage(String logger, String contents) {
		ArrayList<String> results = new ArrayList<>();
		synchronized (this) {
			Collection<String> collection = eventMessagesByLogger.get( logger );
			if ( collection == null ) {
				return results;
			}
			for ( String eventMessage : collection ) {
				if ( eventMessage.contains( contents ) ) {
					results.add( eventMessage );
				}
			}
		}
		return results;
	}
}
