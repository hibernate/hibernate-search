/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.spi;

import java.util.function.Function;

import org.hibernate.search.engine.search.common.NamedValues;
import org.hibernate.search.engine.search.sort.dsl.SortFinalStep;

public interface WithParametersSortBuilder extends SearchSortBuilder {

	void creator(Function<? super NamedValues, ? extends SortFinalStep> sortCreator);
}
