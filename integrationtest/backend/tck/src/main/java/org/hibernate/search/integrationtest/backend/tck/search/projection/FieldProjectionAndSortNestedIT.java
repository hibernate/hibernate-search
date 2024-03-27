/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.integrationtest.backend.tck.search.projection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-4584")
class FieldProjectionAndSortNestedIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void before() {
		setupHelper.start().withIndex( index ).setup();
	}

	@Test
	void test() {
		index.index( "1", doc -> {
			doc.addValue( index.binding().year, 1845 );

			DocumentElement author = doc.addObject( index.binding().author );
			author.addValue( index.binding().name, "Edgar Allen Poe" );
		} );
		index.index( "2", doc -> {
			doc.addValue( index.binding().year, 1890 );

			DocumentElement author = doc.addObject( index.binding().author );
			author.addValue( index.binding().name, "Emily Dickinson" );
		} );
		index.index( "3", doc -> {
			doc.addValue( index.binding().year, 1883 );

			DocumentElement author = doc.addObject( index.binding().author );
			author.addValue( index.binding().name, "Emma Lazarus" );
		} );

		StubMappingScope scope = index.createScope();
		SearchQuery<Object> query = scope.query()
				.select( f -> f.field( "author.name" ) )
				.where( f -> f.range().field( "year" ).lessThan( 1885 ) )
				.sort( f -> f.field( "author.name" ) )
				.toQuery();

		List<Object> hits = query.fetchAll().hits();
		assertThat( hits ).isNotEmpty();
	}

	private static class IndexBinding {
		private final IndexFieldReference<Integer> year;

		private final IndexObjectFieldReference author;
		private final IndexFieldReference<String> name;

		IndexBinding(IndexSchemaElement root) {
			year = root.field( "year", f -> f.asInteger()
					.projectable( Projectable.YES ).sortable( Sortable.YES ) ).toReference();

			IndexSchemaObjectField nested = root.objectField( "author", ObjectStructure.NESTED );
			author = nested.toReference();

			name = nested.field( "name", f -> f.asString()
					.projectable( Projectable.YES ).sortable( Sortable.YES ) ).toReference();
		}
	}

}
