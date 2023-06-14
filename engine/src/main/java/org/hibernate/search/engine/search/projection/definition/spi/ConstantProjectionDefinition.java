/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.definition.spi;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.spi.ToStringTreeAppender;

@Incubating
public final class ConstantProjectionDefinition<T> extends AbstractProjectionDefinition<T> {
	@SuppressWarnings("rawtypes")
	private static final BeanHolder<? extends ConstantProjectionDefinition> NULL_VALUE_INSTANCE =
			BeanHolder.of( new ConstantProjectionDefinition<Void>( null ) );
	@SuppressWarnings("rawtypes")
	private static final BeanHolder<? extends ConstantProjectionDefinition> EMPTY_LIST_INSTANCE =
			BeanHolder.of( new ConstantProjectionDefinition<List>( Collections.emptyList() ) );

	@SuppressWarnings("unchecked") // NULL_VALUE_INSTANCE works for any T
	public static <T> BeanHolder<ConstantProjectionDefinition<T>> nullValue() {
		return (BeanHolder<ConstantProjectionDefinition<T>>) NULL_VALUE_INSTANCE;
	}

	@SuppressWarnings("unchecked") // EMPTY_LIST_INSTANCE works for any T
	public static <T> BeanHolder<ConstantProjectionDefinition<List<T>>> emptyList() {
		return (BeanHolder<ConstantProjectionDefinition<List<T>>>) EMPTY_LIST_INSTANCE;
	}

	private final T value;

	private ConstantProjectionDefinition(T value) {
		this.value = value;
	}

	@Override
	protected String type() {
		return "constant";
	}

	@Override
	public void appendTo(ToStringTreeAppender appender) {
		super.appendTo( appender );
		appender.attribute( "value", value );
	}

	@Override
	public SearchProjection<T> create(SearchProjectionFactory<?, ?> factory,
			ProjectionDefinitionContext context) {
		return factory.constant( value ).toProjection();
	}
}
