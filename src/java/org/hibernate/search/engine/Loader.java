//$Id$
package org.hibernate.search.engine;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.search.engine.EntityInfo;

/**
 * @author Emmanuel Bernard
 */
public interface Loader {
	void init(Session session, SearchFactoryImplementor searchFactoryImplementor);
	Object load(EntityInfo entityInfo);
	List load(EntityInfo... entityInfos);
}
