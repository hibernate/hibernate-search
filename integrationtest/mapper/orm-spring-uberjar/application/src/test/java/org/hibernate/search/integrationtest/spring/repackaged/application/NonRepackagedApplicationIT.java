/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
