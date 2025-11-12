package main.java.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java Main <caminho_do_arquivo>");
            return;
        }

        String filePath = args[0];

        try {
            // 1. Leitura do arquivo
            String code = new String(Files.readAllBytes(Paths.get(filePath)));

            // 2. Análise Léxica (Lexer)
            Lexer lexer = new Lexer(code);
            List<Token> tokens = lexer.tokenize();

            System.out.println("Tokens gerados pelo Lexer:");
            tokens.forEach(t -> System.out.println("  " + t));

            // 3. Análise Sintática (Parser)
            Parser parser = new Parser(tokens);
            Node ast = parser.parse();

            // Se a análise sintática falhar (AST é null), interrompe
            if (ast == null) {
                System.out.println("\nFalha na análise sintática. Compilação abortada.");
                return;
            }

            System.out.println("\nÁrvore sintática gerada pelo Parser:");
            System.out.println(ast);

            // 4. Análise Semântica (SemanticAnalyzer)
            System.out.println("\nIniciando Análise Semântica...");
            SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
            semanticAnalyzer.analyze(ast);
            
            // Se chegou até aqui, é porque as três fases passaram!
            System.out.println("Análise Semântica concluída com sucesso! ");
            System.out.println("\nCompilação concluída com sucesso! ");

        } catch (IOException e) {
            System.out.println("Erro ao ler o arquivo: " + e.getMessage());
        } catch (ParseError e) {
            // Captura erros lançados pelo Parser
            System.out.println("\nErro de compilação: " + e.getMessage());
        } catch (SemanticError e) {
            // Captura erros lançados pelo novo SemanticAnalyzer
            System.out.println("\nErro de compilação: " + e.getMessage());
        }
    }
}