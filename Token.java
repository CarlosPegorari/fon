package main.java.compiler;

public class Token {
    private TokenType type;
    private String lexeme;
    private int line;
    private int position;

    public Token(TokenType type, String lexeme, int line, int position) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.position = position;
    }

    public TokenType getType() { return type; }
    public String getLexeme() { return lexeme; }
    public int getLine() { return line; }
    public int getPosition() { return position; }

    @Override
    public String toString() {
        return String.format("%s('%s') em %d:%d", type, lexeme, line, position);
    }
}
