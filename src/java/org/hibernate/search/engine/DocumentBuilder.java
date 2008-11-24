// $Id$
package org.hibernate.search.engine;

import org.hibernate.search.ProjectionConstants;

/**
 * Interface created to keep backwards compatibility.
 *
 * @deprecated As of release 3.1.0, replaced by {@link org.hibernate.search.ProjectionConstants}
 * @author Hardy Ferentschik
 */
public interface DocumentBuilder {

	/**
	 * Lucene document field name containing the fully qualified classname of the indexed class.
	 *
	 * @deprecated As of release 3.1.0, replaced by {@link org.hibernate.search.ProjectionConstants#OBJECT_CLASS}
	 */
	@Deprecated
	String CLASS_FIELDNAME = ProjectionConstants.OBJECT_CLASS;
}
