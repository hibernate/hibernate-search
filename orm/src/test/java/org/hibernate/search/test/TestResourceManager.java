/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test;

import java.io.File;

import org.apache.lucene.store.Directory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.spi.SearchIntegrator;

/**
 * Interface defining ORM and Search infrastructure methods a test base class needs to offer.
 *
 * @author Hardy Ferentschik
 */
public interface TestResourceManager {

	Configuration getCfg();

	void openSessionFactory();

	void closeSessionFactory();

	SessionFactory getSessionFactory();

	Session openSession();

	Session getSession();

	SearchFactory getSearchFactory();

	SearchIntegrator getExtendedSearchIntegrator();

	Directory getDirectory(Class<?> clazz);

	void ensureIndexesAreEmpty();

	File getBaseIndexDir();

	void forceConfigurationRebuild();

	boolean needsConfigurationRebuild();
}
