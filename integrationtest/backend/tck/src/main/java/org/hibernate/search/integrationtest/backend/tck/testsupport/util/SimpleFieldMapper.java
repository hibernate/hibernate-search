/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;

public final class SimpleFieldMapper<F, S extends SearchableProjectableIndexFieldTypeOptionsStep<?, F>, M> {

	public static <F, S extends SearchableProjectableIndexFieldTypeOptionsStep<?, F>, M> SimpleFieldMapper<F, S, M> of(
			Function<IndexFieldTypeFactory, S> initialConfiguration,
			BiFunction<IndexFieldReference<F>, String, M> resultFunction) {
		return of( initialConfiguration, ignored -> {}, resultFunction );
	}

	public static <
			F,
			S extends SearchableProjectableIndexFieldTypeOptionsStep<?, F>> SimpleFieldMapper<F, S, IndexFieldReference<F>> of(
					Function<IndexFieldTypeFactory, S> initialConfiguration,
					Consumer<? super S> configurationAdjustment) {
		return of( initialConfiguration, configurationAdjustment, (reference, ignored) -> reference );
	}

	public static <F, S extends SearchableProjectableIndexFieldTypeOptionsStep<?, F>, M> SimpleFieldMapper<F, S, M> of(
			Function<IndexFieldTypeFactory, S> initialConfiguration,
			Consumer<? super S> configurationAdjustment,
			BiFunction<IndexFieldReference<F>, String, M> resultFunction) {
		return new SimpleFieldMapper<>(
				initialConfiguration, configurationAdjustment, resultFunction
		);
	}

	private final Function<IndexFieldTypeFactory, S> initialConfiguration;
	private final Consumer<? super S> configurationAdjustment;
	private final BiFunction<IndexFieldReference<F>, String, M> resultFunction;

	private SimpleFieldMapper(
			Function<IndexFieldTypeFactory, S> initialConfiguration,
			Consumer<? super S> configurationAdjustment,
			BiFunction<IndexFieldReference<F>, String, M> resultFunction) {
		this.initialConfiguration = initialConfiguration;
		this.configurationAdjustment = configurationAdjustment;
		this.resultFunction = resultFunction;
	}

	public M map(IndexSchemaElement parent, String name) {
		return map( parent, name, ignored -> {} );
	}

	public M mapMultiValued(IndexSchemaElement parent, String name) {
		return mapMultiValued( parent, name, ignored -> {} );
	}

	@SafeVarargs
	public final M map(IndexSchemaElement parent, String name,
			Consumer<? super S>... additionalConfigurations) {
		return map( parent, name, false, additionalConfigurations );
	}

	@SafeVarargs
	public final M mapMultiValued(IndexSchemaElement parent, String name,
			Consumer<? super S>... additionalConfigurations) {
		return map( parent, name, true, additionalConfigurations );
	}

	// Note: this needs to be final even if it's private; otherwise javac will raise an error when using Java 8.
	@SafeVarargs
	private final M map(IndexSchemaElement parent, String name, boolean multiValued,
			Consumer<? super S>... additionalConfigurations) {
		IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> fieldContext = parent
				.field(
						name,
						f -> {
							S typeContext = initialConfiguration.apply( f );
							configurationAdjustment.accept( typeContext );
							for ( Consumer<? super S> additionalConfiguration : additionalConfigurations ) {
								additionalConfiguration.accept( typeContext );
							}
							return typeContext;
						}
				);
		if ( multiValued ) {
			fieldContext.multiValued();
		}
		IndexFieldReference<F> reference = fieldContext.toReference();
		return resultFunction.apply( reference, name );
	}

}
