/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.projection.definition.spi;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
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
	@SuppressWarnings("rawtypes")
	private static final BeanHolder<? extends ConstantProjectionDefinition> EMPTY_SET_INSTANCE =
			BeanHolder.of( new ConstantProjectionDefinition<Set>( Collections.emptySet() ) );
	@SuppressWarnings("rawtypes")
	private static final BeanHolder<? extends ConstantProjectionDefinition> EMPTY_SORTED_SET_INSTANCE =
			BeanHolder.of( new ConstantProjectionDefinition<SortedSet>( Collections.emptySortedSet() ) );
	@SuppressWarnings("rawtypes")
	private static final BeanHolder<? extends ConstantProjectionDefinition> OPTIONAL_EMPTY_INSTANCE =
			BeanHolder.of( new ConstantProjectionDefinition<Optional>( Optional.empty() ) );

	@SuppressWarnings("unchecked") // NULL_VALUE_INSTANCE works for any T
	public static <T> BeanHolder<ConstantProjectionDefinition<T>> nullValue() {
		return (BeanHolder<ConstantProjectionDefinition<T>>) NULL_VALUE_INSTANCE;
	}

	/**
	 * @deprecated Use {@link #empty(ProjectionCollector.Provider)} instead.
	 */
	@Deprecated(since = "8.0")
	@SuppressWarnings("unchecked") // EMPTY_LIST_INSTANCE works for any T
	public static <T> BeanHolder<ConstantProjectionDefinition<List<T>>> emptyList() {
		return (BeanHolder<ConstantProjectionDefinition<List<T>>>) EMPTY_LIST_INSTANCE;
	}

	@SuppressWarnings("unchecked") // empty collections works for any T
	public static <T> BeanHolder<ConstantProjectionDefinition<T>> empty(ProjectionCollector.Provider<?, T> collector) {
		T empty = collector.get().empty();

		if ( ProjectionCollector.nullable().equals( collector ) ) {
			return nullValue();
		}
		if ( ProjectionCollector.optional().equals( collector ) ) {
			return (BeanHolder<ConstantProjectionDefinition<T>>) OPTIONAL_EMPTY_INSTANCE;
		}
		if ( ProjectionCollector.list().equals( collector ) ) {
			return (BeanHolder<ConstantProjectionDefinition<T>>) EMPTY_LIST_INSTANCE;
		}
		if ( ProjectionCollector.set().equals( collector ) ) {
			return (BeanHolder<ConstantProjectionDefinition<T>>) EMPTY_SET_INSTANCE;
		}
		if ( empty instanceof SortedSet ) {
			return (BeanHolder<ConstantProjectionDefinition<T>>) EMPTY_SORTED_SET_INSTANCE;
		}

		return BeanHolder.of( new ConstantProjectionDefinition<>( empty ) );
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
	public SearchProjection<T> create(TypedSearchProjectionFactory<?, ?, ?> factory,
			ProjectionDefinitionContext context) {
		return factory.constant( value ).toProjection();
	}
}
