package org.hibernate.search.engine.environment.bean.spi;

import org.hibernate.search.engine.environment.bean.BeanResolver;

public interface BeanCreationContext {

	BeanResolver beanResolver();

}
