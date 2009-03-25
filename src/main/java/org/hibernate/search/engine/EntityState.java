package org.hibernate.search.engine;

/**
 * Entity state with regard to indexing possibilities
 *
 * @author Emmanuel Bernard
 */
public enum EntityState {
	INDEXED,
	CONTAINED_IN_ONLY,
	NON_INDEXABLE
}
