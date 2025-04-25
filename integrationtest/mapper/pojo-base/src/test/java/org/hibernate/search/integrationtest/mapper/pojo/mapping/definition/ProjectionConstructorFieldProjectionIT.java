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

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FieldProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-4574")
class ProjectionConstructorFieldProjectionIT extends AbstractProjectionConstructorIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	@Test
	void noArg() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( NoArgMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, NoArgMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1" ),
						Arrays.asList( "result2" )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class )
						)
						.asList(),
				Arrays.asList(
						new NoArgMyProjection( "result1" ),
						new NoArgMyProjection( "result2" )
				)
		);
	}

	static class NoArgMyProjection {
		public final String text;

		@ProjectionConstructor
		public NoArgMyProjection(@FieldProjection String text) {
			this.text = text;
		}
	}

	@Test
	void path() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField(name = "myText")
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( PathMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, PathMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1" ),
						Arrays.asList( "result2" )
				),
				f -> f.composite()
						.from(
								f.field( "myText", String.class )
						)
						.asList(),
				Arrays.asList(
						new PathMyProjection( "result1" ),
						new PathMyProjection( "result2" )
				)
		);
	}

	static class PathMyProjection {
		public final String text;

		@ProjectionConstructor
		public PathMyProjection(@FieldProjection(path = "myText") String text) {
			this.text = text;
		}
	}

	@Test
	void valueModel() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField
			public MyEnum myEnum;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( ValueModelMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, ValueModelMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1" ),
						Arrays.asList( "result2" )
				),
				f -> f.composite()
						.from(
								f.field( "myEnum", String.class, ValueModel.INDEX )
						)
						.asList(),
				Arrays.asList(
						new ValueModelMyProjection( "result1" ),
						new ValueModelMyProjection( "result2" )
				)
		);
	}

	static class ValueModelMyProjection {
		public final String myEnum;

		@ProjectionConstructor
		public ValueModelMyProjection(
				@FieldProjection(valueModel = ValueModel.INDEX) String myEnum) {
			this.myEnum = myEnum;
		}
	}

	@Deprecated(since = "test")
	@Test
	void valueConvert_nonDefault() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField
			public MyEnum myEnum;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( ValueConvert_nonDefaultMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, ValueConvert_nonDefaultMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1" ),
						Arrays.asList( "result2" )
				),
				f -> f.composite()
						.from(
								f.field( "myEnum", String.class, ValueModel.INDEX )
						)
						.asList(),
				Arrays.asList(
						new ValueConvert_nonDefaultMyProjection( "result1" ),
						new ValueConvert_nonDefaultMyProjection( "result2" )
				)
		);
	}

	@Deprecated(since = "test")
	static class ValueConvert_nonDefaultMyProjection {
		public final String myEnum;

		@ProjectionConstructor
		public ValueConvert_nonDefaultMyProjection(
				@FieldProjection(convert = org.hibernate.search.engine.search.common.ValueConvert.NO) String myEnum) {
			this.myEnum = myEnum;
		}
	}

	@Deprecated(since = "test")
	@Test
	void valueConvertAndValueModel_nonDefaultFails() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField
			public MyEnum myEnum;
		}

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( ValueConvertAndValueModel_nonDefaultFailsMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Using non-default `valueModel=ValueModel.INDEX` and `convert=ValueConvert.NO` at the same time is not allowed. Remove the `convert` attribute and keep only the `valueModel=ValueModel.INDEX`." );
	}

	@Deprecated(since = "test")
	static class ValueConvertAndValueModel_nonDefaultFailsMyProjection {
		public final String myEnum;

		@ProjectionConstructor
		public ValueConvertAndValueModel_nonDefaultFailsMyProjection(
				@FieldProjection(convert = org.hibernate.search.engine.search.common.ValueConvert.NO,
						valueModel = ValueModel.INDEX) String myEnum) {
			this.myEnum = myEnum;
		}
	}

	enum MyEnum {
		FOO, BAR;
	}

	@Test
	void supertype() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( SupertypeMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, SupertypeMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1" ),
						Arrays.asList( "result2" )
				),
				f -> f.composite()
						.from(
								f.field( "text", Object.class )
						)
						.asList(),
				Arrays.asList(
						new SupertypeMyProjection( "result1" ),
						new SupertypeMyProjection( "result2" )
				)
		);
	}

	static class SupertypeMyProjection {
		public final Object text;

		@ProjectionConstructor
		public SupertypeMyProjection(@FieldProjection Object text) {
			this.text = text;
		}
	}

	@Test
	void primitiveType() {
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@GenericField
			public int integer;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( PrimitiveTypeMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, PrimitiveTypeMyProjection.class,
				Arrays.asList(
						Arrays.asList( 1 ),
						Arrays.asList( 2 )
				),
				f -> f.composite()
						.from(
								f.field( "integer", Integer.class )
						)
						.asList(),
				Arrays.asList(
						new PrimitiveTypeMyProjection( 1 ),
						new PrimitiveTypeMyProjection( 2 )
				)
		);
	}

	static class PrimitiveTypeMyProjection {
		public final int integer;

		@ProjectionConstructor
		public PrimitiveTypeMyProjection(@FieldProjection int integer) {
			this.integer = integer;
		}
	}

	@Test
	void inObjectField() {
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
				.withAnnotatedTypes( InObjectFieldMyProjection.class, InObjectFieldMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, InObjectFieldMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1" ) ),
						Arrays.asList( "result2", Arrays.asList( (Object) null ) ),
						Arrays.asList( "result3", null )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new InObjectFieldMyProjection( "result1", new InObjectFieldMyInnerProjection( "result1_1" ) ),
						new InObjectFieldMyProjection( "result2", new InObjectFieldMyInnerProjection( null ) ),
						new InObjectFieldMyProjection( "result3", null )
				)
		);
	}

	static class InObjectFieldMyInnerProjection {
		public final String text;

		@ProjectionConstructor
		public InObjectFieldMyInnerProjection(@FieldProjection String text) {
			this.text = text;
		}
	}

	static class InObjectFieldMyProjection {
		public final String text;
		public final InObjectFieldMyInnerProjection contained;

		@ProjectionConstructor
		public InObjectFieldMyProjection(@FieldProjection String text, InObjectFieldMyInnerProjection contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void inObjectField_filteredOut() {
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
				.withAnnotatedTypes( InObjectField_filteredOutMyProjection.class,
						InObjectField_filteredOutMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, InObjectField_filteredOutMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", null ) ),
						Arrays.asList( "result2", null )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												// "contained.integer" got filtered out due to @ObjectProjection filters
												f.constant( null )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new InObjectField_filteredOutMyProjection( "result1",
								new InObjectField_filteredOutMyInnerProjection( "result1_1", null ) ),
						new InObjectField_filteredOutMyProjection( "result2", null )
				)
		);
	}

	static class InObjectField_filteredOutMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public InObjectField_filteredOutMyInnerProjection(@FieldProjection String text, @FieldProjection Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class InObjectField_filteredOutMyProjection {
		public final String text;
		public final InObjectField_filteredOutMyInnerProjection contained;

		@ProjectionConstructor
		public InObjectField_filteredOutMyProjection(@FieldProjection String text,
				@ObjectProjection(includePaths = { "text" }) InObjectField_filteredOutMyInnerProjection contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void inObjectField_multiValued_filteredOut() {
		class Contained {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public List<Integer> integer;
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
				.withAnnotatedTypes( InObjectField_multiValued_filteredOutMyProjection.class,
						InObjectField_multiValued_filteredOutMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, InObjectField_multiValued_filteredOutMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", null ) ),
						Arrays.asList( "result2", null )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												// "contained.integer" got filtered out due to @ObjectProjection filters
												f.constant( Collections.emptyList() )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new InObjectField_multiValued_filteredOutMyProjection( "result1",
								new InObjectField_multiValued_filteredOutMyInnerProjection( "result1_1",
										Collections.emptyList() ) ),
						new InObjectField_multiValued_filteredOutMyProjection( "result2", null )
				)
		);
	}

	static class InObjectField_multiValued_filteredOutMyInnerProjection {
		public final String text;
		public final List<Integer> integer;

		@ProjectionConstructor
		public InObjectField_multiValued_filteredOutMyInnerProjection(@FieldProjection String text,
				@FieldProjection List<Integer> integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class InObjectField_multiValued_filteredOutMyProjection {
		public final String text;
		public final InObjectField_multiValued_filteredOutMyInnerProjection contained;

		@ProjectionConstructor
		public InObjectField_multiValued_filteredOutMyProjection(@FieldProjection String text,
				@ObjectProjection(includePaths = { "text" }) InObjectField_multiValued_filteredOutMyInnerProjection contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void multiValued_list() {
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
				.withAnnotatedTypes( MultiValued_listMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MultiValued_listMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						Arrays.asList( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						Arrays.asList( Collections.emptyList(), Collections.emptyList() ),
						Arrays.asList( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ).collector( ProjectionCollector.list() ),
								f.field( "integer", Integer.class ).collector( ProjectionCollector.list() )
						)
						.asList(),
				Arrays.asList(
						new MultiValued_listMyProjection( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						new MultiValued_listMyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new MultiValued_listMyProjection( Collections.emptyList(), Collections.emptyList() ),
						new MultiValued_listMyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	static class MultiValued_listMyProjection {
		public final List<String> text;
		public final List<Integer> integer;

		@ProjectionConstructor
		public MultiValued_listMyProjection(@FieldProjection List<String> text, @FieldProjection List<Integer> integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void multiValued_collection() {
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
				.withAnnotatedTypes( MultiValued_collectionMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MultiValued_collectionMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						Arrays.asList( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						Arrays.asList( Collections.emptyList(), Collections.emptyList() ),
						Arrays.asList( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ).collector( ProjectionCollector.list() ),
								f.field( "integer", Integer.class ).collector( ProjectionCollector.list() )
						)
						.asList(),
				Arrays.asList(
						new MultiValued_collectionMyProjection( Arrays.asList( "result1_1", "result1_2" ),
								Arrays.asList( 11, 12 ) ),
						new MultiValued_collectionMyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new MultiValued_collectionMyProjection( Collections.emptyList(), Collections.emptyList() ),
						new MultiValued_collectionMyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	static class MultiValued_collectionMyProjection {
		public final Collection<String> text;
		public final Collection<Integer> integer;

		@ProjectionConstructor
		public MultiValued_collectionMyProjection(@FieldProjection Collection<String> text,
				@FieldProjection Collection<Integer> integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void multiValued_iterable() {
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
				.withAnnotatedTypes( MultiValued_iterableMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MultiValued_iterableMyProjection.class,
				Arrays.asList(
						Arrays.asList( Arrays.asList( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						Arrays.asList( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						Arrays.asList( Collections.emptyList(), Collections.emptyList() ),
						Arrays.asList( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ).collector( ProjectionCollector.list() ),
								f.field( "integer", Integer.class ).collector( ProjectionCollector.list() )
						)
						.asList(),
				Arrays.asList(
						new MultiValued_iterableMyProjection( Arrays.asList( "result1_1", "result1_2" ),
								Arrays.asList( 11, 12 ) ),
						new MultiValued_iterableMyProjection( Arrays.asList( "result2_1" ), Arrays.asList( 21 ) ),
						new MultiValued_iterableMyProjection( Collections.emptyList(), Collections.emptyList() ),
						new MultiValued_iterableMyProjection( Arrays.asList( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	static class MultiValued_iterableMyProjection {
		public final Iterable<String> text;
		public final Iterable<Integer> integer;

		@ProjectionConstructor
		public MultiValued_iterableMyProjection(@FieldProjection Iterable<String> text,
				@FieldProjection Iterable<Integer> integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	@Test
	void multiValued_set() {
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
				.withAnnotatedTypes( MultiValued_setMyProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MultiValued_setMyProjection.class,
				Arrays.asList(
						Arrays.asList( Set.of( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						Arrays.asList( Set.of( "result2_1" ), Arrays.asList( 21 ) ),
						Arrays.asList( Set.of(), Collections.emptyList() ),
						Arrays.asList( Set.of( "result4_1" ), Arrays.asList( 41 ) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ).collector( ProjectionCollector.set() ),
								f.field( "integer", Integer.class ).collector( ProjectionCollector.list() )
						)
						.asList(),
				Arrays.asList(
						new MultiValued_setMyProjection( Set.of( "result1_1", "result1_2" ), Arrays.asList( 11, 12 ) ),
						new MultiValued_setMyProjection( Set.of( "result2_1" ), Arrays.asList( 21 ) ),
						new MultiValued_setMyProjection( Set.of(), Collections.emptyList() ),
						new MultiValued_setMyProjection( Set.of( "result4_1" ), Arrays.asList( 41 ) )
				)
		);
	}

	static class MultiValued_setMyProjection {
		public final Set<String> text;
		public final List<Integer> integer;

		@ProjectionConstructor
		public MultiValued_setMyProjection(@FieldProjection Set<String> text, @FieldProjection List<Integer> integer) {
			this.text = text;
			this.integer = integer;
		}
	}

}
