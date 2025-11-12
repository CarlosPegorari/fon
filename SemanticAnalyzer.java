package main.java.compiler;

import java.util.HashMap;
import java.util.Map;

// Criamos uma nova exceção para erros semânticos
class SemanticError extends RuntimeException {
    public SemanticError(String message) {
        super(message);
    }
}

public class SemanticAnalyzer {

    // A Tabela de Símbolos.
    // Armazena: <Nome da Variável (String), Tipo da Variável (String)>
    private Map<String, String> symbolTable;

    public SemanticAnalyzer() {
        this.symbolTable = new HashMap<>();
    }

    // Método principal que inicia a análise
    public void analyze(Node ast) {
        try {
            traverse(ast);
        } catch (SemanticError e) {
            System.err.println("Erro Semântico: " + e.getMessage());
            // Interrompe a compilação (ou pode ser tratado de forma mais robusta)
            throw e; 
        }
    }

    // Método recursivo que "caminha" pela árvore (AST)
    private void traverse(Node node) {
        if (node == null) {
            return;
        }

        // 1. ANÁLISE DO NÓ DE PROGRAMA (A Raiz)
        if (node instanceof ProgramNode) {
            ProgramNode program = (ProgramNode) node;
            for (Node statement : program.statements) {
                traverse(statement); // Analisa cada linha do programa
            }
        }

        // 2. ANÁLISE DE DECLARAÇÃO (ex: int x = 10;)
        else if (node instanceof VarDeclarationNode) {
            VarDeclarationNode decl = (VarDeclarationNode) node;
            String varName = decl.name.getLexeme();
            
            // REGRA 1: Variável já foi declarada?
            if (symbolTable.containsKey(varName)) {
                throw new SemanticError(String.format("[Linha %d] Variável '%s' já foi declarada.", decl.name.getLine(), varName));
            }
            
            // Se não, adiciona na tabela
            symbolTable.put(varName, decl.type.getLexeme());
            
            // Analisa a expressão de inicialização (ex: int x = y + 5;)
            if (decl.initializer != null) {
                traverse(decl.initializer);
            }
        }

        // 3. ANÁLISE DE ATRIBUIÇÃO (ex: x = 20;)
        else if (node instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) node;
            String varName = assign.name.getLexeme();

            // REGRA 2: Tentando atribuir a uma variável que não existe?
            if (!symbolTable.containsKey(varName)) {
                throw new SemanticError(String.format("[Linha %d] Variável '%s' não foi declarada.", assign.name.getLine(), varName));
            }
            
            // Analisa a expressão do lado direito (ex: x = y + z;)
            traverse(assign.value);
        }

        // 4. ANÁLISE DO USO DE UMA VARIÁVEL (ex: ... = x + 5;)
        else if (node instanceof VariableNode) {
            VariableNode var = (VariableNode) node;
            String varName = var.name.getLexeme();
            
            // REGRA 3: Tentando usar uma variável que não existe?
            if (!symbolTable.containsKey(varName)) {
                throw new SemanticError(String.format("[Linha %d] Variável '%s' não foi declarada.", var.name.getLine(), varName));
            }
        }

        // 5. NÓS DE NAVEGAÇÃO (apenas continua a descida)
        else if (node instanceof BinaryNode) {
            BinaryNode binOp = (BinaryNode) node;
            traverse(binOp.left);
            traverse(binOp.right);
        }

        // 6. NÓS BASE (não precisam de análise)
        else if (node instanceof NumberNode) {
            // Números são sempre semanticamente válidos.
            return;
        }
    }
}