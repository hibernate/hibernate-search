package org.hibernate.search.test.async;

import java.util.Map;

import org.hibernate.search.backend.triggers.impl.TriggerServiceConstants;

/**
 * Created by Martin on 09.03.2016.
 */
public class AutomaticAsyncIndexUpdateTest extends BaseAsyncIndexUpdateTest {

	public AutomaticAsyncIndexUpdateTest() {
		super( true );
	}

	protected AutomaticAsyncIndexUpdateTest(boolean isProfileTest) {
		super( isProfileTest );
	}

	@Override
	protected void setup() {

	}

	@Override
	protected void shutdown() {

	}

	@Override
	public void configure(Map<String, Object> cfg) {
		super.configure( cfg );
	}

}
