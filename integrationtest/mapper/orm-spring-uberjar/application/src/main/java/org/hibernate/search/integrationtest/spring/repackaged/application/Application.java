/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.spring.repackaged.application;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

// CHECKSTYLE:OFF: HideUtilityClassConstructor
@SpringBootApplication
@EntityScan({ "acme.org.hibernate.search.integrationtest.spring.repackaged.model" })
public class Application {
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx =
				new SpringApplicationBuilder( Application.class ).web( WebApplicationType.NONE ).run();
		System.out.println( "Spring Boot application started" );
		ctx.getBean( SmokeTestingBean.class ).smokeTest();
		ctx.close();
	}
}
// CHECKSTYLE:ON
