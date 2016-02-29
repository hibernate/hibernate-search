/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.async;

import java.util.Map;

import org.hibernate.search.backend.triggers.impl.TriggerServiceConstants;
import org.hibernate.search.db.events.jpa.impl.AsyncUpdateConstants;
import org.hibernate.search.db.events.triggers.HSQLDBTriggerSQLStringSource;

/**
 * Created by Martin on 14.11.2015.
 */
public class AutomaticAsyncIndexUpdateTest extends BaseAsyncIndexUpdateTest {
	@Override
	protected void setup() {

	}

	@Override
	protected void shutdown() {

	}

	@Override
	public void configure(Map<String, Object> cfg) {
		super.configure( cfg );
		cfg.put( TriggerServiceConstants.TRIGGER_BASED_BACKEND_KEY, "true" );
		cfg.put( AsyncUpdateConstants.TRIGGER_SOURCE_KEY, HSQLDBTriggerSQLStringSource.class.getName() );
	}
}
