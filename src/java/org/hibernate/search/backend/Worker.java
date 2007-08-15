//$Id$
package org.hibernate.search.backend;

import java.util.Properties;
import java.io.Serializable;

import org.hibernate.event.EventSource;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Perform work for a given session. This implementation has to be multi threaded
 * @author Emmanuel Bernard
 */
public interface Worker {
	void performWork(Object entity, Serializable id, WorkType workType, EventSource session);

	void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor);
}
