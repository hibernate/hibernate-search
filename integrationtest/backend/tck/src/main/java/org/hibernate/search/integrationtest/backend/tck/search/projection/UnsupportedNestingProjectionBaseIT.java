/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class UnsupportedNestingProjectionBaseIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
			.name( "my-index" );

	@Before
	public void setup() {
		setupHelper.start().withIndexes( index ).setup();
		initData();
	}

	@Test
	public void id() {
		assertThatThrownBy( () -> index.createScope().query()
				.select( f -> f.object( "nested" ).from(
								f.id()
						).asList().multi()
				)
				.where( f -> f.matchAll() )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"'projection:id' cannot be nested in an object projection",
						"An ID projection represents the document ID and adding it as a part of the nested object projection might produce misleading results since it is always a root document ID and not a nested object ID."

				);
	}

	@Test
	public void entity() {
		assertThatThrownBy( () -> index.createScope().query()
				.select( f -> f.object( "nested" ).from(
								f.entity()
						).asList().multi()
				)
				.where( f -> f.matchAll() )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"'projection:entity' cannot be nested in an object projection",
						"An entity projection represents a root entity and adding it as a part of the nested object projection might produce misleading results."

				);
	}

	@Test
	public void entityReference() {
		assertThatThrownBy( () -> index.createScope().query()
				.select( f -> f.object( "nested" ).from(
								f.entityReference()
						).asList().multi()
				)
				.where( f -> f.matchAll() )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"'projection:entity-reference' cannot be nested in an object projection",
						"An entity reference projection represents a root entity and adding it as a part of the nested object projection might produce misleading results."

				);
	}

	@Test
	public void documentReference() {
		assertThatThrownBy( () -> index.createScope().query()
				.select( f -> f.object( "nested" ).from(
								f.documentReference()
						).asList().multi()
				)
				.where( f -> f.matchAll() )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"'projection:document-reference' cannot be nested in an object projection",
						"A document reference projection represents a root document and adding it as a part of the nested object projection might produce misleading results."

				);
	}

	@Test
	public void score() {
		assertThatThrownBy( () -> index.createScope().query()
				.select( f -> f.object( "nested" ).from(
								f.score()
						).asList().multi()
				)
				.where( f -> f.matchAll() )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"'projection:score' cannot be nested in an object projection",
						"A score projection provides the score for the entire hit and adding it as a part of the nested object projection might produce misleading results."

				);
	}

	private void initData() {
		index.bulkIndexer()
				.add( 5, i -> documentProvider(
						"document_" + i,
						document -> {
							document.addValue( index.binding().name, "string_" + i );
							document.addObject( index.binding().nested )
									.addValue( index.binding().nestedString, "string_" + i );
							document.addObject( index.binding().nested )
									.addValue( index.binding().nestedString, "string_" + i );
							document.addObject( index.binding().nested )
									.addValue( index.binding().nestedString, "string_" + i );
						}
				) )
				.join();
	}

	private static class IndexBinding {
		private final IndexFieldReference<String> name;
		private final IndexObjectFieldReference nested;
		private final IndexFieldReference<String> nestedString;

		IndexBinding(IndexSchemaElement root) {
			// this field is irrelevant for the test
			name = root.field( "name", f -> f.asString() ).toReference();
			// these fields are also irrelevant for the test, we only need them so that there's a nested context we can
			// create a projection in:
			IndexSchemaObjectField nested = root.objectField( "nested", ObjectStructure.NESTED ).multiValued();
			this.nestedString = nested.field( "string", f -> f.asString() ).toReference();
			this.nested = nested.toReference();
		}
	}
}
