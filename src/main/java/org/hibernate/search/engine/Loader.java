//$Id$
package org.hibernate.search.engine;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.search.engine.EntityInfo;

/**
 * Interface defining a set of operations in order to load entities which matched a query. Depending on the type of
 * indexed entities and the type of query different strategies can be used.
 *
 *
 * @author Emmanuel Bernard
 */
public interface Loader {
	void init(Session session, SearchFactoryImplementor searchFactoryImplementor);

	Object load(EntityInfo entityInfo);

	List load(EntityInfo... entityInfos);
}
