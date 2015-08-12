/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.leakdetection;

import org.apache.lucene.store.LockFactory;
import org.hibernate.search.store.impl.RAMDirectoryProvider;


/**
 * This DirectoryProvider enables us to check that all files have been properly closed,
 * both after writes and reads.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class FileMonitoringDirectoryProvider extends RAMDirectoryProvider {

	@Override
	protected FileMonitoringDirectory makeRAMDirectory(LockFactory lockFactory) {
		return new FileMonitoringDirectory();
	}

}
