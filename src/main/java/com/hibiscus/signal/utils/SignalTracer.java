package com.hibiscus.signal.utils;

import com.hibiscus.signal.core.SignalContext;

import java.util.*;

public class SignalTracer {

    public static void printTraceTree(SignalContext context) {
        System.out.println("[TRACE traceId=" + context.getTraceId() + "]");
        List<SignalContext.Span> spans = context.getSpans();
        if (spans.isEmpty()) {
            System.out.println("  (no span data)");
            return;
        }

        // 构建父子映射
        Map<String, List<SignalContext.Span>> childMap = new HashMap<>();
        Map<String, SignalContext.Span> spanMap = new HashMap<>();
        for (SignalContext.Span span : spans) {
            spanMap.put(span.getSpanId(), span);
            childMap.computeIfAbsent(span.getParentSpanId(), k -> new ArrayList<>()).add(span);
        }

        // 找到根 span（parentSpanId 无对应 spanId）
        for (SignalContext.Span span : spans) {
            if (!spanMap.containsKey(span.getParentSpanId())) {
                printSpanTree(span, childMap, 0);
            }
        }
    }

    private static void printSpanTree(SignalContext.Span span, Map<String, List<SignalContext.Span>> childMap, int level) {
        String indent = repeat("  ", level);
        System.out.printf("%s└─ [%s] %s (%dms)%n", indent, span.getSpanId(), span.getOperation(),
                span.getEndTime() - span.getStartTime());

        List<SignalContext.Span> children = childMap.get(span.getSpanId());
        if (children != null) {
            for (SignalContext.Span child : children) {
                printSpanTree(child, childMap, level + 1);
            }
        }
    }

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append(str);
        return sb.toString();
    }
}
