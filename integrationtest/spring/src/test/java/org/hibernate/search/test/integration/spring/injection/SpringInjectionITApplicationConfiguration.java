/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.injection;

import javax.inject.Inject;

import org.hibernate.search.test.integration.spring.injection.integration.SpringBeanResolverContributor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * @author Yoann Rodiere
 */
@Configuration
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = JtaAutoConfiguration.class)
@ComponentScan
@EntityScan
public class SpringInjectionITApplicationConfiguration {

	@Bean
	public BeanPostProcessor addJpaBeanFactoryProperty() {
		return new BeanPostProcessor() {
			@Inject
			private ApplicationContext applicationContext;

			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				if ( bean instanceof LocalContainerEntityManagerFactoryBean ) {
					LocalContainerEntityManagerFactoryBean factoryBean = (LocalContainerEntityManagerFactoryBean) bean;
					factoryBean.getJpaPropertyMap()
							.put( SpringBeanResolverContributor.BEAN_FACTORY, applicationContext );
				}
				return bean;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				return bean;
			}
		};
	}

}
