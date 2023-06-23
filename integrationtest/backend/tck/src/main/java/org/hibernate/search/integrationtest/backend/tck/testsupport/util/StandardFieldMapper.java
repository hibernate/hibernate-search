/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldOptionsStep;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;

public final class StandardFieldMapper<F, M> {

	public static <F, M> StandardFieldMapper<F, M> of(
			Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> initialConfiguration,
			BiFunction<IndexFieldReference<F>, String, M> resultFunction) {
		return of( initialConfiguration, ignored -> {}, resultFunction );
	}

	public static <F> StandardFieldMapper<F, IndexFieldReference<F>> of(
			Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> initialConfiguration,
			Consumer<? super StandardIndexFieldTypeOptionsStep<?, F>> configurationAdjustment) {
		return of( initialConfiguration, configurationAdjustment, (reference, ignored) -> reference );
	}

	public static <F, M> StandardFieldMapper<F, M> of(
			Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> initialConfiguration,
			Consumer<? super StandardIndexFieldTypeOptionsStep<?, F>> configurationAdjustment,
			BiFunction<IndexFieldReference<F>, String, M> resultFunction) {
		return new StandardFieldMapper<>(
				initialConfiguration, configurationAdjustment, resultFunction
		);
	}

	private final Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> initialConfiguration;
	private final Consumer<? super StandardIndexFieldTypeOptionsStep<?, F>> configurationAdjustment;
	private final BiFunction<IndexFieldReference<F>, String, M> resultFunction;

	private StandardFieldMapper(
			Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> initialConfiguration,
			Consumer<? super StandardIndexFieldTypeOptionsStep<?, F>> configurationAdjustment,
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
			Consumer<? super StandardIndexFieldTypeOptionsStep<?, F>>... additionalConfigurations) {
		return map( parent, name, false, additionalConfigurations );
	}

	@SafeVarargs
	public final M mapMultiValued(IndexSchemaElement parent, String name,
			Consumer<? super StandardIndexFieldTypeOptionsStep<?, F>>... additionalConfigurations) {
		return map( parent, name, true, additionalConfigurations );
	}

	// Note: this needs to be final even if it's private; otherwise javac will raise an error when using Java 8.
	@SafeVarargs
	private final M map(IndexSchemaElement parent, String name, boolean multiValued,
			Consumer<? super StandardIndexFieldTypeOptionsStep<?, F>>... additionalConfigurations) {
		IndexSchemaFieldOptionsStep<?, IndexFieldReference<F>> fieldContext = parent
				.field(
						name,
						f -> {
							StandardIndexFieldTypeOptionsStep<?, F> typeContext = initialConfiguration.apply( f );
							configurationAdjustment.accept( typeContext );
							for ( Consumer<? super StandardIndexFieldTypeOptionsStep<?,
									F>> additionalConfiguration : additionalConfigurations ) {
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
