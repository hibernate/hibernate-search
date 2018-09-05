/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.directoryProvider;

import java.io.File;
import java.util.Properties;

import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.hibernate.search.store.LockFactoryProvider;

public class CustomLockFactoryProvider implements LockFactoryProvider {

	// A real implementation would probably not use a static field; useful to keep the test simple.
	static String optionValue;

	@Override
	public LockFactory createLockFactory(File indexDir, Properties dirConfiguration) {
		optionValue = dirConfiguration.getProperty( "locking_option" );
		return new SingleInstanceLockFactory();
	}

}
