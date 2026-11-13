package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.types.format.impl.ElasticsearchDefaultFieldFormatProvider;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.backend.types.Scorable;
import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.gson.Gson;

class ElasticsearchScorableFieldTypeOptionsStepTest {

	@ParameterizedTest
	@EnumSource(Scorable.class)
	void booleanScorableDoesNotEnableDocValues(Scorable scorable) {
		var step = new ElasticsearchBooleanIndexFieldTypeOptionsStep( buildContext() );
		if ( scorable == Scorable.YES ) {
			assertThatThrownBy( () -> step.scorable( scorable ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "HSEARCH400697", "Scorable.YES is not supported", "Only numeric fields" );
		}
		else {
			step.scorable( scorable );
			assertMappingAndCapability( (ElasticsearchIndexValueFieldType<?>) step.toIndexFieldType(), false );
		}
	}

	@ParameterizedTest
	@EnumSource(Scorable.class)
	void dateScorableDoesNotEnableDocValues(Scorable scorable) {
		var step = new ElasticsearchLocalDateIndexFieldTypeOptionsStep( buildContext() );
		if ( scorable == Scorable.YES ) {
			assertThatThrownBy( () -> step.scorable( scorable ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "HSEARCH400697", "Scorable.YES is not supported", "Only numeric fields" );
		}
		else {
			step.scorable( scorable );
			assertMappingAndCapability( (ElasticsearchIndexValueFieldType<?>) step.toIndexFieldType(), false );
		}
	}

	@ParameterizedTest
	@EnumSource(Scorable.class)
	void numericScorableEnablesDocValuesAndCapability(Scorable scorable) {
		var step = new ElasticsearchIntegerIndexFieldTypeOptionsStep( buildContext() );
		step.scorable( scorable );
		assertMappingAndCapability( (ElasticsearchIndexValueFieldType<?>) step.toIndexFieldType(), scorable == Scorable.YES );
	}

	private static void assertMappingAndCapability(ElasticsearchIndexValueFieldType<?> type, boolean scorable) {
		assertThat( new Gson().toJsonTree( type.mapping() ).getAsJsonObject().get( "doc_values" ).getAsBoolean() )
				.isEqualTo( scorable );
		assertThat( type.scorable() ).isEqualTo( scorable );
		if ( scorable ) {
			assertThat( type.traits() ).contains( IndexFieldTraits.ScoreFunctions.FIELD_VALUE_FACTOR );
		}
		else {
			assertThat( type.traits() ).doesNotContain( IndexFieldTraits.ScoreFunctions.FIELD_VALUE_FACTOR );
		}
	}

	private static ElasticsearchIndexFieldTypeBuildContext buildContext() {
		var context = mock( ElasticsearchIndexFieldTypeBuildContext.class );
		when( context.getUserFacingGson() ).thenReturn( new Gson() );
		var formats = mock( ElasticsearchDefaultFieldFormatProvider.class );
		when( formats.getDefaultMappingFormat( LocalDate.class ) ).thenReturn( List.of( "strict_date" ) );
		when( formats.getDefaultDateTimeFormatter( LocalDate.class ) ).thenReturn( DateTimeFormatter.ISO_LOCAL_DATE );
		when( context.getDefaultFieldFormatProvider() ).thenReturn( formats );
		return context;
	}
}
