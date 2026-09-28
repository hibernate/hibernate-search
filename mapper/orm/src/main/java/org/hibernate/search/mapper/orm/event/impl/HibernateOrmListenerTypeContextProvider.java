package org.hibernate.search.mapper.orm.event.impl;

import org.hibernate.search.util.common.data.spi.KeyValueProvider;

public interface HibernateOrmListenerTypeContextProvider {

	KeyValueProvider<String, ? extends HibernateOrmListenerTypeContext> byHibernateOrmEntityName();

}
