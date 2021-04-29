/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading.impl;

import org.hibernate.search.mapper.javabean.loading.dsl.SelectionLoadingOptionsStep;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContextBuilder;

public interface JavaBeanSelectionLoadingContextBuilder extends PojoSelectionLoadingContextBuilder<SelectionLoadingOptionsStep>,
		PojoMassIndexingContextBuilder<SelectionLoadingOptionsStep> {

	@Override
	JavaBeanLoadingContext build();

}
