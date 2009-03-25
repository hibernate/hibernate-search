//$Id$
package org.hibernate.search.annotations;

/**
 * Whether or not the value is stored in the document
 *
 * @author Emmanuel Bernard
 */
public enum Store {
	/** does not store the value in the index */
	NO,
	/** stores the value in the index */
	YES,
	/** stores the value in the index in a compressed form */
	COMPRESS
}
