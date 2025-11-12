package main.java.compiler;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private final String input;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    // Palavras reservadas da linguagem
    private static final String[] KEYWORDS = {"if", "else", "while", "return", "int", "float"};

    public Lexer(String input) { this.input = input; }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (pos < input.length()) {
            char current = peek();

            if (Character.isWhitespace(current)) {
                consumeWhitespace(); // Ignora espaços e quebra de linha
                continue;
            }

            if (Character.isLetter(current) || current == '_') { // Adicionado suporte a '_' em identificadores
                tokens.add(readIdentifierOrKeyword()); // Identificador ou palavra-chave
                continue;
            }

            if (Character.isDigit(current)) {
                tokens.add(readNumber()); // Número inteiro ou decimal
                continue;
            }

            if ("+-*/=<>!".indexOf(current) != -1) {
                tokens.add(readOperator()); // Operadores
                continue;
            }

            if ("();{},".indexOf(current) != -1) { // Adicionado ',' para ser um símbolo
                tokens.add(new Token(TokenType.SIMBOLO, String.valueOf(current), line, column));
                advance();
                continue;
            }
            
            // Caso de caractere inválido
            tokens.add(new Token(TokenType.INVALIDO, String.valueOf(current), line, column));
            advance();
        }

        // Adiciona o token de Fim de Arquivo (EOF)
        tokens.add(new Token(TokenType.EOF, "", line, column));

        return tokens;
    }

    private char peek() { return pos < input.length() ? input.charAt(pos) : '\0'; }

    private void advance() {
        if (pos < input.length()) {
            if (peek() == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            pos++;
        }
    }

    private Token readIdentifierOrKeyword() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        // Permite letras, números e underscore no identificador
        while (pos < input.length() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(peek());
            advance();
        }
        String value = sb.toString();
        // Verifica se é uma palavra-chave
        TokenType type = isKeyword(value) ? TokenType.PALAVRA_CHAVE : TokenType.IDENTIFICADOR;
        return new Token(type, value, line, startCol);
    }

    private boolean isKeyword(String s) {
        for (String kw : KEYWORDS) {
            if (kw.equals(s)) return true;
        }
        return false;
    }

    private Token readNumber() {
        int startCol = column;
        StringBuilder sb = new StringBuilder();
        
        // Lê a parte inteira
        while (pos < input.length() && Character.isDigit(peek())) {
            sb.append(peek());
            advance();
        }
        
        // Verifica se é um número de ponto flutuante
        if (peek() == '.') {
            sb.append(peek());
            advance(); // Consome o '.'
            
            // Lê a parte decimal
            while (pos < input.length() && Character.isDigit(peek())) {
                sb.append(peek());
                advance();
            }
        }
        
        return new Token(TokenType.NUMERO, sb.toString(), line, startCol);
    }

    private Token readOperator() {
        int startCol = column;
        char op = peek();
        advance();
        
        String opStr = String.valueOf(op);
        
        // Verifica operadores de dois caracteres (ex: ==, !=, <=, >=)
        if (pos < input.length()) {
            char next = peek();
            if ((op == '=' && next == '=') || (op == '!' && next == '=') ||
                (op == '<' && next == '=') || (op == '>' && next == '=')) {
                advance();
                opStr += next;
            }
        }
        return new Token(TokenType.OPERADOR, opStr, line, startCol);
    }

    private void consumeWhitespace() {
        while (pos < input.length() && Character.isWhitespace(peek())) {
            if (peek() == '\n') {
                line++;
                column = 0; // Será 1 após o avanço
            }
            pos++;
            column++;
        }
        // Ajuste final na coluna após loop
        if (pos > 0 && input.charAt(pos - 1) == '\n') {
            column = 1;
        } else if (pos > 0) {
            // Conta a coluna corretamente
            int lastNewline = input.lastIndexOf('\n', pos - 1);
            column = pos - lastNewline;
        }
    }
}