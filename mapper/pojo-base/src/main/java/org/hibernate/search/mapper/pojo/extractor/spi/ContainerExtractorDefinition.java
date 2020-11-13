/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.extractor.spi;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractor;

@SuppressWarnings("rawtypes") // We need to allow raw container types, e.g. MapValueExtractor.class
public final class ContainerExtractorDefinition<C extends ContainerExtractor> {

	private final Class<C> type;
	private final BeanReference<? extends C> reference;

	ContainerExtractorDefinition(Class<C> type, BeanReference<? extends C> reference) {
		this.type = type;
		this.reference = reference;
	}

	public Class<C> type() {
		return type;
	}

	public BeanReference<? extends C> reference() {
		return reference;
	}
}
