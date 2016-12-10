/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.metadata.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Field;
import org.hibernate.search.bridge.spi.FieldType;

/**
 * A field defined by a bridge.
 *
 * @author Gunnar Morling
 */
public class BridgeDefinedField {

	private final BackReference<DocumentFieldMetadata> sourceField;

	/*
	 * Here we cannot use DocumentFieldPath because metadata-providing field bridges only provide the absolute name,
	 * without telling the prefix from the relative name.
	 */
	private final String absoluteName;
	private final FieldType type;

	/*
	 * Backends have specific field properties (Elastic search dynamic mapping for example). this map will group the
	 * properties per backend.
	 */
	private Map<Class<?>, Object> extra = new HashMap<>();

	public BridgeDefinedField(BridgeDefinedField bridgeDefinedField) {
		this.sourceField = bridgeDefinedField.sourceField;
		this.absoluteName = bridgeDefinedField.absoluteName;
		this.type = bridgeDefinedField.type;
	}

	public BridgeDefinedField(BackReference<DocumentFieldMetadata> sourceField, String absoluteName, FieldType type) {
		this.sourceField = sourceField;
		this.absoluteName = absoluteName;
		this.type = type;
	}

	/**
	 * @return The {@link DocumentFieldMetadata} whose field bridge declared this field.
	 */
	public DocumentFieldMetadata getSourceField() {
		return sourceField.get();
	}

	/**
	 * @return The full name of this field, including any indexed-embedded prefix.
	 */
	public String getAbsoluteName() {
		return absoluteName;
	}

	public Field.Index getIndex() {
		return getSourceField().getIndex();
	}

	public FieldType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "BridgeDefinedField [name=" + absoluteName + ", type=" + type + "]";
	}

	/**
	 * Adds the object that containing the field properties specific for a backend,
	 *
	 * @param backendBridgeDefineFieldClass the type of the object containing the properties
	 * @param backendBridgeDefineField the object that stores the properties
	 */
	public <T> void add(Class<T> backendBridgeDefineFieldClass, T backendBridgeDefineField) {
		extra.put( backendBridgeDefineFieldClass, backendBridgeDefineField );
	}

	/**
	 * Returns the object containing the properties of the field for the specific class.
	 *
	 * @param backendBridgeDefineFieldClass the type of the object containing the properties of the field
	 * @return the object containing the properties of the field
	 */
	public <T> T mappedOn(Class<T> backendBridgeDefineFieldClass) {
		return backendBridgeDefineFieldClass.cast( extra.get( backendBridgeDefineFieldClass ) );
	}
}
