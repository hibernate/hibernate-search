/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinitionContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.KeywordStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PredicateDefinitionContextIT {

	private static final String DOCUMENT_1 = "document1";

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();

		BulkIndexer indexer = index.bulkIndexer();
		indexer.add( DOCUMENT_1, document -> {} );
		indexer.join();
	}

	@Test
	void param() {
		Object[] givenParams = new Object[] { "string", new Object(), 5L, Optional.empty() };
		Object[] receivedParams = new Object[givenParams.length];

		assertThatQuery( index.query()
				.where( f -> f.named( "stub-predicate" )
						.param( "param1", givenParams[0] )
						.param( "param2", givenParams[1] )
						.param( "param3", givenParams[2] )
						.param( "param4", givenParams[3] )
						.param( "impl", (PredicateDefinition) context -> {
							receivedParams[0] = context.param( "param1", String.class );
							receivedParams[1] = context.param( "param2", Object.class );
							receivedParams[2] = context.param( "param3", Long.class );
							receivedParams[3] = context.paramOptional( "param5", Optional.class );
							return context.predicate().matchAll().toPredicate();
						} ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThat( receivedParams ).containsExactly( givenParams );
	}

	@Test
	void param_absent() {
		Object[] expectedParams = new Object[] { Optional.empty() };
		Object[] actualParams = new Object[1];

		assertThatQuery( index.query()
				.where( f -> f.named( "stub-predicate" )
						.param( "impl", (PredicateDefinition) context -> {
							actualParams[0] = context.paramOptional( "absent", Optional.class );
							return context.predicate().matchAll().toPredicate();
						} ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );

		assertThat( actualParams ).containsExactly( expectedParams );
	}

	@Test
	void param_nullName() {
		assertThatThrownBy( () -> index.createScope().predicate().named( "stub-predicate" )
				.param( "impl", (PredicateDefinition) context -> {
					context.param( null, String.class );
					return context.predicate().matchAll().toPredicate();
				} )
				.toPredicate() )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'name' must not be null" );
	}

	@Test
	void missingParam() {
		assertThatThrownBy( () -> index.createScope().predicate().named( "stub-predicate" )
				.param( "impl", (PredicateDefinition) context -> {
					context.param( "missing", String.class );
					return context.predicate().matchAll().toPredicate();
				} )
				.toPredicate() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Param with name 'missing' has not been defined for the named predicate 'stub-predicate'." );
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> field1;
		final ObjectFieldBinding nested;
		final ObjectFieldBinding flattened;

		IndexBinding(IndexSchemaElement root) {
			field1 = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( root, "field1" );
			root.namedPredicate( "stub-predicate", new StubPredicateDefinition() );

			nested = ObjectFieldBinding.create( root, "nested", ObjectStructure.NESTED );
			flattened = ObjectFieldBinding.create( root, "flattened", ObjectStructure.FLATTENED );
		}
	}

	static class ObjectFieldBinding {
		final SimpleFieldModel<String> field1;
		final IndexObjectFieldReference reference;

		static ObjectFieldBinding create(IndexSchemaElement parent, String relativeFieldName,
				ObjectStructure structure) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure ).multiValued();
			return new ObjectFieldBinding( objectField );
		}

		ObjectFieldBinding(IndexSchemaObjectField objectField) {
			reference = objectField.toReference();
			field1 = SimpleFieldModel.mapper( KeywordStringFieldTypeDescriptor.INSTANCE )
					.map( objectField, "field1" );
			objectField.namedPredicate( "stub-predicate", new StubPredicateDefinition() );
		}
	}

	public static class StubPredicateDefinition implements PredicateDefinition {
		@Override
		public SearchPredicate create(PredicateDefinitionContext context) {
			PredicateDefinition impl = context.param( "impl", PredicateDefinition.class );
			return impl.create( context );
		}
	}

}
