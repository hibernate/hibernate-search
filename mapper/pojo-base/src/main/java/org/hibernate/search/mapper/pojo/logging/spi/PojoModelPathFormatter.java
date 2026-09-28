package org.hibernate.search.mapper.pojo.logging.spi;

import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;

public final class PojoModelPathFormatter {

	private final PojoModelPath pojoModelPath;

	public PojoModelPathFormatter(PojoModelPath pojoModelPath) {
		this.pojoModelPath = pojoModelPath;
	}

	@Override
	public String toString() {
		return pojoModelPath.toPathString();
	}
}
