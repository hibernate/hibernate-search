/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.testsupport;

import java.util.concurrent.CompletableFuture;

import org.hibernate.SessionFactory;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.HibernateOrmMappingHandle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@EnableAutoConfiguration(exclude = JtaAutoConfiguration.class)
public abstract class AbstractSpringITConfig {
	private final CompletableFuture<BackendMappingHandle> mappingHandlePromise = new CompletableFuture<>();

	@Bean
	public BackendMock backendMock() {
		return BackendMock.create().ignoreSchema();
	}

	@Bean
	public HibernatePropertiesCustomizer backendMockPropertiesCustomizer(@Autowired BackendMock backendMock) {
		return hibernateProperties -> hibernateProperties.put( "hibernate.search.backend.type",
				backendMock.factory( mappingHandlePromise ) );
	}

	@EventListener(ApplicationReadyEvent.class)
	public void initBackendMappingHandle(ApplicationReadyEvent event) {
		mappingHandlePromise
				.complete( new HibernateOrmMappingHandle( event.getApplicationContext().getBean( SessionFactory.class ) ) );
	}
}
