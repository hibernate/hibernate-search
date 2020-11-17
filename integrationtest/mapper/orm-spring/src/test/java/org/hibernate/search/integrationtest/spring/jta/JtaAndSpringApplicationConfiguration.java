/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
