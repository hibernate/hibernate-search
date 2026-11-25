package org.hibernate.search.integrationtest.backend.tck.scorable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Scorable;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ScorableFieldTypeIT {
	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@ParameterizedTest
	@ValueSource(strings = { "string", "boolean", "date" })
	void unsupportedScorableRejectedAtBootstrap(String type) {
		assertThatThrownBy( () -> setupHelper.start()
				.withIndex( StubMappedIndex.ofNonRetrievable( root -> defineField( root, type, Scorable.YES ) ) )
				.setup() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Scorable.YES is not supported", "Only numeric fields", "index 'indexName'" );
	}

	@ParameterizedTest
	@CsvSource({ "string, NO", "string, DEFAULT", "boolean, NO", "boolean, DEFAULT", "date, NO", "date, DEFAULT" })
	void unsupportedScorableDisabled(String type, Scorable scorable) {
		setupHelper.start()
				.withIndex( StubMappedIndex.ofNonRetrievable( root -> defineField( root, type, scorable ) ) )
				.setup();
	}

	private static void defineField(IndexSchemaElement root, String type, Scorable scorable) {
		switch ( type ) {
			case "string" -> root.field( "value", f -> f.asString().scorable( scorable ) ).toReference();
			case "boolean" -> root.field( "value", f -> f.asBoolean().scorable( scorable ) ).toReference();
			case "date" -> root.field( "value", f -> f.asLocalDate().scorable( scorable ) ).toReference();
			default -> throw new AssertionFailure( type );
		}
	}
}
