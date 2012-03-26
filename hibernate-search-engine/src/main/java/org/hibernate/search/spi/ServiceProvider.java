package org.hibernate.search.spi;

import java.util.Properties;

/**
 * Control the life cycle of a service attached and used by Hibernate Search
 *
 * It allows to:
 *  - start the service
 *  - stop the service
 *  - declare the key the service is exposed to
 * TODO should it be the implementation name itself?
 *  - provide access tot he service
 *
 * @author Emmanuel Bernard
 */
public interface ServiceProvider<T> {
	void start(Properties properties, BuildContext context);
	T getService();
	void stop();
}
