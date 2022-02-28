package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch(RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================


    private Statement parseProgramStatement() {
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression expression = parseComparisonExpression();
        while (tokens.match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rhs = parseComparisonExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rhs);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(rhs.getEnd());
            expression = equalityExpression;
        }
        return expression;
    }
    private Expression parseComparisonExpression() {
        Expression expression = parseAdditiveExpression();
        while (tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rhs = parseAdditiveExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator,expression, rhs);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(rhs.getEnd());
            expression = comparisonExpression;
        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rhs = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rhs);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rhs.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();
        while(tokens.match(SLASH, STAR)){
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rightHandSide.getEnd());
            expression = factorExpression;
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token operator = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(operator, rhs);
            unaryExpression.setStart(operator);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(STRING)) {
            Token stringToken = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
            stringExpression.setToken(stringToken);
            return stringExpression;
        } else if (tokens.match(IDENTIFIER)) {
            // the following three lines handle parseIdentifierExpression() test case
            // returns identifier Expression at the bottom.
            Token identifierToken = tokens.consumeToken();
            IdentifierExpression identifierExpression = new IdentifierExpression(identifierToken.getStringValue());
            identifierExpression.setToken(identifierToken);

            // ErrorType variable error set to null (will later be used if EOF)
            ErrorType error = null;
            // LinkedList for function call arguments
            List<Expression> funcCallArguments= new LinkedList<>();
            // if a Left Parenthesis is found enter this logic.
            if (tokens.matchAndConsume(LEFT_PAREN)) {
                /* the following logic handles the following test cases
                * parseFunctionCallExpression()
                * parseNoArgFunctionCallExpression()
                * parseUnterminatedFunctionCallExpression() */
                while (!tokens.match(RIGHT_PAREN)) { // as long as a right parenthesis isn't found, continue to loop through
                    if(tokens.matchAndConsume(COMMA)) { // consume those commas & continue with your day
                        continue;
                    } else if (tokens.match(EOF)) { // check for END OF FILE || store it if found
                        error = (ErrorType.UNTERMINATED_ARG_LIST);
                        break;
                    } else { // if the above cases haven't been hit - then parse the expression
                        Expression expression = parseExpression();
                        funcCallArguments.add(expression); // add to the linked list
                    }
                }
                FunctionCallExpression fcExpression = new FunctionCallExpression(identifierToken.getStringValue(), funcCallArguments);
                fcExpression.setStart(identifierToken);
                fcExpression.setEnd(tokens.getCurrentToken());
                tokens.matchAndConsume(RIGHT_PAREN);
                if (error != null) { // if error is not null then an error was found & we add it
                    fcExpression.addError(error);
                }
                return fcExpression;
            }
            return identifierExpression;
        } else if (tokens.match(TRUE)) {
            Token booToken = tokens.consumeToken();
            BooleanLiteralExpression booExpression = new BooleanLiteralExpression(true);
            booExpression.setToken(booToken);
            return booExpression;
        } else if (tokens.match(FALSE)) {
            Token booToken = tokens.consumeToken();
            BooleanLiteralExpression booExpression = new BooleanLiteralExpression(false);
            booExpression.setToken(booToken);
            return booExpression;
        } else if (tokens.match(NULL)) {
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExpression = new NullLiteralExpression();
            nullExpression.setToken(nullToken);
            return nullExpression;
        } else if (tokens.match(LEFT_BRACKET)) {
            Token llToken = tokens.consumeToken();
            List<Expression> listExpression = new LinkedList<>();
            ErrorType error = null;
            while (!tokens.match(RIGHT_BRACKET)) {
                if (tokens.matchAndConsume(COMMA)) {
                    continue;
                } else if(tokens.match(EOF)){
                    error = (ErrorType.UNTERMINATED_LIST);
                    break;
                } else {
                    Expression expression = parseExpression();
                    listExpression.add(expression);
                }
            }
            ListLiteralExpression llExpression = new ListLiteralExpression(listExpression);
            llExpression.setStart(llToken);
            llExpression.setEnd(tokens.getCurrentToken());
            tokens.matchAndConsume(RIGHT_BRACKET);
            if(error != null){
                llExpression.addError(error);
            }
            return llExpression;
        } else if (tokens.match(LEFT_PAREN)) {
            Token operator = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(operator.getStringValue());
            return new ParenthesizedExpression(stringExpression);
        } else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
