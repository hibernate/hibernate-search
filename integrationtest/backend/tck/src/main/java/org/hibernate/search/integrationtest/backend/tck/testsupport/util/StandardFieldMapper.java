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

import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;

public final class StandardFieldMapper<F, M> {

	public static <F, M> StandardFieldMapper<F, M> of(
			Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> initialConfiguration,
			BiFunction<IndexFieldAccessor<F>, String, M> resultFunction) {
		return of( initialConfiguration, ignored -> { }, resultFunction );
	}

	public static <F> StandardFieldMapper<F, IndexFieldAccessor<F>> of(
			Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> initialConfiguration,
			Consumer<? super StandardIndexFieldTypeContext<?, F>> configurationAdjustment) {
		return of( initialConfiguration, configurationAdjustment, (accessor, ignored) -> accessor );
	}

	public static <F, M> StandardFieldMapper<F, M> of(
			Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> initialConfiguration,
			Consumer<? super StandardIndexFieldTypeContext<?, F>> configurationAdjustment,
			BiFunction<IndexFieldAccessor<F>, String, M> resultFunction) {
		return new StandardFieldMapper<>(
				initialConfiguration, configurationAdjustment, resultFunction
		);
	}

	private final Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> initialConfiguration;
	private final Consumer<? super StandardIndexFieldTypeContext<?, F>> configurationAdjustment;
	private final BiFunction<IndexFieldAccessor<F>, String, M> resultFunction;

	private StandardFieldMapper(
			Function<IndexFieldTypeFactoryContext, StandardIndexFieldTypeContext<?, F>> initialConfiguration,
			Consumer<? super StandardIndexFieldTypeContext<?, F>> configurationAdjustment,
			BiFunction<IndexFieldAccessor<F>, String, M> resultFunction) {
		this.initialConfiguration = initialConfiguration;
		this.configurationAdjustment = configurationAdjustment;
		this.resultFunction = resultFunction;
	}

	public M map(IndexSchemaElement parent, String name) {
		return map( parent, name, ignored -> { } );
	}

	@SafeVarargs
	public final M map(IndexSchemaElement parent, String name,
			Consumer<? super StandardIndexFieldTypeContext<?, F>>... additionalConfigurations) {
		IndexFieldAccessor<F> accessor = parent.field(
				name,
				f -> {
					StandardIndexFieldTypeContext<?, F> context = initialConfiguration.apply( f );
					configurationAdjustment.accept( context );
					for ( Consumer<? super StandardIndexFieldTypeContext<?, F>> additionalConfiguration : additionalConfigurations ) {
						additionalConfiguration.accept( context );
					}
					return context.toIndexFieldType();
				}
		)
				.createAccessor();
		return resultFunction.apply( accessor, name );
	}

}
