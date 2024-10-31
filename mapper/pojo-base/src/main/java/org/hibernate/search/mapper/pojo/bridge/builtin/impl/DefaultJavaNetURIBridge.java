/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.net.URI;
import java.net.URISyntaxException;

import org.hibernate.search.mapper.pojo.logging.impl.FormattingLog;

public final class DefaultJavaNetURIBridge extends AbstractStringBasedDefaultBridge<URI> {

	public static final DefaultJavaNetURIBridge INSTANCE = new DefaultJavaNetURIBridge();

	private DefaultJavaNetURIBridge() {
	}

	@Override
	protected String toString(URI value) {
		return value.toString();
	}

	@Override
	protected URI fromString(String value) {
		try {
			return new URI( value );
		}
		catch (URISyntaxException e) {
			throw FormattingLog.INSTANCE.badURISyntax( value, e );
		}
	}

}
