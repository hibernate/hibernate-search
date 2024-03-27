/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.jta;

import org.hibernate.search.integrationtest.spring.testsupport.AbstractSpringITConfig;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Yoann Rodiere
 */
@Configuration
@EnableAutoConfiguration
@Import(JtaAutoConfiguration.class)
@ComponentScan
@EntityScan
public class JtaAndSpringApplicationConfiguration extends AbstractSpringITConfig {

}
