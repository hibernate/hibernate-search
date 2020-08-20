/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.orm.loading.impl.HibernateStatelessInitializer;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

/**
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class HibernateManualConfiguration extends SearchConfigurationForTest implements SearchConfiguration {

	public HibernateManualConfiguration() {
		super( HibernateStatelessInitializer.INSTANCE, true );
	}

}
