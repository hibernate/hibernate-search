// $Id$
package org.hibernate.search.test.directoryProvider;

import java.io.File;
import java.util.Properties;

import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.hibernate.search.store.LockFactoryFactory;

public class CustomLockFactoryFactory implements LockFactoryFactory {

	// A real implementation would probably not use a static field; useful to keep the test simple.
	static String optionValue;

	public LockFactory createLockFactory(File indexDir, Properties dirConfiguration) {
		optionValue = dirConfiguration.getProperty( "locking_option" );
		return new SingleInstanceLockFactory();
	}

}
