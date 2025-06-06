package com.hibiscus.signal.utils;

import com.hibiscus.signal.core.SignalContext;

import java.util.*;

/**
 * Utility class for printing a visual trace tree of signal processing spans.
 * Purpose:
 * - Helps visualize the hierarchical structure of spans in a signal context.
 * - Useful for debugging, monitoring, or performance analysis.
 */
public class SignalTracer {

    /**
     * Prints the trace tree for a given SignalContext.
     *
     * @param context the SignalContext containing tracing spans
     */
    public static void printTraceTree(SignalContext context) {
        System.out.println("[TRACE traceId=" + context.getTraceId() + "]");
        List<SignalContext.Span> spans = context.getSpans();

        if (spans.isEmpty()) {
            System.out.println("  (no span data)");
            return;
        }

        // Build a map of parent span ID to list of child spans
        Map<String, List<SignalContext.Span>> childMap = new HashMap<>();
        // Build a map of span ID to the span itself
        Map<String, SignalContext.Span> spanMap = new HashMap<>();
        for (SignalContext.Span span : spans) {
            spanMap.put(span.getSpanId(), span);
            childMap.computeIfAbsent(span.getParentSpanId(), k -> new ArrayList<>()).add(span);
        }

        // Find the root spans (those with parentSpanId not found in spanMap)
        for (SignalContext.Span span : spans) {
            if (!spanMap.containsKey(span.getParentSpanId())) {
                printSpanTree(span, childMap, 0);
            }
        }
    }

    /**
     * Recursively prints the span tree with indentation for hierarchy.
     *
     * @param span      the current span to print
     * @param childMap  map of parent span ID to child spans
     * @param level     the indentation level (depth in the tree)
     */
    private static void printSpanTree(SignalContext.Span span, Map<String, List<SignalContext.Span>> childMap, int level) {
        String indent = repeat("  ", level);
        System.out.printf("%s└─ [%s] %s (%dms)%n",
                indent,
                span.getSpanId(),
                span.getOperation(),
                span.getEndTime() - span.getStartTime());

        // Recursively print children of this span, if any
        List<SignalContext.Span> children = childMap.get(span.getSpanId());
        if (children != null) {
            for (SignalContext.Span child : children) {
                printSpanTree(child, childMap, level + 1);
            }
        }
    }

    /**
     * Repeats a given string a specific number of times.
     *
     * @param str   the string to repeat
     * @param count the number of times to repeat
     * @return the resulting repeated string
     */
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(str);
        return sb.toString();
    }
}
