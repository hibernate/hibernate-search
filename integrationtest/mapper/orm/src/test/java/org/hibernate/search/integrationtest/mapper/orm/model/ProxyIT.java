/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ProxyIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( EntityWithPropertyAccessTypeForId.INDEX, b -> b
				.field( "text", String.class )
		);

		sessionFactory = ormSetupHelper.start()
				.setup(
						ParentEntity.class,
						EntityWithPropertyAccessTypeForId.class
				);
		backendMock.verifyExpectationsMet();
	}

	/**
	 * This tests that Hibernate Search is able to index an entity that was initially retrieved as a proxy,
	 * either because it unproxies the entity before indexing,
	 * or because it uses the "property" (method) access type when accessing properties
	 * (either solution would work).
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-383")
	public void proxyAccess() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			EntityWithPropertyAccessTypeForId entity1 = new EntityWithPropertyAccessTypeForId();
			entity1.id = 1;
			entity1.text = "initialValue";

			session.persist( entity1 );

			backendMock.expectWorks( EntityWithPropertyAccessTypeForId.INDEX )
					.add( "1", b -> b
							.field( "text", entity1.text )
					)
					.processedThenExecuted();
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			ParentEntity proxy = session.load( ParentEntity.class, 1 );

			// 'proxy' is a Hibernate proxy and accessing its fields will not work, even after the proxy is initialized
			assertThat( proxy ).isInstanceOf( HibernateProxy.class );
			Hibernate.initialize( proxy );
			assertThat( proxy.id ).isNull();
			assertThat( proxy.text ).isNull();

			// ... yet if we trigger reindexing ...
			proxy.setText( "updatedValue" );

			// ... the ID is correctly detected
			backendMock.expectWorks( EntityWithPropertyAccessTypeForId.INDEX )
					.update( "1", b -> b
							.field( "text", proxy.getText() )
					)
					.processedThenExecuted();
		} );
	}

	@Entity
	public abstract static class ParentEntity {
		/*
		 * The annotation is on the ID *field*, not the method,
		 * but we expect HSearch to access the ID through the method due to the access type being PROPERTY.
		 */
		@DocumentId
		Integer id;

		// Same here
		@GenericField
		String text;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Basic
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Entity(name = "PropertyAccess")
	@Indexed(index = EntityWithPropertyAccessTypeForId.INDEX)
	public static class EntityWithPropertyAccessTypeForId extends ParentEntity {
		public static final String INDEX = "EntityWithPropertyAccessTypeForId";
	}

}
