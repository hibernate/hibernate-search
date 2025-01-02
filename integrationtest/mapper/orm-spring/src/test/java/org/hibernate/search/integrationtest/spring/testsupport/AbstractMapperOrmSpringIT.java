/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.testsupport;

import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;

import org.junit.jupiter.api.BeforeEach;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
public abstract class AbstractMapperOrmSpringIT {
	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		DatabaseContainer.configuration().addAsSpring( (key, value) -> registry.add( key, () -> value ) );
	}

	@BeforeEach
	void setUp() {
		// Because the default timeout is set as a static property it won't get reinitialized between tests on its own,
		// that is why we set it ourselves here:
		TxControl.setDefaultTimeout( BeanPopulator.getDefaultInstance( CoordinatorEnvironmentBean.class ).getDefaultTimeout() );
	}
}
