/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-3927")
class ProjectionConstructorInnerInferredIT extends AbstractProjectionConstructorIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void value() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( ValueMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, ValueMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", 11 ),
						Arrays.asList( "result2", 21 )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.field( "integer", Integer.class )
						)
						.asList(),
				Arrays.asList(
						new ValueMyProjection( "result1", 11 ),
						new ValueMyProjection( "result2", 21 )
				)
		);
	}

	static class ValueMyProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public ValueMyProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void value_multiValued_list() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public Collection<String> text;
			@GenericField
			public Set<Integer> integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Value_multiValued_listMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, Value_multiValued_listMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						Arrays.asList( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						Arrays.asList( Collections.emptyList(), Collections.emptyList() ),
						Arrays.asList( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ).multi(),
								f.field( "integer", Integer.class ).multi()
						)
						.asList(),
				Arrays.asList(
						new Value_multiValued_listMyProjection( Arrays.asList( "result1_1", "result1_2" ),
								Arrays.asList( 11, 12 ) ),
						new Value_multiValued_listMyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new Value_multiValued_listMyProjection( Collections.emptyList(), Collections.emptyList() ),
						new Value_multiValued_listMyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	static class Value_multiValued_listMyProjection {
		public final List<String> text;
		public final List<Integer> integer;

		@ProjectionConstructor
		public Value_multiValued_listMyProjection(List<String> text, List<Integer> integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void value_multiValued_collection() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public List<String> text;
			@GenericField
			public Set<Integer> integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Value_multiValued_collectionMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, Value_multiValued_collectionMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						Arrays.asList( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						Arrays.asList( Collections.emptyList(), Collections.emptyList() ),
						Arrays.asList( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ).multi(),
								f.field( "integer", Integer.class ).multi()
						)
						.asList(),
				Arrays.asList(
						new Value_multiValued_collectionMyProjection( Arrays.asList( "result1_1", "result1_2" ),
								Arrays.asList( 11, 12 ) ),
						new Value_multiValued_collectionMyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new Value_multiValued_collectionMyProjection( Collections.emptyList(), Collections.emptyList() ),
						new Value_multiValued_collectionMyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	static class Value_multiValued_collectionMyProjection {
		public final Collection<String> text;
		public final Collection<Integer> integer;

		@ProjectionConstructor
		public Value_multiValued_collectionMyProjection(Collection<String> text, Collection<Integer> integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void value_multiValued_iterable() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public List<String> text;
			@GenericField
			public Set<Integer> integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Value_multiValued_iterableMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, Value_multiValued_iterableMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						Arrays.asList( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						Arrays.asList( Collections.emptyList(), Collections.emptyList() ),
						Arrays.asList( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ).multi(),
								f.field( "integer", Integer.class ).multi()
						)
						.asList(),
				Arrays.asList(
						new Value_multiValued_iterableMyProjection( Arrays.asList( "result1_1", "result1_2" ),
								Arrays.asList( 11, 12 ) ),
						new Value_multiValued_iterableMyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new Value_multiValued_iterableMyProjection( Collections.emptyList(), Collections.emptyList() ),
						new Value_multiValued_iterableMyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	static class Value_multiValued_iterableMyProjection {
		public final Iterable<String> text;
		public final Iterable<Integer> integer;

		@ProjectionConstructor
		public Value_multiValued_iterableMyProjection(Iterable<String> text, Iterable<Integer> integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void value_multiValued_set() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public Collection<String> text;
			@GenericField
			public Set<Integer> integer;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Value_multiValued_setMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Value_multiValued_setMyProjection.class.getName() )
						.constructorContext( Set.class, List.class )
						.methodParameterContext( 0, "text" )
						.failure( "Invalid parameter type for projection constructor",
								"java.util.Set<java.lang.String>",
								"When inferring the cardinality of inner projections from constructor parameters,"
										+ " multi-valued constructor parameters must be lists (java.util.List<...>)"
										+ " or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );
	}

	static class Value_multiValued_setMyProjection {
		public final Set<String> text;
		public final List<Integer> integer;

		@ProjectionConstructor
		public Value_multiValued_setMyProjection(Set<String> text, List<Integer> integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void object() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public Contained contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( ObjectMyProjection.class, ObjectMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, ObjectMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", 11 ) ),
						Arrays.asList( "result2", Arrays.asList( null, null ) ),
						Arrays.asList( "result3", null )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												f.field( "contained.integer", Integer.class )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new ObjectMyProjection( "result1", new ObjectMyInnerProjection( "result1_1", 11 ) ),
						new ObjectMyProjection( "result2", new ObjectMyInnerProjection( null, null ) ),
						new ObjectMyProjection( "result3", null )
				)
		);
	}

	static class ObjectMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public ObjectMyInnerProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class ObjectMyProjection {
		public final String text;
		public final ObjectMyInnerProjection contained;

		@ProjectionConstructor
		public ObjectMyProjection(String text, ObjectMyInnerProjection contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	// If an inner projection type is not included in any Jandex index on startup,
	// Hibernate Search can still get on its feet thanks to annotated type discovery.
	@Test
	void object_annotatedTypeDiscovery() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public Contained contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				// We're not processing annotations on MyInnerProjection on purpose:
				// this simulates the class not being included in any Jandex index on startup.
				.withAnnotatedTypes( Object_annotatedTypeDiscoveryMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, Object_annotatedTypeDiscoveryMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", 11 ) ),
						Arrays.asList( "result2", Arrays.asList( null, null ) ),
						Arrays.asList( "result3", null )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												f.field( "contained.integer", Integer.class )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new Object_annotatedTypeDiscoveryMyProjection( "result1",
								new Object_annotatedTypeDiscoveryMyInnerProjection( "result1_1", 11 ) ),
						new Object_annotatedTypeDiscoveryMyProjection( "result2",
								new Object_annotatedTypeDiscoveryMyInnerProjection( null, null ) ),
						new Object_annotatedTypeDiscoveryMyProjection( "result3", null )
				)
		);
	}

	static class Object_annotatedTypeDiscoveryMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public Object_annotatedTypeDiscoveryMyInnerProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class Object_annotatedTypeDiscoveryMyProjection {
		public final String text;
		public final Object_annotatedTypeDiscoveryMyInnerProjection contained;

		@ProjectionConstructor
		public Object_annotatedTypeDiscoveryMyProjection(String text,
				Object_annotatedTypeDiscoveryMyInnerProjection contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void object_multiValued_list() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public Collection<Contained> contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Object_multiValued_listMyProjection.class, Object_multiValued_listMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, Object_multiValued_listMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList(
								Arrays.asList( "result1_1", 11 ),
								Arrays.asList( "result1_2", 12 )
						) ),
						Arrays.asList( "result2", Arrays.asList(
								Arrays.asList( "result2_1", 21 )
						) ),
						Arrays.asList( "result3", Collections.emptyList() ),
						Arrays.asList( "result4", Arrays.asList(
								Arrays.asList( "result4_1", 41 )
						) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												f.field( "contained.integer", Integer.class )
										)
										.asList()
										.multi()
						)
						.asList(),
				Arrays.asList(
						new Object_multiValued_listMyProjection( "result1", Arrays.asList(
								new Object_multiValued_listMyInnerProjection( "result1_1", 11 ),
								new Object_multiValued_listMyInnerProjection( "result1_2", 12 )
						) ),
						new Object_multiValued_listMyProjection( "result2", Arrays.asList(
								new Object_multiValued_listMyInnerProjection( "result2_1", 21 )
						) ),
						new Object_multiValued_listMyProjection( "result3", Collections.emptyList() ),
						new Object_multiValued_listMyProjection( "result4", Arrays.asList(
								new Object_multiValued_listMyInnerProjection( "result4_1", 41 )
						) )
				)
		);
	}

	static class Object_multiValued_listMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public Object_multiValued_listMyInnerProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class Object_multiValued_listMyProjection {
		public final String text;
		public final List<Object_multiValued_listMyInnerProjection> contained;

		@ProjectionConstructor
		public Object_multiValued_listMyProjection(String text, List<Object_multiValued_listMyInnerProjection> contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void object_multiValued_collection() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public List<Contained> contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Object_multiValued_collectionMyProjection.class,
						Object_multiValued_collectionMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, Object_multiValued_collectionMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList(
								Arrays.asList( "result1_1", 11 ),
								Arrays.asList( "result1_2", 12 )
						) ),
						Arrays.asList( "result2", Arrays.asList(
								Arrays.asList( "result2_1", 21 )
						) ),
						Arrays.asList( "result3", Collections.emptyList() ),
						Arrays.asList( "result4", Arrays.asList(
								Arrays.asList( "result4_1", 41 )
						) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												f.field( "contained.integer", Integer.class )
										)
										.asList()
										.multi()
						)
						.asList(),
				Arrays.asList(
						new Object_multiValued_collectionMyProjection( "result1", Arrays.asList(
								new Object_multiValued_collectionMyInnerProjection( "result1_1", 11 ),
								new Object_multiValued_collectionMyInnerProjection( "result1_2", 12 )
						) ),
						new Object_multiValued_collectionMyProjection( "result2", Arrays.asList(
								new Object_multiValued_collectionMyInnerProjection( "result2_1", 21 )
						) ),
						new Object_multiValued_collectionMyProjection( "result3", Collections.emptyList() ),
						new Object_multiValued_collectionMyProjection( "result4", Arrays.asList(
								new Object_multiValued_collectionMyInnerProjection( "result4_1", 41 )
						) )
				)
		);
	}

	static class Object_multiValued_collectionMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public Object_multiValued_collectionMyInnerProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class Object_multiValued_collectionMyProjection {
		public final String text;
		public final Collection<Object_multiValued_collectionMyInnerProjection> contained;

		@ProjectionConstructor
		public Object_multiValued_collectionMyProjection(String text,
				Collection<Object_multiValued_collectionMyInnerProjection> contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void object_multiValued_iterable() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public List<Contained> contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( Object_multiValued_iterableMyProjection.class,
						Object_multiValued_iterableMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, Object_multiValued_iterableMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList(
								Arrays.asList( "result1_1", 11 ),
								Arrays.asList( "result1_2", 12 )
						) ),
						Arrays.asList( "result2", Arrays.asList(
								Arrays.asList( "result2_1", 21 )
						) ),
						Arrays.asList( "result3", Collections.emptyList() ),
						Arrays.asList( "result4", Arrays.asList(
								Arrays.asList( "result4_1", 41 )
						) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												f.field( "contained.integer", Integer.class )
										)
										.asList()
										.multi()
						)
						.asList(),
				Arrays.asList(
						new Object_multiValued_iterableMyProjection( "result1", Arrays.asList(
								new Object_multiValued_iterableMyInnerProjection( "result1_1", 11 ),
								new Object_multiValued_iterableMyInnerProjection( "result1_2", 12 )
						) ),
						new Object_multiValued_iterableMyProjection( "result2", Arrays.asList(
								new Object_multiValued_iterableMyInnerProjection( "result2_1", 21 )
						) ),
						new Object_multiValued_iterableMyProjection( "result3", Collections.emptyList() ),
						new Object_multiValued_iterableMyProjection( "result4", Arrays.asList(
								new Object_multiValued_iterableMyInnerProjection( "result4_1", 41 )
						) )
				)
		);
	}

	static class Object_multiValued_iterableMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public Object_multiValued_iterableMyInnerProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class Object_multiValued_iterableMyProjection {
		public final String text;
		public final Iterable<Object_multiValued_iterableMyInnerProjection> contained;

		@ProjectionConstructor
		public Object_multiValued_iterableMyProjection(String text,
				Iterable<Object_multiValued_iterableMyInnerProjection> contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void object_multiValued_set() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public List<Contained> contained;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( Object_multiValued_setMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( Object_multiValued_setMyProjection.class.getName() )
						.constructorContext( String.class, Set.class )
						.methodParameterContext( 1, "contained" )
						.failure( "Invalid parameter type for projection constructor",
								"java.util.Set<" + Object_multiValued_setMyInnerProjection.class.getName() + ">",
								"When inferring the cardinality of inner projections from constructor parameters,"
										+ " multi-valued constructor parameters must be lists (java.util.List<...>)"
										+ " or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );
	}

	static class Object_multiValued_setMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public Object_multiValued_setMyInnerProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class Object_multiValued_setMyProjection {
		public final String text;
		public final Set<Object_multiValued_setMyInnerProjection> contained;

		@ProjectionConstructor
		public Object_multiValued_setMyProjection(String text, Set<Object_multiValued_setMyInnerProjection> contained) {
			this.text = text;
			this.contained = contained;
		}
	}
}
