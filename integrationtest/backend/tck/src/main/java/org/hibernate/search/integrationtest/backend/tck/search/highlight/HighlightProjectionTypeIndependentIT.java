/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HighlightProjectionTypeIndependentIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );
	private static final SimpleMappedIndex<NestedIndexBinding> nestedIndex =
			SimpleMappedIndex.of( NestedIndexBinding::new ).name( "nestedIndex" );
	private static final SimpleMappedIndex<NotMatchingTypeIndexBinding> notMatchingTypeIndex =
			SimpleMappedIndex.of( NotMatchingTypeIndexBinding::new ).name( "notMatchingTypeIndex" );
	private static final SimpleMappedIndex<IndexBinding> matchingIndex = SimpleMappedIndex.of( IndexBinding::new )
			.name( "matchingIndex" );

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndex( index )
				.withIndex( nestedIndex )
				.withIndex( notMatchingTypeIndex )
				.withIndex( matchingIndex )
				.setup();

		index.bulkIndexer()
				.add( "1", d -> d.addValue( "string", "This string mentions a dog" ) )
				.add( "2", d -> d.addValue( "string", "This string mentions a dog too" ) )
				.join();

		matchingIndex.bulkIndexer()
				.add( "100", d -> d.addValue( "string", "string with dog" ) )
				.join();
	}

	@Test
	void unknownField() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy(
				() -> scope.query().select( f -> f.highlight( "unknownField" ) ).where( f -> f.matchAll() ).toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unknown field",
						"unknownField",
						index.name()
				);
	}

	@Test
	void objectField_nested() {
		String fieldPath = index.binding().nestedObject.relativeFieldName;
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query().select( f -> f.highlight( fieldPath ) ).where( f -> f.matchAll() ).toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'projection:highlight' on field '" + fieldPath + "'" );
	}

	/*
	 * Trying to create a highlight projection on any field located inside a `ObjectStructure.NESTED` object
	 * should result in an error.
	 */
	@Test
	void objectField_nested_field() {
		String fieldPath = index.binding().nestedObject.relativeFieldName + ".string";
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query().select( f -> f.highlight( fieldPath ) ).where( f -> f.matchAll() ).toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'projection:highlight' on field '" + fieldPath + "'" );
	}

	@Test
	void objectField_flattened() {
		String fieldPath = index.binding().flattenedObject.relativeFieldName;
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query().select( f -> f.highlight( fieldPath ) ).where( f -> f.matchAll() ).toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'projection:highlight' on field '" + fieldPath + "'" );
	}

	@Test
	void highlighterNullName() {
		assertThatThrownBy(
				() -> index.createScope().query().select(
						f -> f.highlight( "string" )
				).where( f -> f.matchAll() )
						.highlighter( null, h -> h.plain() )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Named highlighters cannot use a blank string as name."
				);
	}

	@Test
	void highlighterEmptyName() {
		assertThatThrownBy(
				() -> index.createScope().query().select(
						f -> f.highlight( "string" )
				).where( f -> f.matchAll() )
						.highlighter( "", h -> h.plain() )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Named highlighters cannot use a blank string as name."
				);
	}

	@Test
	void highlighterSameName() {
		assertThatThrownBy(
				() -> index.createScope().query().select(
						f -> f.highlight( "string" )
				).where( f -> f.matchAll() )
						.highlighter( "same-name", h -> h.plain() )
						.highlighter( "same-name", h -> h.plain() )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Highlighter with name 'same-name' is already defined. Use a different name to add another highlighter."
				);
	}

	@Test
	void cannotHighlightNestedObjectStructureFields() {
		assertThatThrownBy( () -> nestedIndex.createScope().query().select(
				f -> f.highlight( "objectNested.string" )
		).where( f -> f.matchAll() )
				.toQuery() ).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:highlight' on field 'objectNested.string'",
						"The highlight projection cannot be applied to a field from an object using `ObjectStructure.NESTED` structure",
						"Context: field 'objectNested.string'"
				);
	}

	/*
	 * In this test we are only looking at the default/flattened object structure,
	 * nested structure is tested elsewhere, and it produces an exception in a different place compared to the use of flattened fields.
	 */
	@Test
	void inObjectProjection() {
		List<String> objects = Arrays.asList( "objectDefault", "objectFlattened" );
		for ( String object : objects ) {
			assertThatThrownBy( () -> nestedIndex.query().select(
					f -> f.object( object )
							.from(
									f.composite().from(
											f.highlight( object + ".string" )
									).asList()
							)
							.asList()
			)
					.where( f -> f.matchAll() )
					.toQuery() )
					.as( object )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Highlight projection cannot be applied within nested context of",
							object
					);
			for ( String level2 : objects ) {
				assertThatThrownBy( () -> nestedIndex.query().select(
						f -> f.object( object )
								.from(
										f.composite().from(
												f.field( object + ".string" ),
												f.object( object + ".level2" + level2 )
														.from( f.highlight( object + ".level2" + level2 + ".string" ) )
														.asList()
										).asList()
								)
								.asList()
				)
						.where( f -> f.matchAll() )
						.toQuery() )
						.as( object )
						.isInstanceOf( SearchException.class )
						.hasMessageContainingAll(
								"Highlight projection cannot be applied within nested context of",
								object,
								level2
						);
			}
		}
	}

	@Test
	void highlightable_enabled_trait() {
		assertThat( Arrays.asList( "string", "objectFlattened.string" ) )
				.allSatisfy( fieldPath -> assertThat( index.toApi().descriptor().field( fieldPath ) )
						.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
								.as( "traits of field '" + fieldPath + "'" )
								.contains( "projection:highlight" ) ) );
	}

	@Test
	void projectable_no_trait() {
		String fieldPath = "stringNotProjectable";
		if ( TckConfiguration.get().getBackendFeatures().supportsHighlightableWithoutProjectable() ) {
			assertThat( index.toApi().descriptor().field( fieldPath ) )
					.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
							.as( "traits of field '" + fieldPath + "'" )
							.contains( IndexFieldTraits.Projections.HIGHLIGHT ) );
		}
		else {
			assertThat( index.toApi().descriptor().field( fieldPath ) )
					.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
							.as( "traits of field '" + fieldPath + "'" )
							.doesNotContain( IndexFieldTraits.Projections.HIGHLIGHT ) );
		}
	}

	@Test
	void highlightable_enabled_trait_nested() {
		assertThat( Arrays.asList(
				"objectNested.string",
				"objectNested.level2objectDefault.string",
				"objectNested.level2objectNested.string",
				"objectNested.level2objectFlattened.string",
				"objectDefault.level2objectNested.string",
				"objectFlattened.level2objectNested.string"
		) )
				.allSatisfy( inObjectFieldPath -> assertThat( nestedIndex.toApi().descriptor().field( inObjectFieldPath ) )
						.hasValueSatisfying( fieldDescriptor -> assertThat( fieldDescriptor.type().traits() )
								.as( "traits of field '" + inObjectFieldPath + "'" )
								// See HSEARCH-4841: highlighting is forbidden on nested fields...
								// but here we're inspecting the field *type*, which unfortunately
								// is independent of the field structure and thus doesn't know
								// highlighting is not available.
								.contains( IndexFieldTraits.Projections.HIGHLIGHT ) ) );
	}

	@Test
	void highlightNonAnalyzedField() {
		assertThatThrownBy(
				() -> index.createScope().query().select(
						f -> f.highlight( "notAnalyzedString" )
				).where( f -> f.matchAll() )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:highlight' on field 'notAnalyzedString':",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant).",
						"If it already is, then 'projection:highlight' is not available for fields of this type."
				);
	}

	@Test
	void multipleIndexesScopeIncompatibleTypes() {
		assertThatThrownBy(
				() -> index.createScope( notMatchingTypeIndex ).query().select(
						f -> f.highlight( "string" )
				).where( f -> f.matchAll() )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent support for 'projection:highlight'",
						"'projection:highlight' can be used in some of the targeted indexes, but not all of them.",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant) in all indexes, and that the field has the same type in all indexes"
				);
	}

	@Test
	void multipleIndexesScopeIncompatibleTypesInObjectField() {
		assertThatThrownBy(
				() -> index.createScope( notMatchingTypeIndex ).query().select(
						f -> f.highlight( "objectFlattened.string" )
				).where( f -> f.matchAll() )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent support for 'projection:highlight'",
						"'projection:highlight' can be used in some of the targeted indexes, but not all of them.",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant) in all indexes, and that the field has the same type in all indexes."
				);
	}

	@Test
	void multipleIndexesScopeCompatibleTypes() {
		SearchQuery<List<String>> highlights = index.createScope( matchingIndex ).query().select(
				f -> f.highlight( "string" )
		).where( f -> f.match().field( "string" ).matching( "dog" ) )
				.highlighter( h -> h.plain() )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder( Arrays.asList(
						Collections.singletonList( "string with <em>dog</em>" ),
						Collections.singletonList( "This string mentions a <em>dog</em>" ),
						Collections.singletonList( "This string mentions a <em>dog</em> too" )
				) );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> stringField;
		final IndexFieldReference<String> notAnalyzedString;
		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;
		final IndexFieldReference<String> stringNotProjectableField;

		IndexBinding(IndexSchemaElement root) {
			stringField = root.field( "string", f -> f.asString()
					.highlightable( Collections.singleton( Highlightable.ANY ) )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					.termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS )
			).toReference();
			notAnalyzedString = root.field( "notAnalyzedString", f -> f.asString() ).toReference();

			flattenedObject = new ObjectMapping( root, "objectFlattened", ObjectStructure.FLATTENED );
			nestedObject = new ObjectMapping( root, "objectNested", ObjectStructure.NESTED );

			stringNotProjectableField = root.field( "stringNotProjectable", f -> f.asString()
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			).toReference();
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final IndexFieldReference<String> string;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			self = objectField.toReference();
			string = objectField.field( "string", f -> f.asString()
					.highlightable( Collections.singleton( Highlightable.ANY ) )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).toReference();
		}
	}

	private static class NestedIndexBinding {
		NestedIndexBinding(IndexSchemaElement root) {
			createObjects( "", root, 2, true );
		}

		private void createObjects(String prefix, IndexSchemaElement element, int level, boolean addAnotherLevel) {
			IndexSchemaObjectField objectDefault = element.objectField( prefix + "objectDefault" );

			createString( "string", objectDefault );
			if ( addAnotherLevel ) {
				createObjects( "level" + ( level ), objectDefault, level + 1, false );
			}
			objectDefault.toReference();

			IndexSchemaObjectField objectNested = element.objectField( prefix + "objectNested", ObjectStructure.NESTED );
			createString( "string", objectNested );
			if ( addAnotherLevel ) {
				createObjects( "level" + ( level ), objectNested, level + 1, false );
			}
			objectNested.toReference();

			IndexSchemaObjectField objectFlattened =
					element.objectField( prefix + "objectFlattened", ObjectStructure.FLATTENED );
			createString( "string", objectFlattened );
			if ( addAnotherLevel ) {
				createObjects( "level" + ( level ), objectFlattened, level + 1, false );
			}
			objectFlattened.toReference();
		}

		private IndexFieldReference<String> createString(String name, IndexSchemaObjectField objectField) {
			return objectField.field( name, f -> f.asString()
					.highlightable( Collections.singleton( Highlightable.ANY ) )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			).toReference();
		}
	}

	private static class NotMatchingTypeIndexBinding {
		final IndexFieldReference<Integer> stringField;
		final IndexObjectFieldReference flattenedField;
		final IndexFieldReference<LocalDate> objectFlattenedString;

		NotMatchingTypeIndexBinding(IndexSchemaElement root) {
			stringField = root.field( "string", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField objectField = root.objectField( "objectFlattened" );
			flattenedField = objectField.toReference();

			objectFlattenedString = objectField.field( "string", f -> f.asLocalDate()
					.projectable( Projectable.YES )
			).toReference();
		}
	}

}
