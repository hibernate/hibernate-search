// $Id$
package org.hibernate.search;

/**
 * represents a FullTextFilter that is about to be applied
 * Used to inject parameters
 *
 * @author Emmanuel Bernard
 */
public interface FullTextFilter {
	FullTextFilter setParameter(String name, Object value);
	Object getParameter(String name);
}
