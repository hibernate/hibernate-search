/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.cfg;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.search.bridge.FieldBridge;

public class ContainedInMapping {

	private final SearchMapping mapping;
	private final PropertyDescriptor property;
	private final EntityDescriptor entity;

	public ContainedInMapping(SearchMapping mapping,PropertyDescriptor property, EntityDescriptor entity) {
		this.mapping = mapping;
		this.property = property;
		this.entity = entity;
		Map<String, Object> containedIn = new HashMap<String, Object>();
		property.setContainedIn( containedIn );
	}

	public FieldMapping field() {
		return new FieldMapping(property, entity, mapping);
	}

	/**
	 * @deprecated Invoke {@code field().numericField()} instead.
	 */
	@Deprecated
	public NumericFieldMapping numericField() {
		return new NumericFieldMapping( property.getName(), property, entity, mapping );
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping(name, type, entity, mapping);
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping(name, tokenizerFactory, mapping);
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping(entityType, mapping);
	}

	public PropertyMapping bridge(Class<? extends FieldBridge> fieldBridge) {
		return new FieldBridgeDirectMapping( property, entity, mapping, fieldBridge );
	}

}
