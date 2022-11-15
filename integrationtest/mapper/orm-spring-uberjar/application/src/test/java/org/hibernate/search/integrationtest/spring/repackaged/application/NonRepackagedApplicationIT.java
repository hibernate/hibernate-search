/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.repackaged.application;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * This test runs in the failsafe classloader which gets classes
 * directly from the project build directory,
 * so it doesn't use the repackaged JAR.
 */
@SpringBootTest(classes = Application.class)
class NonRepackagedApplicationIT {

	@Autowired
	private SmokeTestingBean smokeTestingBean;

	@Test
	void smokeTest() {
		smokeTestingBean.smokeTest();
	}

}
