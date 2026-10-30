package com.muhammaddaffa.mdlib.commands.args;

import java.util.List;

public final class TokenReader {
    private final List<String> tokens;
    private int i = 0;

    public TokenReader(String[] raw) { this.tokens = List.of(raw); }

    public boolean hasNext()      { return i < tokens.size(); }
    public String  peek()         { return hasNext() ? tokens.get(i) : null; }
    public String  next()         { return hasNext() ? tokens.get(i++) : null; }
    public int     remaining()    { return tokens.size() - i; }

    public String remainingJoined() {
        if (!hasNext()) return "";
        StringBuilder sb = new StringBuilder();
        while (hasNext()) sb.append(next()).append(' ');
        return sb.toString().trim();
    }
}
