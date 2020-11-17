/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.testsupport;

import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(exclude = JtaAutoConfiguration.class)
public abstract class AbstractSpringITConfig {

	@Bean
	public BackendMock backendMock() {
		return new BackendMock().ignoreSchema();
	}

	@Bean
	public HibernatePropertiesCustomizer backendMockPropertiesCustomizer(@Autowired BackendMock backendMock) {
		return hibernateProperties ->
				hibernateProperties.put( "hibernate.search.backend.type", backendMock.factory() );
	}
}
