/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo;

import static org.junit.Assert.fail;

import java.beans.Transient;
import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.mapper.javabean.JavaBeanMappingInitiator;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Yoann Rodiere
 */
public class JavaBeanMappingErrorIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void derivedFrom_invalidPath() {
		@Indexed
		class DerivedFromInvalidPath {
			@DocumentId
			Integer id;
			public Integer getId() {
				return id;
			}
			@Transient
			@Field
			@IndexingDependency(derivedFrom = @ObjectPath( @PropertyValue( propertyName = "invalidPath" ) ) )
			public String getDerived() {
				throw new UnsupportedOperationException( "Should not be called" );
			}
		}
		thrown.expectMessage( "Exception while retrieving property model" );
		thrown.expectMessage( "'invalidPath'" );
		startup( DerivedFromInvalidPath.class );
	}

	@Test
	public void derivedFrom_cycle() {
		class DerivedFromCycle {
			@Indexed
			class A {
				@DocumentId
				Integer id;
				B b;
				public Integer getId() {
					return id;
				}
				public B getB() {
					return b;
				}
				@Transient
				@Field
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "b"),
						@PropertyValue(propertyName = "derivedB")
				}))
				public String getDerivedA() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
			class B {
				C c;
				public C getC() {
					return c;
				}
				@Transient
				@Field
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "c"),
						@PropertyValue(propertyName = "derivedC")
				}))
				public String getDerivedB() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
			class C {
				A a;
				public A getA() {
					return a;
				}
				@Transient
				@Field
				@IndexingDependency(derivedFrom = @ObjectPath({
						@PropertyValue(propertyName = "a"),
						@PropertyValue(propertyName = "derivedA")
				}))
				public String getDerivedC() {
					throw new UnsupportedOperationException( "Should not be called" );
				}
			}
		}
		thrown.expectMessage( "Found a cyclic dependency between derived properties" );
		thrown.expectMessage(
				"path '"
				+ PojoModelPathValueNode.fromRoot( "derivedA" )
						.value( ContainerValueExtractorPath.noExtractors() )
				+ "'"
		);
		thrown.expectMessage( DerivedFromCycle.A.class.getName() );
		startup(
				CollectionHelper.asSet( DerivedFromCycle.A.class ),
				CollectionHelper.asSet( DerivedFromCycle.A.class, DerivedFromCycle.B.class, DerivedFromCycle.C.class )
		);
	}

	private void startup(Class<?> ... classes) {
		Set<Class<?>> classesSet = CollectionHelper.asSet( classes );
		startup( classesSet, classesSet );
	}

	private void startup(Set<Class<?>> entityTypes, Set<Class<?>> annotatedTypes) {
		SearchMappingRepositoryBuilder mappingRepositoryBuilder = SearchMappingRepository.builder()
				.setProperty( "backend.stubBackend.type", StubBackendFactory.class.getName() )
				.setProperty( "index.default.backend", "stubBackend" );

		JavaBeanMappingInitiator initiator = JavaBeanMappingInitiator.create(
				mappingRepositoryBuilder,
				MethodHandles.lookup(),
				true,
				false
		);

		initiator.addEntityTypes( entityTypes );

		initiator.annotationMapping().add( annotatedTypes );

		try ( SearchMappingRepository ignored = mappingRepositoryBuilder.build() ) {
			fail( "Expected a failure" );
		}
	}

}
