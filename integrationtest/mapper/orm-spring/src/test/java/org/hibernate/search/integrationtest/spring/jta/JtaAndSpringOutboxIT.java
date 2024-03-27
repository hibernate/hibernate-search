/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.jta;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.search.integrationtest.spring.jta.dao.SnertDAO;
import org.hibernate.search.integrationtest.spring.jta.entity.Snert;
import org.hibernate.search.integrationtest.spring.testsupport.AbstractMapperOrmSpringIT;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSearchSessionHolder;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = JtaAndSpringOutboxApplicationConfiguration.class)
@ActiveProfiles({ "jta", "outbox" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JtaAndSpringOutboxIT extends AbstractMapperOrmSpringIT {

	@Autowired
	@RegisterExtension
	public BackendMock backendMock;

	@Autowired
	private SnertDAO snertDAO;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void checkJta() {
		assertThat( entityManagerFactory.unwrap( SessionFactoryImplementor.class )
				.getServiceRegistry().getService( TransactionCoordinatorBuilder.class ) )
				.returns( true, TransactionCoordinatorBuilder::isJta );
	}

	@AfterEach
	void checkNoMemoryLeak() {
		assertThat( HibernateOrmSearchSessionHolder.staticMapSize() ).isZero();
	}

	@Test
	void test() {
		Snert snert = new Snert();
		snert.setId( 1L );
		snert.setName( "dave" );
		snert.setNickname( "dude" );
		snert.setAge( 99 );
		snert.setCool( Boolean.TRUE );

		backendMock.expectWorks( "Snert" )
				.add( "1", b -> b
						.field( "age", 99 )
						.field( "cool", true )
						.field( "name", "dave" ) );
		snertDAO.persist( snert );
		backendMock.verifyExpectationsMet();
	}
}
