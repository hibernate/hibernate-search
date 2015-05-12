/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.engine;

import java.util.Map;

import org.hibernate.search.cfg.Environment;

/**
 * Verify functionality of Environment.ENABLE_DIRTY_CHECK
 *
 * @author Sanne Grinovero
 * @since 3.4
 */
public class DirtyChecksDisabledTest extends SkipIndexingWorkForUnaffectingChangesTest {

	@Override
	protected boolean isDirtyCheckEnabled() {
		return false;
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		super.configure( cfg );
		cfg.put( Environment.ENABLE_DIRTY_CHECK, " false" ); //intentional space in the value
	}

}
