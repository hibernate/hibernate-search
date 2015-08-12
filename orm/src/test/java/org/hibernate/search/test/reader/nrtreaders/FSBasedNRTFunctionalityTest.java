/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.reader.nrtreaders;

import java.util.Map;

/**
 * Similar to parent class, but make sure the same functionality works fine
 * when using a FSDirectory [HSEARCH-1095].
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class FSBasedNRTFunctionalityTest extends BasicNRTFunctionalityTest {

	@Override
	public void configure(Map<String,Object> cfg) {
		super.configure( cfg );
		cfg.put( "hibernate.search.default.directory_provider", "filesystem" );
	}

}
