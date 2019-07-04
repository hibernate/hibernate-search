/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.dirtiness.impl.PojoImplicitReindexingResolverNode;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * A very specific non-regression test for a case where Hibernate Search was erroneously considering
 * some dirty properties as requiring the reindexing of entities.
 * <p>
 * The problem essentially stemmed from the fact that among {@link PojoImplicitReindexingResolverNode} subclasses,
 * the type node had two responsibilities:
 * <ul>
 *     <li>Mark the object passed as an argument as "to reindex"</li>
 *     <li>Pass the object to property nodes</li>
 * </ul>
 * As a result, we could end up in the following situation,
 * where reindexing in type node #1 would happen when property 1 changed,
 * even though only property 2 matters to this node:
 * <pre><code>
   PojoImplicitReindexingResolverDirtinessFilterNode {
		dirtyPropertiesTriggeringReindexing=[property1, property2]
		delegate=PojoImplicitReindexingResolverOriginalTypeNode { // This is type node #1
			shouldMarkForReindexing=true
			nestedNodes=[
 				PojoImplicitReindexingResolverDirtinessFilterNode {
					dirtyPropertiesTriggeringReindexing=[property1]
					delegate=PojoImplicitReindexingResolverPropertyNode {
						handle=MethodHandleValueReadHandle[level1]
						nestedNodes=[
 							PojoImplicitReindexingResolverOriginalTypeNode { // This is type node #2
								shouldMarkForReindexing=true
								nestedNodes=[
								]
							}
						]
					}
				}
			]
		}
	}
 * </code></pre>
 *
 * Moving the responsibility of marking objects as "to reindex" to a nested node fixed the issue.
 */
@TestForIssue( jiraKey = "HSEARCH-3199")
public class AutomaticIndexingOverReindexingIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema( Level1Entity.INDEX, b -> b
				.field( "property1FromBridge", String.class )
		);

		backendMock.expectSchema( Level2Entity.INDEX, b -> b
					.objectField( "level3", b3 -> b3
							.field( "property2", String.class )
					)
		);

		sessionFactory = ormSetupHelper.start()
				.setup(
						Level1Entity.class,
						Level2Entity.class,
						Level3Entity.class
				);
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void test() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			Level1Entity level1 = new Level1Entity();
			level1.setId( 1 );

			Level2Entity level2 = new Level2Entity();
			level2.setId( 2 );
			level1.setLevel2( level2 );
			level2.setLevel1( level1 );

			Level3Entity level3 = new Level3Entity();
			level3.setId( 3 );
			level3.setProperty1( "initialValue" );
			level3.setProperty2( "initialValue" );
			level2.setLevel3( level3 );
			level3.setLevel2( level2 );

			session.persist( level3 );
			session.persist( level2 );
			session.persist( level1 );

			backendMock.expectWorks( Level1Entity.INDEX )
					.add( "1", b -> b
							.field( "property1FromBridge", "initialValue" )
					)
					.preparedThenExecuted();

			backendMock.expectWorks( Level2Entity.INDEX )
					.add( "2", b -> b
							.objectField( "level3", b2 -> b2
									.field( "property2", "initialValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value that should only affect level 2
		OrmUtils.withinTransaction( sessionFactory, session -> {
			Level3Entity level3 = session.get( Level3Entity.class, 3 );
			level3.setProperty2( "updatedValue" );

			backendMock.expectWorks( Level2Entity.INDEX )
					.update( "2", b -> b
							.objectField( "level3", b2 -> b2
									.field( "property2", "updatedValue" )
							)
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();

		// Test updating the value that should only affect level 1
		// This is what used to fail and we don't want to see regress: it used to reindex level 2 as well, for no good reason.
		OrmUtils.withinTransaction( sessionFactory, session -> {
			Level3Entity level3 = session.get( Level3Entity.class, 3 );
			level3.setProperty1( "updatedValue" );

			backendMock.expectWorks( Level1Entity.INDEX )
					.update( "1", b -> b
							.field( "property1FromBridge", "updatedValue" )
					)
					.preparedThenExecuted();
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = "level1")
	@Indexed(index = Level1Entity.INDEX)
	@Level3Property1BridgeAnnotation
	public static class Level1Entity {

		static final String INDEX = "level1";

		@Id
		private Integer id;

		@OneToOne
		private Level2Entity level2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Level2Entity getLevel2() {
			return level2;
		}

		public void setLevel2(Level2Entity level2) {
			this.level2 = level2;
		}

	}

	@Entity(name = "level2")
	@Indexed(index = Level2Entity.INDEX)
	public static class Level2Entity {

		static final String INDEX = "level2";

		@Id
		private Integer id;

		@OneToOne(mappedBy = "level2")
		private Level1Entity level1;

		@OneToOne
		@IndexedEmbedded(includePaths = { "property2" })
		private Level3Entity level3;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Level1Entity getLevel1() {
			return level1;
		}

		public void setLevel1(Level1Entity level1) {
			this.level1 = level1;
		}

		public Level3Entity getLevel3() {
			return level3;
		}

		public void setLevel3(Level3Entity level3) {
			this.level3 = level3;
		}
	}

	@Entity(name = "level3")
	public static class Level3Entity {

		static final String INDEX = "level3";

		@Id
		private Integer id;

		@OneToOne(mappedBy = "level3")
		private Level2Entity level2;

		@Basic
		private String property1;

		@Basic
		@GenericField
		private String property2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Level2Entity getLevel2() {
			return level2;
		}

		public void setLevel2(Level2Entity level2) {
			this.level2 = level2;
		}

		public String getProperty1() {
			return property1;
		}

		public void setProperty1(String property1) {
			this.property1 = property1;
		}

		public String getProperty2() {
			return property2;
		}

		public void setProperty2(String property2) {
			this.property2 = property2;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	@TypeBridgeMapping(bridge = @TypeBridgeRef(binderType = Level3Property1Bridge.Binder.class))
	public @interface Level3Property1BridgeAnnotation {

	}

	public static class Level3Property1Bridge implements TypeBridge {

		private PojoElementAccessor<String> level3Property1SourceAccessor;
		private IndexFieldReference<String> property1FromBridgeFieldReference;

		private Level3Property1Bridge(TypeBindingContext context) {
			level3Property1SourceAccessor = context.getBridgedElement().property( "level2" )
					.property( "level3" )
					.property( "property1" )
					.createAccessor( String.class );
			property1FromBridgeFieldReference = context.getIndexSchemaElement().field(
					"property1FromBridge", f -> f.asString()
			)
					.toReference();
		}

		@Override
		public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
			target.addValue(
					property1FromBridgeFieldReference,
					level3Property1SourceAccessor.read( bridgedElement )
			);
		}

		public static class Binder implements TypeBinder<Level3Property1BridgeAnnotation> {
			@Override
			public void bind(TypeBindingContext context) {
				context.setBridge( new Level3Property1Bridge( context ) );
			}
		}
	}
}
