package org.hibernate.search.bridge;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.util.NumericUtils;

/**
 * The NumericFieldBrigde to index Integer, Double, Float and Long values using
 * lucene's underlying NumericField
 * 
 * author: Gustavo Fernandes
 */
public abstract class NumericFieldBridge implements FieldBridge {

	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		NumericField numericField = luceneOptions.createNumericField(name);
		if (value != null) {
			setValue(numericField, value);
			luceneOptions.addNumericFieldToDocument(numericField, document);
		}
	}

	public abstract void setValue(NumericField numericField, Object value);

	public abstract String toPrefixedCode(Object value);

	public Object get(String name, Document document) {
		NumericField numericField = (NumericField) document.getFieldable(name);
		return numericField.getNumericValue();
	}

}

class LongNumericFieldBrigde extends NumericFieldBridge {

	@Override
	public void setValue(NumericField numericField, Object value) {
		numericField.setLongValue((Long) value);
	}

	@Override
	public String toPrefixedCode(Object value) {
		return NumericUtils.longToPrefixCoded((Long) value);
	}
}

class DoubleNumericFieldBrige extends NumericFieldBridge {

	@Override
	public void setValue(NumericField numericField, Object value) {
		numericField.setDoubleValue((Double) value);
	}

	@Override
	public String toPrefixedCode(Object value) {
		return NumericUtils.doubleToPrefixCoded((Double) value);
	}
}

class IntNumericFieldBridge extends NumericFieldBridge {

	@Override
	public void setValue(NumericField numericField, Object value) {
		numericField.setIntValue((Integer) value);
	}

	@Override
	public String toPrefixedCode(Object value) {
		return NumericUtils.intToPrefixCoded((Integer) value);
	}
}

class FloatNumericFieldBridge extends NumericFieldBridge {

	@Override
	public void setValue(NumericField numericField, Object value) {
		numericField.setFloatValue((Float) value);
	}

	@Override
	public String toPrefixedCode(Object value) {
		return NumericUtils.floatToPrefixCoded((Float) value);
	}
}