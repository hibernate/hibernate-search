/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model.additionalmetadata.building.impl;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContexts;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.building.spi.PojoAdditionalMetadataCollectorValueNode;
import org.hibernate.search.mapper.pojo.model.additionalmetadata.impl.PojoValueAdditionalMetadata;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;

class PojoValueAdditionalMetadataBuilder implements PojoAdditionalMetadataCollectorValueNode {
	private final PojoTypeAdditionalMetadataBuilder rootBuilder;
	private final String propertyName;
	private final ContainerExtractorPath extractorPath;

	private PojoModelPathValueNode inverseSidePath;
	private boolean associationEmbedded = false;
	private Optional<ReindexOnUpdate> reindexOnUpdate = Optional.empty();
	private Set<PojoModelPathValueNode> derivedFrom = Collections.emptySet();
	private Integer decimalScale;

	PojoValueAdditionalMetadataBuilder(PojoTypeAdditionalMetadataBuilder rootBuilder, String propertyName,
			ContainerExtractorPath extractorPath) {
		this.rootBuilder = rootBuilder;
		this.propertyName = propertyName;
		this.extractorPath = extractorPath;
	}

	@Override
	public ContextualFailureCollector getFailureCollector() {
		return rootBuilder.getFailureCollector().withContext(
				PojoEventContexts.fromPath( PojoModelPath.ofValue( propertyName, extractorPath ) )
		);
	}

	@Override
	public void associationInverseSide(PojoModelPathValueNode inverseSidePath) {
		this.inverseSidePath = inverseSidePath;
	}

	@Override
	public void associationEmbedded() {
		this.associationEmbedded = true;
	}

	@Override
	public void reindexOnUpdate(ReindexOnUpdate reindexOnUpdate) {
		this.reindexOnUpdate = Optional.of( reindexOnUpdate );
	}

	@Override
	public void derivedFrom(Set<PojoModelPathValueNode> derivedFrom) {
		this.derivedFrom = derivedFrom;
	}

	@Override
	public void decimalScale(int decimalScale) {
		this.decimalScale = decimalScale;
	}

	PojoValueAdditionalMetadata build() {
		return new PojoValueAdditionalMetadata(
				inverseSidePath, associationEmbedded, reindexOnUpdate, derivedFrom, decimalScale
		);
	}
}
