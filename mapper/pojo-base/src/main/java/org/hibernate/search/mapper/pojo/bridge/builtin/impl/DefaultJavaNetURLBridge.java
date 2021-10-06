/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.impl;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class DefaultJavaNetURLBridge extends AbstractStringBasedDefaultBridge<URL> {

	public static final DefaultJavaNetURLBridge INSTANCE = new DefaultJavaNetURLBridge();

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private DefaultJavaNetURLBridge() {
	}

	@Override
	protected String toString(URL value) {
		return value.toExternalForm();
	}

	@Override
	protected URL fromString(String value) {
		try {
			return new URL( value );
		}
		catch (MalformedURLException e) {
			throw log.malformedURL( value, e );
		}
	}

}