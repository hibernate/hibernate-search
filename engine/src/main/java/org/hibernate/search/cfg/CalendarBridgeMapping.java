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
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.Resolution;

public class CalendarBridgeMapping {

	private final SearchMapping mapping;
	private final Map<String, Object> resolution;
	private EntityDescriptor entity;
	private PropertyDescriptor property;

	public CalendarBridgeMapping(SearchMapping mapping,EntityDescriptor entity,PropertyDescriptor property, Resolution resolution) {
		if ( resolution == null ) {
			throw new SearchException( "Resolution required in order to index calendar property" );
		}
		this.mapping = mapping;
		this.resolution = new HashMap<String, Object>();
		this.entity = entity;
		this.property = property;
		this.resolution.put( "resolution", resolution );
		property.setCalendarBridge( this.resolution );
	}

	public FieldMapping field() {
		return new FieldMapping( property, entity, mapping );
	}

	public PropertyMapping property(String name, ElementType type) {
		return new PropertyMapping( name, type, entity, mapping );
	}

	public AnalyzerDefMapping analyzerDef(String name, Class<? extends TokenizerFactory> tokenizerFactory) {
		return new AnalyzerDefMapping( name, tokenizerFactory, mapping );
	}

	public EntityMapping entity(Class<?> entityType) {
		return new EntityMapping( entityType, mapping );
	}

}
