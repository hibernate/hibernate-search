package org.hibernate.search.test.async;

import java.util.Properties;

import org.hibernate.search.backend.triggers.impl.TriggerAsyncBackendService;
import org.hibernate.search.backend.triggers.impl.TriggerAsyncBackendServiceImpl;
import org.hibernate.search.backend.triggers.impl.TriggerServiceConstants;
import org.hibernate.search.engine.service.classloading.impl.DefaultClassLoaderService;

/**
 * Created by Martin on 09.03.2016.
 *
 * this starts the service by hand
 */
public class ManualAsyncIndexUpdateStartTest extends BaseAsyncIndexUpdateTest {

	public ManualAsyncIndexUpdateStartTest() {
		super( true );
	}

	protected ManualAsyncIndexUpdateStartTest(boolean isProfileTest) {
		super( isProfileTest );
	}

	private TriggerAsyncBackendService triggerAsyncBackendService;

	@Override
	protected void setup() {
		this.triggerAsyncBackendService = new TriggerAsyncBackendServiceImpl();
		Properties properties = new Properties();
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
