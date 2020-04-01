/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.rule.StaticCounters;

import org.junit.Rule;
import org.junit.Test;

public class TransientPropertyIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Rule
	public StaticCounters counters = new StaticCounters();

	@Test
	public void withoutDerivedFrom() {
		Assertions.assertThatThrownBy(
				() -> ormSetupHelper.start().setup( EntityWithoutDerivedFrom.class )
		)
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.typeContext( EntityWithoutDerivedFrom.class.getName() )
						.failure(
								"Path '.APlusB' cannot be resolved to a persisted value in Hibernate ORM metadata.",
								"If this path points to a transient value, use @IndexingDependency(derivedFrom = ...)"
										+ " to specify which persisted values it is derived from.",
								"See the reference documentation for more information"
						)
						.build()
				);
	}

	@Test
	public void withDerivedFrom() {
		backendMock.expectSchema( EntityWithDerivedFrom.INDEX, b -> b
				.field( "APlusB", Integer.class )
		);

		SessionFactory sessionFactory = ormSetupHelper.start().setup( EntityWithDerivedFrom.class );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			EntityWithDerivedFrom entity1 = new EntityWithDerivedFrom();
			entity1.setId( 1 );
			entity1.setA( 2 );
			entity1.setB( 5 );
			entity1.setC( 11 );

			session.persist( entity1 );

			backendMock.expectWorks( EntityWithDerivedFrom.INDEX )
					.add( "1", b -> b
							.field( "APlusB", 7 )
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// A is used to derive the transient property, so it should trigger reindexing.
		// More related tests in the AutomaticIndexing*IT tests.
		OrmUtils.withinTransaction( sessionFactory, session -> {
			EntityWithDerivedFrom entity1 = session.load( EntityWithDerivedFrom.class, 1 );
			entity1.setA( 4 );

			session.persist( entity1 );

			backendMock.expectWorks( EntityWithDerivedFrom.INDEX )
					.update( "1", b -> b
							.field( "APlusB", 9 )
					)
					.processedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// C is not used to derive the transient property, so it should not trigger reindexing.
		OrmUtils.withinTransaction( sessionFactory, session -> {
			EntityWithDerivedFrom entity1 = session.load( EntityWithDerivedFrom.class, 1 );
			entity1.setC( 42 );

			session.persist( entity1 );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "noDerivedFrom")
	@Indexed(index = EntityWithoutDerivedFrom.INDEX)
	public static final class EntityWithoutDerivedFrom {

		static final String INDEX = "IndexWithoutDerivedFrom";

		@Id
		private Integer id;

		@Basic
		private int a;

		@Basic
		private int b;

		@Basic
		private int c;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public int getA() {
			return a;
		}

		public void setA(int a) {
			this.a = a;
		}

		public int getB() {
			return b;
		}

		public void setB(int b) {
			this.b = b;
		}

		public int getC() {
			return c;
		}

		public void setC(int c) {
			this.c = c;
		}

		@Transient
		@GenericField
		public Integer getAPlusB() {
			return a + b;
		}
	}

	@Entity(name = "derivedFrom")
	@Indexed(index = EntityWithDerivedFrom.INDEX)
	public static final class EntityWithDerivedFrom {

		static final String INDEX = "IndexWithDerivedFrom";

		@Id
		private Integer id;

		@Basic
		private int a;

		@Basic
		private int b;

		@Basic
		private int c;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public int getA() {
			return a;
		}

		public void setA(int a) {
			this.a = a;
		}

		public int getB() {
			return b;
		}

		public void setB(int b) {
			this.b = b;
		}

		public int getC() {
			return c;
		}

		public void setC(int c) {
			this.c = c;
		}

		@Transient
		@GenericField
		@IndexingDependency(derivedFrom = {
				@ObjectPath(@PropertyValue(propertyName = "a")),
				@ObjectPath(@PropertyValue(propertyName = "b"))
		})
		public Integer getAPlusB() {
			return a + b;
		}
	}


}
