package org.hibernate.search.mapper.pojo.standalone.mapping;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface CloseableSearchMapping extends SearchMapping, AutoCloseable {

	@Override
	void close();
}
