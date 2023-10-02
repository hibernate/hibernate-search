/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.indexedembedded.includeembeddedobjectid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class IndexedEmbeddedIncludeEmbeddedObjectIdIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Employee.class, Department.class );
	}

	@Test
	void smoke() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			Department department1 = new Department();
			department1.setId( 1 );
			department1.setName( "Shipping" );
			Department department2 = new Department();
			department2.setId( 2 );
			department2.setName( "Accounting" );

			Employee employee1 = new Employee();
			employee1.setId( 1 );
			employee1.setName( "Jane Doe" );
			employee1.setDepartment( department1 );
			department1.getEmployees().add( employee1 );
			Employee employee2 = new Employee();
			employee2.setId( 2 );
			employee2.setName( "John Smith" );
			employee2.setDepartment( department2 );
			department2.getEmployees().add( employee2 );

			entityManager.persist( department1 );
			entityManager.persist( department2 );
			entityManager.persist( employee1 );
			entityManager.persist( employee2 );
		} );

		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Employee> result = searchSession.search( Employee.class )
					.where( f -> f.and(
							f.match().field( "name" ).matching( "Jane" ),
							f.match().field( "department.id" ).matching( 1 ),
							f.match().field( "department.name" ).matching( "Shipping" )
					) )
					.fetchHits( 20 );
			assertThat( result ).hasSize( 1 );
		} );
	}

}
