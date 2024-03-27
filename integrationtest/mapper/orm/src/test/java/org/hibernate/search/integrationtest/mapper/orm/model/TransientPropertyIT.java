/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class TransientPropertyIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	void withoutDerivedFrom() {
		assertThatThrownBy(
				() -> ormSetupHelper.start().setup( EntityWithoutDerivedFrom.class )
		)
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( EntityWithoutDerivedFrom.class.getName() )
						.failure(
								"Unable to resolve path '.APlusB' to a persisted attribute in Hibernate ORM metadata.",
								"If this path points to a transient attribute, use @IndexingDependency(derivedFrom = ...)"
										+ " to specify which persisted attributes it is derived from.",
								"See the reference documentation for more information"
						) );
	}

	@Test
	void withDerivedFrom() {
		backendMock.expectSchema( EntityWithDerivedFrom.INDEX, b -> b
				.field( "APlusB", Integer.class )
		);

		SessionFactory sessionFactory = ormSetupHelper.start().setup( EntityWithDerivedFrom.class );

		with( sessionFactory ).runInTransaction( session -> {
			EntityWithDerivedFrom entity1 = new EntityWithDerivedFrom();
			entity1.setId( 1 );
			entity1.setA( 2 );
			entity1.setB( 5 );
			entity1.setC( 11 );

			session.persist( entity1 );

			backendMock.expectWorks( EntityWithDerivedFrom.INDEX )
					.add( "1", b -> b
							.field( "APlusB", 7 )
					);
		} );
		backendMock.verifyExpectationsMet();

		// A is used to derive the transient property, so it should trigger reindexing.
		// More related tests in the AutomaticIndexing*IT tests.
		with( sessionFactory ).runInTransaction( session -> {
			EntityWithDerivedFrom entity1 = session.getReference( EntityWithDerivedFrom.class, 1 );
			entity1.setA( 4 );

			session.persist( entity1 );

			backendMock.expectWorks( EntityWithDerivedFrom.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "APlusB", 9 )
					);
		} );
		backendMock.verifyExpectationsMet();

		// C is not used to derive the transient property, so it should not trigger reindexing.
		with( sessionFactory ).runInTransaction( session -> {
			EntityWithDerivedFrom entity1 = session.getReference( EntityWithDerivedFrom.class, 1 );
			entity1.setC( 42 );

			session.persist( entity1 );
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void withDerivedFromAndBridge() {
		backendMock.expectSchema( EntityWithDerivedFromAndBridge.INDEX, b -> b
				.field( "APlusB", Integer.class )
		);

		SessionFactory sessionFactory = ormSetupHelper.start().setup( EntityWithDerivedFromAndBridge.class );

		with( sessionFactory ).runInTransaction( session -> {
			EntityWithDerivedFromAndBridge entity1 = new EntityWithDerivedFromAndBridge();
			entity1.setId( 1 );
			entity1.setA( 2 );
			entity1.setB( 5 );
			entity1.setC( 11 );

			session.persist( entity1 );

			backendMock.expectWorks( EntityWithDerivedFromAndBridge.INDEX )
					.add( "1", b -> b
							.field( "APlusB", 7 )
					);
		} );
		backendMock.verifyExpectationsMet();

		// A is used to derive the transient property, so it should trigger reindexing.
		// More related tests in the AutomaticIndexing*IT tests.
		with( sessionFactory ).runInTransaction( session -> {
			EntityWithDerivedFromAndBridge entity1 = session.getReference( EntityWithDerivedFromAndBridge.class, 1 );
			entity1.setA( 4 );

			session.persist( entity1 );

			backendMock.expectWorks( EntityWithDerivedFromAndBridge.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "APlusB", 9 )
					);
		} );
		backendMock.verifyExpectationsMet();

		// C is not used to derive the transient property, so it should not trigger reindexing.
		with( sessionFactory ).runInTransaction( session -> {
			EntityWithDerivedFromAndBridge entity1 = session.getReference( EntityWithDerivedFromAndBridge.class, 1 );
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

	@Entity(name = "drvdFAndBdge")
	@Indexed(index = EntityWithDerivedFromAndBridge.INDEX)
	public static final class EntityWithDerivedFromAndBridge {

		static final String INDEX = "IndexWithDerivedFromAndBridge";

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
		@PropertyBinding(binder = @PropertyBinderRef(type = AdditionBinder.class))
		@IndexingDependency(derivedFrom = {
				@ObjectPath(@PropertyValue(propertyName = "a")),
				@ObjectPath(@PropertyValue(propertyName = "b"))
		})
		public Addition getAPlusB() {
			return new Addition( a, b );
		}
	}

	private static final class Addition {
		public final int left;
		public final int right;

		private Addition(int left, int right) {
			this.left = left;
			this.right = right;
		}
	}

	public static class AdditionBinder implements PropertyBinder {

		@Override
		public void bind(PropertyBindingContext context) {
			context.dependencies().use( "left" ).use( "right" );

			IndexFieldReference<Integer> fieldRef = context.indexSchemaElement()
					.field( context.bridgedElement().name(), f -> f.asInteger() )
					.toReference();

			context.bridge( Addition.class, new Bridge( fieldRef ) );
		}

		private static class Bridge implements PropertyBridge<Addition> {
			private final IndexFieldReference<Integer> fieldRef;

			public Bridge(IndexFieldReference<Integer> fieldRef) {
				this.fieldRef = fieldRef;
			}

			@Override
			public void write(DocumentElement target, Addition addition, PropertyBridgeWriteContext context) {
				target.addValue( fieldRef, addition.left + addition.right );
			}
		}
	}


}
