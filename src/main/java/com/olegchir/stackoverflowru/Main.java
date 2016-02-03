package com.olegchir.stackoverflowru;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.apache.commons.cli.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Created by olegchir on 04.02.2016.
 */

/**
 * Этот класс содержит в себе исполняемую из терминала программу,
 * которая читает java-файл, добавляет в него поле согласно параметрам командной строки,
 * и пишет его в исходящий java-файл.
 */
public class Main {

    /**
     * Главный метод приложения
     * @param args
     */
    public static void main(String[] args) {
        //Парсим параметры командной строки
        Optional<ParsedArgs> argzOpt = ParsedArgs.parse(args);
        if (!argzOpt.isPresent()) {
            System.out.println("Can't parse parameters, terminated.");
            return;
        }
        ParsedArgs argz = argzOpt.get();

        //Перезаписываем файл
        addVariableAndReplaceFile(argz.type, argz.varName, argz.initString, argz.srcFile, argz.destFile);

        //PROFIT
        System.out.println("All operations completed.");
    }

    /**
     * Перезаписать файл destFile содержимым srcFile с добавлением поля типа type, с именем name и инициализатором initString
     */
    public static void addVariableAndReplaceFile(String type, String name, String initString, String srcFile, String destFile) {
        //Добавляем поле, получаем новый текст класса
        Optional<String> result = addVariable(type, name, initString, srcFile);
        if (result.isPresent()) {
            try {
                //Пишем полученный текст в файл
                Files.write(Paths.get(destFile), result.get().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(String.format("File was changed and saved to: %s", destFile));
        } else {
            System.out.println("Parser returned an empty response - probably something gone wrong!");
        }
    }

    /**
     * Вернуть строку с содержимым srcFile с добавлением поля типа type, с именем name и инициализатором initString
     */
    public static Optional<String> addVariable(String type, String name, String initString, String srcFile) {
        Optional<String> result = Optional.empty();

        //С помощью Javaparser конструируем CompilationUnit из файла
        Optional<CompilationUnit> compilationUnitOpt = Optional.empty();
        try(FileInputStream in = new FileInputStream(srcFile)) {
            compilationUnitOpt = Optional.of(JavaParser.parse(in));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Определяем параметры добавляемого поля
        FieldDeclaration fieldDeclaration = new FieldDeclaration(
                ModifierSet.PUBLIC |  ModifierSet.STATIC |  ModifierSet.FINAL,
                new ClassOrInterfaceType(type),
                new VariableDeclarator(new VariableDeclaratorId(name), new NameExpr(initString))
        );

        //Добавляем поле в compilation unit
        if (compilationUnitOpt.isPresent()) {
            CompilationUnit compilationUnit = compilationUnitOpt.get();
            for (TypeDeclaration typ : compilationUnit.getTypes()) {
                ASTHelper.addMember(typ, fieldDeclaration);
            }
            result = Optional.of(compilationUnit.toString());
        }

        return result;
    }

    /**
     * Класс для хранения пачки параметров командной строки для приложния, которое должно выполняться в терминале
     */
    private static class ParsedArgs {
        public String srcFile;
        public String destFile;
        public String type;
        public String varName;
        public String initString;

        public static Optional<ParsedArgs> parse(String[] args) {
            Optional<ParsedArgs> result = Optional.empty();

            try {
                //Формируем правила Apache Commons CLI
                ParsedArgs obj = new ParsedArgs();
                Options options = new Options();
                options.addOption("s", "source", true, "source file");
                options.addOption("d", "dest", true, "destination file");
                options.addOption("t", "type", true, "type of the variable");
                options.addOption("n", "name", true, "name of the variable");
                options.addOption("i", "init", true, "initialization string for the variable");

                //Парсим аргументы с помощью заготовленных правил
                CommandLineParser parser = new DefaultParser();
                CommandLine cmd = parser.parse( options, args);

                //Перебрасываем в поля класса
                obj.srcFile = cmd.getOptionValue("s");
                obj.destFile = cmd.getOptionValue("d");
                obj.type = cmd.getOptionValue("t");
                obj.varName = cmd.getOptionValue("n");
                obj.initString = cmd.getOptionValue("i");

                //Все переменные строго обязательные - если одна из них пропущена, печатаем help и выходим
                if (null == obj.srcFile || null == obj.destFile || null == obj.type
                        || null == obj.varName || null == obj.initString) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp( "jaddfield", options );
                    System.out.println("ALL PARAMETERS ARE MANDATORY");

                    return Optional.empty();
                }

                result = Optional.of(obj);

            } catch (ParseException e) {
                System.out.println("Error trying to parse options");
                e.printStackTrace();
                return Optional.empty();
            }

            return result;
        }
    }
}
