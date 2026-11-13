package org.hibernate.search.util.json.logging.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.json.spi.JsonEmptyInputException;
import org.hibernate.search.util.json.spi.JsonParseException;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@CategorizedLogger(category = JsonLog.CATEGORY_NAME, description = "JSON parsing, validation and output failures.")
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRange(min = MessageConstants.UTIL_JSON_ID_RANGE_MIN, max = MessageConstants.UTIL_JSON_ID_RANGE_MAX)
public interface JsonLog {
	String CATEGORY_NAME = "org.hibernate.search.json";

	JsonLog INSTANCE = LoggerFactory.make( JsonLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	int ID_OFFSET = MessageConstants.UTIL_JSON_ID_RANGE_MIN;

	@Message(id = ID_OFFSET + 0, value = "Invalid JSON parsing constraints")
	IllegalArgumentException invalidParsingConstraints();

	@Message(id = ID_OFFSET + 1, value = "Expected JSON string, number or boolean")
	IllegalArgumentException expectedScalar();

	@Message(id = ID_OFFSET + 2, value = "Expected JSON boolean or boolean string")
	IllegalArgumentException expectedBoolean();

	@Message(id = ID_OFFSET + 3, value = "Expected a singleton JSON array")
	IllegalArgumentException expectedSingletonArray();

	@Message(id = ID_OFFSET + 4, value = "Expected JSON array")
	IllegalArgumentException expectedArray();

	@Message(id = ID_OFFSET + 5, value = "Expected JSON object")
	IllegalArgumentException expectedObject();

	@Message(id = ID_OFFSET + 6, value = "No JSON codec registered for %s")
	IllegalArgumentException unknownCodec(String typeName);

	@Message(id = ID_OFFSET + 7, value = "Negative probe limit")
	IllegalArgumentException negativeProbeLimit();

	@Message(id = ID_OFFSET + 8, value = "Invalid UTF-8 continuation byte")
	JsonParseException invalidUtf8Continuation();

	@Message(id = ID_OFFSET + 9, value = "Invalid UTF-8 leading byte")
	JsonParseException invalidUtf8LeadingByte();

	@Message(id = ID_OFFSET + 10, value = "Truncated UTF-8 input")
	JsonParseException truncatedUtf8Input();

	@Message(id = ID_OFFSET + 11, value = "JSON write cursor is closed")
	IllegalStateException writeCursorClosed();

	@Message(id = ID_OFFSET + 12, value = "Unsupported JSON node: %s")
	IllegalArgumentException unsupportedNode(String typeName);

	@Message(id = ID_OFFSET + 13, value = "Cyclic JSON tree")
	IllegalArgumentException cyclicTree();

	@Message(id = ID_OFFSET + 14, value = "JSON object member name must be a String")
	IllegalArgumentException invalidMemberName();

	@Message(id = ID_OFFSET + 15, value = "JSON container value must be a non-null JsonNode")
	IllegalArgumentException invalidContainerValue();

	@Message(id = ID_OFFSET + 16, value = "Unexpected output frame stage: %s")
	IllegalStateException unexpectedOutputStage(int stage);

	@Message(id = ID_OFFSET + 17, value = "Charset decoder cannot progress within its bounded input buffer")
	JsonParseException charsetDecoderCannotProgress();

	@Message(id = ID_OFFSET + 18, value = "JSON input exceeds byte limit %s")
	JsonParseException inputByteLimitExceeded(long limit);

	@Message(id = ID_OFFSET + 19, value = "Invalid charset input: %s")
	JsonParseException invalidCharsetInput(String decoderResult);

	@Message(id = ID_OFFSET + 20, value = "Cannot transcode JSON input: %s")
	JsonParseException cannotTranscodeInput(String message, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 21, value = "JSON parser session is closed")
	IllegalStateException parserClosed();

	@Message(id = ID_OFFSET + 22, value = "JSON parser made no progress resuming a property-name surrogate pair")
	IllegalStateException parserCannotResumeSurrogatePair();

	@Message(id = ID_OFFSET + 23, value = "JSON body cursor is closed")
	IllegalStateException bodyCursorClosed();

	@Message(id = ID_OFFSET + 24, value = "Non-finite JSON number")
	IllegalArgumentException nonFiniteNumber();

	@Message(id = ID_OFFSET + 25, value = "Expected one JSON root; found trailing content")
	JsonParseException trailingContent();

	@Message(id = ID_OFFSET + 26, value = "Missing JSON member name")
	JsonParseException missingMemberName();

	@Message(id = ID_OFFSET + 27, value = "Expected one JSON root; input was empty")
	JsonEmptyInputException emptyInput();

	@Message(id = ID_OFFSET + 28, value = "Truncated JSON input")
	JsonParseException truncatedInput();

	@Message(id = ID_OFFSET + 29, value = "Unsupported JSON decimal exponent")
	IllegalArgumentException unsupportedDecimalExponent();

	@Message(id = ID_OFFSET + 30, value = "Unsupported JSON decimal scale/exponent")
	IllegalArgumentException unsupportedDecimalScale();

	@Message(id = ID_OFFSET + 31, value = "Unsupported JSON number")
	IllegalArgumentException unsupportedNumber();

	@Message(id = ID_OFFSET + 32, value = "JSON number is not integral")
	ArithmeticException nonIntegralNumber();

	@Message(id = ID_OFFSET + 33, value = "JSON integer expansion exceeds supported scale")
	ArithmeticException integerExpansionLimitExceeded();

	@Message(id = ID_OFFSET + 34, value = "JSON parser still owns previous input")
	IllegalStateException parserOwnsPreviousInput();

	@Message(id = ID_OFFSET + 35, value = "Unexpected JSON token %s")
	JsonParseException unexpectedToken(String token);

	@Message(id = ID_OFFSET + 36, value = "Invalid JSON input: %s")
	JsonParseException invalidInput(String message, @Cause Throwable cause);

	@Message(id = ID_OFFSET + 37, value = "Cannot close JSON parser")
	JsonParseException cannotCloseParser(@Cause Throwable cause);

	@Message(id = ID_OFFSET + 38, value = "JSON body stream is closed")
	IOException bodyStreamClosed();

	@Message(id = ID_OFFSET + 39, value = "JSON name exceeds length limit %s")
	JsonParseException nameLengthLimitExceeded(int limit);

	@Message(id = ID_OFFSET + 40, value = "Invalid Unicode input")
	JsonParseException invalidUnicodeInput();

	@Message(id = ID_OFFSET + 41, value = "Cannot create JSON parser")
	JsonParseException cannotCreateParser(@Cause Throwable cause);

	@Message(id = ID_OFFSET + 42, value = "Unsupported JSON number %s")
	IllegalArgumentException unsupportedNumberType(String typeName);

	// Next unused message ID: ID_OFFSET + 43.
}
