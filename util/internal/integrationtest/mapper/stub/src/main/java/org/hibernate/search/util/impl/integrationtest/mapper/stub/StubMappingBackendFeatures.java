package org.hibernate.search.util.impl.integrationtest.mapper.stub;

public interface StubMappingBackendFeatures {

	default boolean supportsExplicitRefresh() {
		return true;
	}

}
