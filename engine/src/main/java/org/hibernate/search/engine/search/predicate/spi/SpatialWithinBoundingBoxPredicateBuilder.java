package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.spatial.GeoBoundingBox;

public interface SpatialWithinBoundingBoxPredicateBuilder extends SearchPredicateBuilder {

	void boundingBox(GeoBoundingBox boundingBox);

}
