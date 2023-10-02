/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.jta.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.integrationtest.spring.jta.JtaAndSpringOutboxApplicationConfiguration;
import org.hibernate.search.integrationtest.spring.jta.dao.SnertDAO;
import org.hibernate.search.integrationtest.spring.jta.entity.Snert;
import org.hibernate.search.integrationtest.spring.testsupport.AbstractMapperOrmSpringIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.atomikos.icatch.jta.TransactionManagerImp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = JtaAndSpringOutboxApplicationConfiguration.class)
@ActiveProfiles({ "jta", "outbox", "transaction-timeout" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TransactionTimeoutJtaAndSpringOutboxIT extends AbstractMapperOrmSpringIT {

	@Autowired
	private SnertDAO snertDAO;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void checkJta() {
		TimeoutFailureCollector.EXCEPTIONS.clear();

		assertThat( entityManagerFactory.unwrap( SessionFactoryImplementor.class )
				.getServiceRegistry().getService( TransactionCoordinatorBuilder.class ) )
				.returns( true, TransactionCoordinatorBuilder::isJta );

		// We changed the default Atomikos timeout to 1s (1000ms)
		assertThat( TransactionManagerImp.getDefaultTimeout() ).isOne();

		// The test is supposed to time out
	}

	@Test
	void test() {
		Snert snert = new Snert();
		snert.setId( 1L );
		snert.setName( "dave" );
		snert.setNickname( "dude" );
		snert.setAge( 99 );
		snert.setCool( Boolean.TRUE );
		snertDAO.persist( snert );

		await( "Waiting for indexing assertions" )
				.untilAsserted( () -> assertThat( TimeoutFailureCollector.EXCEPTIONS ).isNotEmpty() );

		Throwable exception = TimeoutFailureCollector.EXCEPTIONS.iterator().next();
		assertThat( exception ).hasStackTraceContaining( "timeout" );
	}
}
