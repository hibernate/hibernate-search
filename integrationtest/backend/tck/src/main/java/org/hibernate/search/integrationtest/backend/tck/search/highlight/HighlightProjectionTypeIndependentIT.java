/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.assertj.core.api.AssertionsForClassTypes;

public class HighlightProjectionTypeIndependentIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );
	private static final SimpleMappedIndex<NestedIndexBinding> nestedIndex =
			SimpleMappedIndex.of( NestedIndexBinding::new ).name( "nestedIndex" );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).withIndex( nestedIndex ).setup();
	}

	@Test
	public void unknownField() {
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
	public void objectField_nested() {
		String fieldPath = index.binding().nestedObject.relativeFieldName;
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query().select( f -> f.highlight( fieldPath ) ).where( f -> f.matchAll() ).toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'projection:highlight' on field '" + fieldPath + "'" );
	}

	@Test
	public void objectField_flattened() {
		String fieldPath = index.binding().flattenedObject.relativeFieldName;
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query().select( f -> f.highlight( fieldPath ) ).where( f -> f.matchAll() ).toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Cannot use 'projection:highlight' on field '" + fieldPath + "'" );
	}

	@Test
	public void highlighterNullName() {
		AssertionsForClassTypes.assertThatThrownBy(
				() -> index.createScope().query().select(
						f -> f.highlight( "string1" )
				).where( f -> f.matchAll() )
						.highlighter( null, h -> h.plain() )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Named highlighters cannot use a blank string as name."
				);
	}

	@Test
	public void highlighterEmptyName() {
		AssertionsForClassTypes.assertThatThrownBy(
				() -> index.createScope().query().select(
						f -> f.highlight( "string1" )
				).where( f -> f.matchAll() )
						.highlighter( "", h -> h.plain() )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Named highlighters cannot use a blank string as name."
				);
	}

	@Test
	public void highlighterSameName() {
		AssertionsForClassTypes.assertThatThrownBy(
				() -> index.createScope().query().select(
						f -> f.highlight( "string1" )
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
	public void cannotHighlightNestedObjectStructureFields() {
		AssertionsForClassTypes.assertThatThrownBy( () -> nestedIndex.createScope().query().select(
				f -> f.highlight( "nested.nestedString" )
		).where( f -> f.matchAll() )
				.toQuery() ).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'projection:highlight' on field 'nested.nestedString'",
						"The highlight projection cannot be applied to a field from an object using `ObjectStructure.NESTED` structure",
						"Context: field 'nested.nestedString'"
				);
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string1Field;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			string1Field = root.field( "string1", f -> f.asString()
					.highlightable( Collections.singleton( Highlightable.ANY ) )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					.termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS )
			).toReference();

			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectStructure.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectStructure.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			self = objectField.toReference();
		}
	}

	private static class NestedIndexBinding {
		final IndexObjectFieldReference nested;
		final IndexFieldReference<String> nestedString;

		NestedIndexBinding(IndexSchemaElement root) {
			IndexSchemaObjectField objectField = root.objectField( "nested", ObjectStructure.NESTED );
			nested = objectField.toReference();

			nestedString = objectField.field( "nestedString", f -> f.asString()
					.highlightable( Collections.singleton( Highlightable.ANY ) )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			).toReference();
		}
	}

}
