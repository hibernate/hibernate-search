/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.engine.cfg.spi.ParseUtils;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;

public final class DefaultOffsetTimeIdentifierBridge implements IdentifierBridge<OffsetTime> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_TIME;

	@Override
	public String toDocumentIdentifier(OffsetTime propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
		return FORMATTER.format( propertyValue );
	}

	@Override
	public OffsetTime fromDocumentIdentifier(String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context) {
		return ParseUtils.parseOffsetTime( documentIdentifier );
	}

	@Override
	public boolean isCompatibleWith(IdentifierBridge<?> other) {
		return getClass().equals( other.getClass() );
	}
}
