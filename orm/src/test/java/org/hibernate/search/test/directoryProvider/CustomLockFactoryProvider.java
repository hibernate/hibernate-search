/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
