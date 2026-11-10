package org.hibernate.search.util.json.impl;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.util.json.logging.impl.JsonLog;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BigIntegerNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DecimalNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.FloatNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;

/**
 * Validates standard, acyclic JSON nodes without invoking serializers or copying.
 */
public final class JsonTreeValidator {

	private static final Set<Class<?>> STANDARD =
			Set.of( ObjectNode.class, ArrayNode.class, StringNode.class, BooleanNode.class, NullNode.class, ShortNode.class,
					IntNode.class, LongNode.class, BigIntegerNode.class, DecimalNode.class, FloatNode.class, DoubleNode.class );

	static void decimal(BigDecimal value) {
		JsonNumbers.validateDecimal( value );
	}

	public static void validate(JsonNode root) {
		var active = new IdentityHashMap<JsonNode, Boolean>();
		Deque<Frame> stack = new ArrayDeque<>();
		check( Objects.requireNonNull( root ) );
		if ( !root.isContainer() ) {
			return;
		}
		active.put( root, true );
		stack.push( frame( root ) );
		while ( !stack.isEmpty() ) {
			Frame current = stack.peek();
			if ( !current.children.hasNext() ) {
				active.remove( current.node );
				stack.pop();
				continue;
			}
			Object next = current.children.next();
			if ( current.node.isObject() ) {
				if ( !( next instanceof Map.Entry<?, ?> entry ) || !( entry.getKey() instanceof String ) ) {
					throw JsonLog.INSTANCE.invalidMemberName();
				}
				next = entry.getValue();
			}
			if ( !( next instanceof JsonNode child ) ) {
				throw JsonLog.INSTANCE.invalidContainerValue();
			}
			check( child );
			if ( child.isContainer() ) {
				if ( active.put( child, true ) != null ) {
					throw JsonLog.INSTANCE.cyclicTree();
				}
				stack.push( frame( child ) );
			}
		}
	}

	private static void check(JsonNode node) {
		if ( !STANDARD.contains( node.getClass() ) ) {
			throw JsonLog.INSTANCE.unsupportedNode( node.getClass().getName() );
		}
		if ( node instanceof DoubleNode || node instanceof FloatNode ) {
			if ( !Double.isFinite( node.doubleValue() ) ) {
				throw JsonLog.INSTANCE.nonFiniteNumber();
			}
		}
		else if ( node instanceof DecimalNode ) {
			decimal( node.decimalValue() );
		}
	}

	private static Frame frame(JsonNode node) {
		return new Frame( node, node.isObject() ? node.properties().iterator() : node.values().iterator() );
	}

	private record Frame(JsonNode node, Iterator<?> children) {
	}

	private JsonTreeValidator() {
	}
}
