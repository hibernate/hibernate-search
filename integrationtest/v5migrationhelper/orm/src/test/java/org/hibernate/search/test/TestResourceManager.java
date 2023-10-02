/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test;

import java.io.IOException;
import java.nio.file.Path;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.SearchFactory;

/**
 * Interface defining ORM and Search infrastructure methods a test base class needs to offer.
 *
 * @author Hardy Ferentschik
 */
public interface TestResourceManager {

	void openSessionFactory();

	void closeSessionFactory();

	SessionFactory getSessionFactory();

	Session openSession();

	Session getSession();

	SearchFactory getSearchFactory();

	void ensureIndexesAreEmpty() throws IOException;

	Path getBaseIndexDir();

}
