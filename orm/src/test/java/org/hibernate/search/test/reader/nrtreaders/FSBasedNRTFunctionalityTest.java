/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.reader.nrtreaders;

import org.hibernate.cfg.Configuration;

/**
 * Similar to parent class, but make sure the same functionality works fine
 * when using a FSDirectory [HSEARCH-1095].
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class FSBasedNRTFunctionalityTest extends BasicNRTFunctionalityTest {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
	}

}
