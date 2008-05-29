//$Id$
package org.hibernate.search.engine;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Emmanuel Bernard
 */
//TODO Move to egine?
public class EntityInfo {
	public Class clazz;
	public Serializable id;
	public Object[] projection;
	public List<Integer> indexesOfThis;
}
