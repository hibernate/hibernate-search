/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.test;

import org.hibernate.search.util.impl.test.annotation.SuppressForbiddenApis;

public final class SystemHelper {

	private SystemHelper() {
	}

	@SuppressForbiddenApis(reason = "This is a safer wrapper around System.setProperty")
	public static SystemPropertyRestorer setSystemProperty(String key, String value) {
		String oldValue = System.getProperty( key );
		System.setProperty( key, value );
		return oldValue == null ? () -> System.clearProperty( key )
				: () -> System.setProperty( key, oldValue );
	}

	public interface SystemPropertyRestorer extends AutoCloseable {
		@Override
		void close();
	}
}
