/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.fileleaks;

import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

/**
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class AllFilesClosedNRTTest extends AllFilesClosedTest {

	@Override
	protected void overrideProperties(SearchConfigurationForTest cfg) {
		cfg.addProperty( "hibernate.search.default.indexmanager", "near-real-time" );
	}

	@Override
	protected boolean nrtNotEnabled() {
		return false;
	}

}
