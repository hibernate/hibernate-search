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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-4574")
class ProjectionConstructorObjectProjectionIT extends AbstractProjectionConstructorIT {

	@RegisterExtension
	public StandalonePojoMappingSetupHelper setupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock )
					// We don't care about reindexing here and don't want to configure association inverse sides
					.disableAssociationReindexing();

	@Test
	void noArg() {
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
				.withAnnotatedTypes( NoArgMyProjection.class, NoArgMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, NoArgMyProjection.class,
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
						new NoArgMyProjection( "result1", new NoArgMyInnerProjection( "result1_1" ) ),
						new NoArgMyProjection( "result2", new NoArgMyInnerProjection( null ) ),
						new NoArgMyProjection( "result3", null )
				)
		);
	}

	static class NoArgMyInnerProjection {
		public final String text;

		@ProjectionConstructor
		public NoArgMyInnerProjection(String text) {
			this.text = text;
		}
	}

	static class NoArgMyProjection {
		public final String text;
		public final NoArgMyInnerProjection contained;

		@ProjectionConstructor
		public NoArgMyProjection(String text, @ObjectProjection NoArgMyInnerProjection contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void path() {
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
			@IndexedEmbedded(name = "myContained")
			public Contained contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( PathMyProjection.class, PathMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, PathMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1" ) ),
						Arrays.asList( "result2", Arrays.asList( (Object) null ) ),
						Arrays.asList( "result3", null )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "myContained" )
										.from(
												f.field( "myContained.text", String.class )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new PathMyProjection( "result1", new PathMyInnerProjection( "result1_1" ) ),
						new PathMyProjection( "result2", new PathMyInnerProjection( null ) ),
						new PathMyProjection( "result3", null )
				)
		);
	}

	static class PathMyInnerProjection {
		public final String text;

		@ProjectionConstructor
		public PathMyInnerProjection(String text) {
			this.text = text;
		}
	}

	static class PathMyProjection {
		public final String text;
		public final PathMyInnerProjection contained;

		@ProjectionConstructor
		public PathMyProjection(String text,
				@ObjectProjection(path = "myContained") PathMyInnerProjection contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void invalidType() {
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

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( InvalidTypeMyProjection.class, InvalidTypeMyNonProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( InvalidTypeMyProjection.class.getName() )
						.constructorContext( InvalidTypeMyNonProjection.class )
						.methodParameterContext( 0, "contained" )
						.failure( "Invalid object class for projection",
								InvalidTypeMyNonProjection.class.getName(),
								"Make sure that this class is mapped correctly, "
										+ "either through annotations (@ProjectionConstructor) or programmatic mapping" ) );
	}

	static class InvalidTypeMyNonProjection {
		public final String text;
		public final Integer integer;

		public InvalidTypeMyNonProjection() {
			this.text = "foo";
			this.integer = 42;
		}

		public InvalidTypeMyNonProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class InvalidTypeMyProjection {
		public final InvalidTypeMyNonProjection contained;

		@ProjectionConstructor
		public InvalidTypeMyProjection(@ObjectProjection InvalidTypeMyNonProjection contained) {
			this.contained = contained;
		}
	}

	@Test
	void nonMatchingIncludePaths() {
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

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( NonMatchingIncludePathsMyProjection.class, NonMatchingIncludePathsMyInnerProjection.class )
				.setup( IndexedEntity.class, Contained.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( NonMatchingIncludePathsMyProjection.class.getName() )
						.projectionConstructorContext()
						.methodParameterContext( 1, "contained" )
						.failure(
								"ObjectProjectionBinder(...) defines includePaths filters that do not match anything",
								"Non-matching includePaths filters: [doesNotExist].",
								"Encountered field paths: [text].",
								"Check the filters for typos, or remove them if they are not useful."
						)
				);
	}

	static class NonMatchingIncludePathsMyInnerProjection {
		public final String text;

		@ProjectionConstructor
		public NonMatchingIncludePathsMyInnerProjection(String text) {
			this.text = text;
		}
	}

	static class NonMatchingIncludePathsMyProjection {
		public final String text;
		public final NonMatchingIncludePathsMyInnerProjection contained;

		@ProjectionConstructor
		public NonMatchingIncludePathsMyProjection(String text,
				@ObjectProjection(includePaths = "doesNotExist") NonMatchingIncludePathsMyInnerProjection contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void nonMatchingExcludePaths() {
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

		assertThatThrownBy( () -> setupHelper.start()
				.withAnnotatedTypes( NonMatchingExcludePathsMyProjection.class, NonMatchingExcludePathsMyInnerProjection.class )
				.setup( IndexedEntity.class, Contained.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( NonMatchingExcludePathsMyProjection.class.getName() )
						.projectionConstructorContext()
						.methodParameterContext( 1, "contained" )
						.failure(
								"ObjectProjectionBinder(...) defines excludePaths filters that do not match anything",
								"Non-matching excludePaths filters: [doesNotExist].",
								"Encountered field paths: [text].",
								"Check the filters for typos, or remove them if they are not useful."
						)
				);
	}

	static class NonMatchingExcludePathsMyInnerProjection {
		public final String text;

		@ProjectionConstructor
		public NonMatchingExcludePathsMyInnerProjection(String text) {
			this.text = text;
		}
	}

	static class NonMatchingExcludePathsMyProjection {
		public final String text;
		public final NonMatchingExcludePathsMyInnerProjection contained;

		@ProjectionConstructor
		public NonMatchingExcludePathsMyProjection(String text,
				@ObjectProjection(excludePaths = "doesNotExist") NonMatchingExcludePathsMyInnerProjection contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void inObjectField() {
		class ContainedLevel2 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class ContainedLevel1 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
			@IndexedEmbedded
			public ContainedLevel2 contained;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public ContainedLevel1 contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( InObjectFieldMyProjection.class, InObjectFieldMyInnerProjectionLevel1.class,
						InObjectFieldMyInnerProjectionLevel2.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, InObjectFieldMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1", Arrays.asList( "result1_1_1" ) ) ),
						Arrays.asList( "result2", Arrays.asList( null, null ) ),
						Arrays.asList( "result3", null ),
						Arrays.asList( "result4", Arrays.asList( "result4_1", Arrays.asList( (Object) null ) ) ),
						Arrays.asList( "result5", Arrays.asList( "result5_1", null ) )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												f.object( "contained.contained" )
														.from(
																f.field( "contained.contained.text", String.class )
														)
														.asList()
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new InObjectFieldMyProjection( "result1", new InObjectFieldMyInnerProjectionLevel1( "result1_1",
								new InObjectFieldMyInnerProjectionLevel2( "result1_1_1" ) ) ),
						new InObjectFieldMyProjection( "result2", new InObjectFieldMyInnerProjectionLevel1( null, null ) ),
						new InObjectFieldMyProjection( "result3", null ),
						new InObjectFieldMyProjection( "result4", new InObjectFieldMyInnerProjectionLevel1( "result4_1",
								new InObjectFieldMyInnerProjectionLevel2( null ) ) ),
						new InObjectFieldMyProjection( "result5",
								new InObjectFieldMyInnerProjectionLevel1( "result5_1", null ) )
				)
		);
	}

	static class InObjectFieldMyInnerProjectionLevel2 {
		public final String text;

		@ProjectionConstructor
		public InObjectFieldMyInnerProjectionLevel2(String text) {
			this.text = text;
		}
	}

	static class InObjectFieldMyInnerProjectionLevel1 {
		public final String text;
		public final InObjectFieldMyInnerProjectionLevel2 contained;

		@ProjectionConstructor
		public InObjectFieldMyInnerProjectionLevel1(String text,
				@ObjectProjection InObjectFieldMyInnerProjectionLevel2 contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	static class InObjectFieldMyProjection {
		public final String text;
		public final InObjectFieldMyInnerProjectionLevel1 contained;

		@ProjectionConstructor
		public InObjectFieldMyProjection(String text, @ObjectProjection InObjectFieldMyInnerProjectionLevel1 contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void inObjectField_filteredOut() {
		class ContainedLevel2 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class ContainedLevel1 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
			@IndexedEmbedded
			public ContainedLevel2 contained;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public ContainedLevel1 contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( InObjectField_filteredOutMyProjection.class,
						InObjectField_filteredOutMyInnerProjectionLevel1.class,
						InObjectField_filteredOutMyInnerProjectionLevel2.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, InObjectField_filteredOutMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1" ) ),
						Arrays.asList( "result2", null )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												// "contained.contained" got filtered out due to @ObjectProjection filters
												f.constant( null )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new InObjectField_filteredOutMyProjection( "result1",
								new InObjectField_filteredOutMyInnerProjectionLevel1( "result1_1", null ) ),
						new InObjectField_filteredOutMyProjection( "result2", null )
				)
		);
	}

	static class InObjectField_filteredOutMyInnerProjectionLevel2 {
		public final String text;

		@ProjectionConstructor
		public InObjectField_filteredOutMyInnerProjectionLevel2(String text) {
			this.text = text;
		}
	}

	static class InObjectField_filteredOutMyInnerProjectionLevel1 {
		public final String text;
		public final InObjectField_filteredOutMyInnerProjectionLevel2 contained;

		@ProjectionConstructor
		public InObjectField_filteredOutMyInnerProjectionLevel1(String text,
				@ObjectProjection InObjectField_filteredOutMyInnerProjectionLevel2 contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	static class InObjectField_filteredOutMyProjection {
		public final String text;
		public final InObjectField_filteredOutMyInnerProjectionLevel1 contained;

		@ProjectionConstructor
		public InObjectField_filteredOutMyProjection(String text,
				@ObjectProjection(includeDepth = 1) InObjectField_filteredOutMyInnerProjectionLevel1 contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void inObjectField_multiValued_filteredOut() {
		class ContainedLevel2 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
		}
		class ContainedLevel1 {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@GenericField
			public Integer integer;
			@IndexedEmbedded
			public List<ContainedLevel2> contained;
		}
		@Indexed(index = INDEX_NAME)
		class IndexedEntity {
			@DocumentId
			public Integer id;
			@FullTextField
			public String text;
			@IndexedEmbedded
			public ContainedLevel1 contained;
		}

		backendMock.expectAnySchema( INDEX_NAME );
		SearchMapping mapping = setupHelper.start()
				.withAnnotatedTypes( InObjectField_multiValued_filteredOutMyProjection.class,
						InObjectField_multiValued_filteredOutMyInnerProjectionLevel1.class,
						InObjectField_multiValued_filteredOutMyInnerProjectionLevel2.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, InObjectField_multiValued_filteredOutMyProjection.class,
				Arrays.asList(
						Arrays.asList( "result1", Arrays.asList( "result1_1" ) ),
						Arrays.asList( "result2", null )
				),
				f -> f.composite()
						.from(
								f.field( "text", String.class ),
								f.object( "contained" )
										.from(
												f.field( "contained.text", String.class ),
												// "contained.contained" got filtered out due to @ObjectProjection filters
												f.constant( Collections.emptyList() )
										)
										.asList()
						)
						.asList(),
				Arrays.asList(
						new InObjectField_multiValued_filteredOutMyProjection( "result1",
								new InObjectField_multiValued_filteredOutMyInnerProjectionLevel1( "result1_1",
										Collections.emptyList() ) ),
						new InObjectField_multiValued_filteredOutMyProjection( "result2", null )
				)
		);
	}

	static class InObjectField_multiValued_filteredOutMyInnerProjectionLevel2 {
		public final String text;

		@ProjectionConstructor
		public InObjectField_multiValued_filteredOutMyInnerProjectionLevel2(String text) {
			this.text = text;
		}
	}

	static class InObjectField_multiValued_filteredOutMyInnerProjectionLevel1 {
		public final String text;
		public final List<InObjectField_multiValued_filteredOutMyInnerProjectionLevel2> contained;

		@ProjectionConstructor
		public InObjectField_multiValued_filteredOutMyInnerProjectionLevel1(String text,
				@ObjectProjection List<InObjectField_multiValued_filteredOutMyInnerProjectionLevel2> contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	static class InObjectField_multiValued_filteredOutMyProjection {
		public final String text;
		public final InObjectField_multiValued_filteredOutMyInnerProjectionLevel1 contained;

		@ProjectionConstructor
		public InObjectField_multiValued_filteredOutMyProjection(String text,
				@ObjectProjection(includeDepth = 1) InObjectField_multiValued_filteredOutMyInnerProjectionLevel1 contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void multiValued_list() {
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
				.withAnnotatedTypes( MultiValued_listMyProjection.class, MultiValued_listMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MultiValued_listMyProjection.class,
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
						new MultiValued_listMyProjection( "result1", Arrays.asList(
								new MultiValued_listMyInnerProjection( "result1_1", 11 ),
								new MultiValued_listMyInnerProjection( "result1_2", 12 )
						) ),
						new MultiValued_listMyProjection( "result2", Arrays.asList(
								new MultiValued_listMyInnerProjection( "result2_1", 21 )
						) ),
						new MultiValued_listMyProjection( "result3", Collections.emptyList() ),
						new MultiValued_listMyProjection( "result4", Arrays.asList(
								new MultiValued_listMyInnerProjection( "result4_1", 41 )
						) )
				)
		);
	}

	static class MultiValued_listMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public MultiValued_listMyInnerProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class MultiValued_listMyProjection {
		public final String text;
		public final List<MultiValued_listMyInnerProjection> contained;

		@ProjectionConstructor
		public MultiValued_listMyProjection(String text, @ObjectProjection List<MultiValued_listMyInnerProjection> contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void multiValued_collection() {
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
				.withAnnotatedTypes( MultiValued_collectionMyProjection.class, MultiValued_collectionMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MultiValued_collectionMyProjection.class,
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
						new MultiValued_collectionMyProjection( "result1", Arrays.asList(
								new MultiValued_collectionMyInnerProjection( "result1_1", 11 ),
								new MultiValued_collectionMyInnerProjection( "result1_2", 12 )
						) ),
						new MultiValued_collectionMyProjection( "result2", Arrays.asList(
								new MultiValued_collectionMyInnerProjection( "result2_1", 21 )
						) ),
						new MultiValued_collectionMyProjection( "result3", Collections.emptyList() ),
						new MultiValued_collectionMyProjection( "result4", Arrays.asList(
								new MultiValued_collectionMyInnerProjection( "result4_1", 41 )
						) )
				)
		);
	}

	static class MultiValued_collectionMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public MultiValued_collectionMyInnerProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class MultiValued_collectionMyProjection {
		public final String text;
		public final Collection<MultiValued_collectionMyInnerProjection> contained;

		@ProjectionConstructor
		public MultiValued_collectionMyProjection(String text,
				@ObjectProjection Collection<MultiValued_collectionMyInnerProjection> contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void multiValued_iterable() {
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
				.withAnnotatedTypes( MultiValued_iterableMyProjection.class, MultiValued_iterableMyInnerProjection.class )
				.setup( IndexedEntity.class );

		testSuccessfulRootProjection(
				mapping, IndexedEntity.class, MultiValued_iterableMyProjection.class,
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
						new MultiValued_iterableMyProjection( "result1", Arrays.asList(
								new MultiValued_iterableMyInnerProjection( "result1_1", 11 ),
								new MultiValued_iterableMyInnerProjection( "result1_2", 12 )
						) ),
						new MultiValued_iterableMyProjection( "result2", Arrays.asList(
								new MultiValued_iterableMyInnerProjection( "result2_1", 21 )
						) ),
						new MultiValued_iterableMyProjection( "result3", Collections.emptyList() ),
						new MultiValued_iterableMyProjection( "result4", Arrays.asList(
								new MultiValued_iterableMyInnerProjection( "result4_1", 41 )
						) )
				)
		);
	}

	static class MultiValued_iterableMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public MultiValued_iterableMyInnerProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class MultiValued_iterableMyProjection {
		public final String text;
		public final Iterable<MultiValued_iterableMyInnerProjection> contained;

		@ProjectionConstructor
		public MultiValued_iterableMyProjection(String text,
				@ObjectProjection Iterable<MultiValued_iterableMyInnerProjection> contained) {
			this.text = text;
			this.contained = contained;
		}
	}

	@Test
	void multiValued_set() {
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
				.withAnnotatedTypes( MultiValued_setMyProjection.class )
				.setup( IndexedEntity.class ) )
				.isInstanceOf( SearchException.class )
				.satisfies( FailureReportUtils.hasFailureReport()
						.typeContext( MultiValued_setMyProjection.class.getName() )
						.constructorContext( String.class, Set.class )
						.methodParameterContext( 1, "contained" )
						.failure( "Invalid parameter type for projection constructor",
								"java.util.Set<" + MultiValued_setMyInnerProjection.class.getName() + ">",
								"When inferring the cardinality of inner projections from constructor parameters,"
										+ " multi-valued constructor parameters must be lists (java.util.List<...>)"
										+ " or list supertypes (java.lang.Iterable<...>, java.util.Collection<...>)" ) );
	}

	static class MultiValued_setMyInnerProjection {
		public final String text;
		public final Integer integer;

		@ProjectionConstructor
		public MultiValued_setMyInnerProjection(String text, Integer integer) {
			this.text = text;
			this.integer = integer;
		}
	}

	static class MultiValued_setMyProjection {
		public final String text;
		public final Set<MultiValued_setMyInnerProjection> contained;

		@ProjectionConstructor
		public MultiValued_setMyProjection(String text, @ObjectProjection Set<MultiValued_setMyInnerProjection> contained) {
			this.text = text;
			this.contained = contained;
		}
	}
}
