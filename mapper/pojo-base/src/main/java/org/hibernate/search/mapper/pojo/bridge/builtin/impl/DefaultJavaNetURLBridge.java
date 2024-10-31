/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.search.mapper.pojo.logging.impl.FormattingLog;

public final class DefaultJavaNetURLBridge extends AbstractStringBasedDefaultBridge<URL> {

	public static final DefaultJavaNetURLBridge INSTANCE = new DefaultJavaNetURLBridge();

	private DefaultJavaNetURLBridge() {
	}

	@Override
	protected String toString(URL value) {
		try {
			return value.toURI().toString();
		}
		catch (URISyntaxException e) {
			throw FormattingLog.INSTANCE.badURISyntax( value.toString(), e );
		}
	}

	@Override
	protected URL fromString(String value) {
		try {
			return new URI( value ).toURL();
		}
		catch (MalformedURLException | URISyntaxException e) {
			throw FormattingLog.INSTANCE.malformedURL( value, e );
		}
	}

}
