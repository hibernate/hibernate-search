/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.async;

import java.util.Properties;

import org.hibernate.search.backend.triggers.impl.TriggerAsyncBackendService;
import org.hibernate.search.backend.triggers.impl.TriggerAsyncBackendServiceImpl;
import org.hibernate.search.backend.triggers.impl.TriggerServiceConstants;
import org.hibernate.search.engine.service.classloading.impl.DefaultClassLoaderService;
import org.hibernate.search.genericjpa.db.events.jpa.impl.AsyncUpdateConstants;
import org.hibernate.search.genericjpa.db.events.triggers.HSQLDBTriggerSQLStringSource;

/**
 * Created by Martin on 14.11.2015.
 */
public class ManualAsyncIndexUpdateTest extends BaseAsyncIndexUpdateTest {

	private TriggerAsyncBackendService triggerAsyncBackendService;

	@Override
	protected void setup() {
		this.triggerAsyncBackendService = new TriggerAsyncBackendServiceImpl();
		Properties properties = new Properties();
		properties.setProperty( AsyncUpdateConstants.TRIGGER_SOURCE_KEY, HSQLDBTriggerSQLStringSource.class.getName() );
		properties.setProperty( TriggerServiceConstants.TRIGGER_BASED_BACKEND_KEY, "true" );
		this.triggerAsyncBackendService.start(
				this.getSessionFactory(),
				this.getExtendedSearchIntegrator(),
				new DefaultClassLoaderService(),
				properties
		);
	}

	@Override
	protected void shutdown() {
		this.triggerAsyncBackendService.stop();
	}

}
