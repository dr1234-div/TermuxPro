package com.termux.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** 解析手机端常见粘贴格式，统一得到 OpenSSH 单一目标参数和端口。 */
final class SshTargetParser {

    private SshTargetParser() {}

    @Nullable
    static ParsedTarget parse(@NonNull String targetInput, @NonNull String portInput,
                              int defaultPort) {
        String rawTarget = targetInput.trim();
        String rawPort = portInput.trim();
        if (rawTarget.isEmpty()) return null;

        ParsedTarget candidate;
        if (rawTarget.startsWith("ssh://")) {
            candidate = parseUri(rawTarget);
        } else if (rawTarget.equals("ssh") || rawTarget.startsWith("ssh ")) {
            candidate = parseSshCommand(rawTarget);
        } else {
            candidate = parsePlainTarget(rawTarget);
        }
        if (candidate == null || !SshTargetValidator.isValid(candidate.host)) return null;

        int resolvedPort = candidate.port;
        if (resolvedPort < 1) {
            resolvedPort = parsePort(rawPort, defaultPort);
        }
        if (resolvedPort < 1 || resolvedPort > 65535) return null;
        return new ParsedTarget(candidate.host, resolvedPort);
    }

    @Nullable
    private static ParsedTarget parseUri(@NonNull String rawTarget) {
        String authorityAndPath = rawTarget.substring("ssh://".length());
        int pathIndex = authorityAndPath.indexOf('/');
        String authority = pathIndex >= 0 ? authorityAndPath.substring(0, pathIndex)
            : authorityAndPath;
        if (authority.isEmpty()) return null;
        return parsePlainTarget(authority);
    }

    @Nullable
    private static ParsedTarget parseSshCommand(@NonNull String rawTarget) {
        String[] tokens = rawTarget.split("\\s+");
        if (tokens.length < 2 || !"ssh".equals(tokens[0])) return null;
        String target = null;
        int port = -1;
        boolean afterDoubleDash = false;
        for (int index = 1; index < tokens.length; index++) {
            String token = tokens[index];
            if (token.isEmpty()) continue;
            if (!afterDoubleDash && "--".equals(token)) {
                afterDoubleDash = true;
                continue;
            }
            if (!afterDoubleDash && "-p".equals(token)) {
                if (++index >= tokens.length) return null;
                port = parsePort(tokens[index], -1);
                if (port < 1) return null;
                continue;
            }
            if (!afterDoubleDash && token.startsWith("-p") && token.length() > 2) {
                port = parsePort(token.substring(2), -1);
                if (port < 1) return null;
                continue;
            }
            if (!afterDoubleDash && token.startsWith("-")) {
                return null;
            }
            if (target != null) return null;
            ParsedTarget parsed = parsePlainTarget(token);
            if (parsed == null) return null;
            target = parsed.host;
            if (parsed.port > 0) port = parsed.port;
        }
        return target == null ? null : new ParsedTarget(target, port);
    }

    @Nullable
    private static ParsedTarget parsePlainTarget(@NonNull String rawTarget) {
        if (rawTarget.indexOf('/') >= 0) return null;
        int atIndex = rawTarget.lastIndexOf('@');
        String hostPart = atIndex >= 0 ? rawTarget.substring(atIndex + 1) : rawTarget;
        int portSeparator = inlinePortSeparator(hostPart);
        if (portSeparator < 0) return new ParsedTarget(rawTarget, -1);

        String hostWithoutPort = hostPart.substring(0, portSeparator);
        String portText = hostPart.substring(portSeparator + 1);
        int port = parsePort(portText, -1);
        if (port < 1 || hostWithoutPort.isEmpty()) return null;
        String prefix = atIndex >= 0 ? rawTarget.substring(0, atIndex + 1) : "";
        return new ParsedTarget(prefix + hostWithoutPort, port);
    }

    private static int inlinePortSeparator(@NonNull String hostPart) {
        int separator = hostPart.lastIndexOf(':');
        if (separator <= 0 || separator >= hostPart.length() - 1) return -1;
        if (hostPart.indexOf(':') != separator) return -1;
        for (int index = separator + 1; index < hostPart.length(); index++) {
            if (!Character.isDigit(hostPart.charAt(index))) return -1;
        }
        return separator;
    }

    private static int parsePort(@NonNull String value, int defaultValue) {
        if (value.trim().isEmpty()) return defaultValue;
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 1 && port <= 65535 ? port : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    static final class ParsedTarget {
        @NonNull final String host;
        final int port;

        ParsedTarget(@NonNull String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}
