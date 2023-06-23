/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContextBuilder;
import org.hibernate.search.mapper.pojo.standalone.loading.dsl.SelectionLoadingOptionsStep;

public interface StandalonePojoSelectionLoadingContextBuilder
		extends PojoSelectionLoadingContextBuilder<SelectionLoadingOptionsStep>,
		PojoMassIndexingContextBuilder<SelectionLoadingOptionsStep> {

	@Override
	StandalonePojoLoadingContext build();

}
