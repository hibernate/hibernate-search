package org.hibernate.search.mapper.orm.spi;

import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;

public interface BatchSessionContext {

	PojoIndexer createIndexer();

}
