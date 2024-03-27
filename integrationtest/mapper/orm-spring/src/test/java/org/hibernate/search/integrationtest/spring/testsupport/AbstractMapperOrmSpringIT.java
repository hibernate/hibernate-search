/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.testsupport;

import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
public abstract class AbstractMapperOrmSpringIT {
	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		DatabaseContainer.configuration().addAsSpring( (key, value) -> registry.add( key, () -> value ) );
	}
}
