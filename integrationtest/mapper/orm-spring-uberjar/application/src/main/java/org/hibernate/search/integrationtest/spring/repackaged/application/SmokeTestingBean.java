/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.spring.repackaged.application;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import acme.org.hibernate.search.integrationtest.spring.repackaged.model.MyEntity;
import acme.org.hibernate.search.integrationtest.spring.repackaged.model.MyProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SmokeTestingBean {
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	public void smokeTest() {
		TransactionTemplate transactionTemplate = new TransactionTemplate( transactionManager );

		transactionTemplate.execute( status -> {
			MyEntity entity = new MyEntity();
			entity.id = 1L;
			entity.name = "name";
			entityManager.persist( entity );
			return null;
		} );

		transactionTemplate.execute( status -> {
			SearchSession session = Search.session( entityManager );
			List<MyProjection> myProjections = session.search( MyEntity.class )
					.select( MyProjection.class )
					.where( f -> f.match().field( "name" ).matching( "name" ) )
					.fetchAllHits();
			if ( myProjections.isEmpty() ) {
				throw new IllegalStateException( "Incorrect count of projections." );
			}
			return null;
		} );

		System.out.println( "Hibernate Search read the nested JAR." );
	}
}
