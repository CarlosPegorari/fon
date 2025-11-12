package main.java.compiler;

import java.util.List;
import java.util.ArrayList;

// =========================================================
// NOVOS NÓS DA AST
// =========================================================

abstract class Node extends ASTNode { }

class NumberNode extends Node {
    public final String value;
    public NumberNode(String value) { this.value = value; }
    @Override
    public String toString() { return value; }
}

class VariableNode extends Node {
    public final Token name;
    public VariableNode(Token name) { this.name = name; }
    @Override
    public String toString() { return name.getLexeme(); }
}

class BinaryNode extends Node {
    public final Node left;
    public final Token operator;
    public final Node right;
    public BinaryNode(Node left, Token operator, Node right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
    @Override
    public String toString() {
        return "(" + left + " " + operator.getLexeme() + " " + right + ")";
    }
}

class AssignmentNode extends Node {
    public final Token name;
    public final Node value;
    public AssignmentNode(Token name, Node value) {
        this.name = name;
        this.value = value;
    }
    @Override
    public String toString() {
        return "Assignment(" + name.getLexeme() + " = " + value + ")";
    }
}

class VarDeclarationNode extends Node {
    public final Token type;
    public final Token name;
    public final Node initializer;
    public VarDeclarationNode(Token type, Token name, Node initializer) {
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }
    @Override
    public String toString() {
        String base = "Decl(" + type.getLexeme() + " " + name.getLexeme();
        return initializer != null ? base + " = " + initializer + ")" : base + ")";
    }
}

class ProgramNode extends Node {
    public final List<Node> statements;
    public ProgramNode(List<Node> statements) {
        this.statements = statements;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Program {\n");
        for (Node statement : statements) {
            sb.append("  ").append(statement.toString()).append(";\n");
        }
        sb.append("}");
        return sb.toString();
    }
}

class ParseError extends RuntimeException {
    public ParseError(String message) { super(message); }
}

// =========================================================
// CLASSE PARSER REVISADA
// =========================================================

public class Parser {

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) { this.tokens = tokens; }

    public Node parse() {
        try { 
            // Novo ponto de entrada para analisar o programa inteiro
            return program(); 
        }
        catch (ParseError e) {
            System.err.println("Erro de sintaxe: " + e.getMessage());
            return null;
        }
    }

    // Analisa a lista de declarações/comandos do programa
    private Node program() {
        List<Node> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return new ProgramNode(statements);
    }

    // Tenta analisar uma declaração de variável ou um comando
    private Node declaration() {
        try {
            // 1. DECLARAÇÃO DE VARIÁVEL (Ex: 'int x = 10;')
            if (checkKeyword("int") || checkKeyword("float")) {
                return varDeclaration();
            }
            
            // Se não for uma declaração, assume que é um comando comum (como atribuição ou expressão avulsa)
            return statement();
        } catch (ParseError error) {
            // Sincronização: Se um comando falhar, pula para o próximo ';' ou EOF
            synchronize();
            return null; // Retorna null para este comando, mas continua analisando
        }
    }

    private Node varDeclaration() {
        // Consome o tipo (int ou float)
        Token type = advance(); 

        // Consome o nome da variável
        Token name = consume(TokenType.IDENTIFICADOR, "Esperado nome da variável após o tipo '" + type.getLexeme() + "'");
        
        // Atribuição inicial opcional (Ex: ' = 10')
        Node initializer = null;
        if (check(TokenType.OPERADOR) && peek().getLexeme().equals("=")) {
            advance(); // consome '='
            initializer = expression();
        }

        // Consome o ponto e vírgula
        consume(TokenType.SIMBOLO, ";", "Esperado ';' após declaração de variável");

        return new VarDeclarationNode(type, name, initializer);
    }

    // Tenta analisar um comando (Statement)
    private Node statement() {
        // Tentativa de atribuição (Ex: 'x = x + y * 2;')
        // Verifica se é um IDENTIFICADOR seguido por '='
        if (check(TokenType.IDENTIFICADOR) && checkNext("=")) {
            Token name = advance(); // Consome o identificador (variável 'x')
            advance(); // Consome o operador '='
            
            Node value = expression(); // Analisa a expressão à direita
            
            consume(TokenType.SIMBOLO, ";", "Esperado ';' após a atribuição");
            return new AssignmentNode(name, value);
        }
        
        // Se não for um comando especial, trata como uma expressão seguida de ';'
        Node expr = expression();
        consume(TokenType.SIMBOLO, ";", "Esperado ';' após a expressão");
        return expr;
    }

    // Regras de precedência (iniciando com a menor precedência: comparação)
    private Node expression() {
        return comparison();
    }

    private Node comparison() {
        Node node = addition();
        while (checkOperator("==") || checkOperator("!=") || checkOperator("<") || checkOperator(">") || checkOperator("<=") || checkOperator(">=")) {
            Token operator = advance();
            Node right = addition();
            node = new BinaryNode(node, operator, right);
        }
        return node;
    }

    private Node addition() {
        Node node = multiplication();
        while (checkOperator("+") || checkOperator("-")) {
            Token operator = advance();
            Node right = multiplication();
            node = new BinaryNode(node, operator, right);
        }
        return node;
    }

    private Node multiplication() {
        Node node = primary();
        while (checkOperator("*") || checkOperator("/")) {
            Token operator = advance();
            Node right = primary();
            node = new BinaryNode(node, operator, right);
        }
        return node;
    }

    // Analisa o menor componente (número, variável, (expressão))
    private Node primary() {
        // 1. Número
        if (match(TokenType.NUMERO)) {
            return new NumberNode(previous().getLexeme());
        }
        
        // 2. Identificador (Variável)
        if (match(TokenType.IDENTIFICADOR)) {
            return new VariableNode(previous());
        }

        // 3. Sub-expressão entre parênteses
        if (check(TokenType.SIMBOLO) && peek().getLexeme().equals("(")) {
            advance(); // consome '('
            Node expr = expression();
            consume(TokenType.SIMBOLO, ")", "Esperado ')' após a expressão");
            return expr;
        }

        // Se nenhum dos casos acima for encontrado
        throw error(peek(), "Esperado número, identificador ou '('");
    }

    // =========================================================
    // Métodos Auxiliares
    // =========================================================
    
    // Sincronização de erro: avança até encontrar um ponto seguro para recomeçar
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().getLexeme().equals(";")) return;

            switch (peek().getType()) {
                case PALAVRA_CHAVE:
                    // Se for uma palavra-chave de início de comando, para aqui
                    if (peek().getLexeme().equals("if") || peek().getLexeme().equals("while") ||
                        peek().getLexeme().equals("int") || peek().getLexeme().equals("float") ||
                        peek().getLexeme().equals("return")) {
                        return;
                    }
                    break;
                default:
                    // Continua avançando
            }
            advance();
        }
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) { advance(); return true; }
        }
        return false;
    }

    private Token consume(TokenType type, String expectedLexeme, String errorMessage) {
        if (check(type) && peek().getLexeme().equals(expectedLexeme)) return advance();
        throw error(peek(), errorMessage);
    }
    
    private Token consume(TokenType type, String errorMessage) {
        if (check(type)) return advance();
        throw error(peek(), errorMessage);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().getType() == type;
    }
    
    private boolean checkNext(String lexeme) {
        if (isAtEnd() || current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).getLexeme().equals(lexeme);
    }

    private boolean checkOperator(String op) {
        return check(TokenType.OPERADOR) && peek().getLexeme().equals(op);
    }

    private boolean checkKeyword(String kw) {
        return check(TokenType.PALAVRA_CHAVE) && peek().getLexeme().equals(kw);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() { return peek().getType() == TokenType.EOF; }
    private Token peek() { 
        if (current >= tokens.size()) return tokens.get(tokens.size() - 1); // Garante que retorna EOF
        return tokens.get(current); 
    }
    private Token previous() { return tokens.get(current - 1); }
    
    private ParseError error(Token token, String message) {
        return new ParseError(String.format("[Linha %d, Coluna %d] %s", token.getLine(), token.getPosition(), message));
    }
}