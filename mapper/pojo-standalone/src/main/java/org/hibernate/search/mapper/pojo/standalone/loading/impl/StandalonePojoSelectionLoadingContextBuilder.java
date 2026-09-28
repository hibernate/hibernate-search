package org.hibernate.search.mapper.pojo.standalone.loading.impl;

import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.standalone.loading.dsl.SelectionLoadingOptionsStep;

public interface StandalonePojoSelectionLoadingContextBuilder
		extends PojoSelectionLoadingContextBuilder<SelectionLoadingOptionsStep> {

	@Override
	StandalonePojoLoadingContext build();

}
