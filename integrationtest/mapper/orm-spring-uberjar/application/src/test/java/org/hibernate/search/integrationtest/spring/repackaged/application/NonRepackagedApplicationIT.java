/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.repackaged.application;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * This test runs in the failsafe classloader which gets classes
 * directly from the project build directory,
 * so it doesn't use the repackaged JAR.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class NonRepackagedApplicationIT {

	@Autowired
	private SmokeTestingBean smokeTestingBean;

	@Test
	public void smokeTest() {
		smokeTestingBean.smokeTest();
	}

}
