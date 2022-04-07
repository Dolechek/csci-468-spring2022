package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import javax.xml.namespace.QName;
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
        if (tokens.match(IDENTIFIER)) {
            Token name = tokens.consumeToken();
            if (tokens.match(EQUAL)) {
                return parseAssignmentStatement(name);
            } else if (tokens.match(LEFT_PAREN)) {
                return parseFunctionCallStatement(name);
            }
        }
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        Statement varStmt = parseVarStatement();
        if (varStmt != null) {
            return varStmt;
        }
        Statement funcDefStmt = parseFunctionDefinition();
        if (funcDefStmt != null) {
            return funcDefStmt;
        }
        Statement forStmt = parseForStatement();
        if ( forStmt != null) {
            return forStmt;
        }
        Statement ifStmt = parseIfStatement();
        if (ifStmt != null) {
            return ifStmt;
        }
        if (currentFunctionDefinition != null) {
            Statement returnStmt = parseReturnStatement();
            if (returnStmt != null) {
                return returnStmt;
            }
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }


    // ==== DONE ====
    private AssignmentStatement parseAssignmentStatement(Token name) {
        AssignmentStatement assignmentStatement = new AssignmentStatement();
        assignmentStatement.setStart(name);
        assignmentStatement.setVariableName(name.getStringValue());
        require(EQUAL, assignmentStatement);
        assignmentStatement.setExpression(parseExpression());
        assignmentStatement.setEnd(assignmentStatement.getExpression().getEnd());
        return assignmentStatement;
    }
    // ==== DONE ====
    private FunctionCallStatement parseFunctionCallStatement(Token name) {
        List<Expression> args = new LinkedList<>();
        if (tokens.matchAndConsume(LEFT_PAREN) && !tokens.match(RIGHT_PAREN)) {
            do {
                Expression expression = parseExpression();
                args.add(expression);
            } while (tokens.matchAndConsume(COMMA));
        }
        FunctionCallExpression functionCallExpression = new FunctionCallExpression(name.getStringValue(), args);
        FunctionCallStatement functionCallStatement = new FunctionCallStatement(functionCallExpression);
        functionCallStatement.setStart(name);
        functionCallStatement.setEnd(require(RIGHT_PAREN, functionCallStatement));
        return functionCallStatement;
    }
    // ==== DONE ====
    private ForStatement parseForStatement() {
        if (tokens.match(FOR)) {
            ForStatement forStatement = new ForStatement();
            forStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN, forStatement);
            Token id = require(IDENTIFIER, forStatement);
            forStatement.setVariableName(id.getStringValue());
            require(IN, forStatement);
            forStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, forStatement);
            require(LEFT_BRACE, forStatement);
            List<Statement> stmtList = new LinkedList<>();
            // loop through and add to the stmtList
            while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
                stmtList.add(parseProgramStatement());
            }
            forStatement.setBody(stmtList);
            forStatement.setEnd(require(RIGHT_BRACE, forStatement));
            return forStatement;
        } else {
            return null;
        }
    }
    // ==== DONE ====
    private IfStatement parseIfStatement() {
        if (tokens.match(IF)) {
            IfStatement ifStatement = new IfStatement();
            ifStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN, ifStatement);
            ifStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, ifStatement);
            require(LEFT_BRACE, ifStatement);
            List<Statement> trueStmt = new LinkedList<>();
            while (!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()) {
                trueStmt.add(parseProgramStatement());
            }
            ifStatement.setTrueStatements(trueStmt);
            Token lastTkn = require(RIGHT_BRACE, ifStatement);
            if (tokens.matchAndConsume(ELSE)) {
                require(LEFT_BRACE, ifStatement);
                LinkedList<Statement> elseStmt = new LinkedList<>();
                while (!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()) {
                    elseStmt.add(parseProgramStatement());
                }
                ifStatement.setElseStatements(elseStmt);
                lastTkn = require(RIGHT_BRACE, ifStatement);
            }
            ifStatement.setEnd(lastTkn);
            return ifStatement;
        } else {
            return null;
        }
    }
    // ==== DONE ====
    private FunctionDefinitionStatement parseFunctionDefinition() {
        if (tokens.match(FUNCTION)) {
            FunctionDefinitionStatement func = new FunctionDefinitionStatement();
            func.setStart(tokens.consumeToken());
            Token funcName = require(IDENTIFIER, func);
            func.setName(funcName.getStringValue());
            require(LEFT_PAREN, func);
            if(!tokens.match(RIGHT_PAREN)) {
                do {
                    Token paramName = require(IDENTIFIER, func);
                    TypeLiteral typeLiteral = null;
                    if(tokens.matchAndConsume(COLON)) {
                        typeLiteral = parseTypeLiteral();
                    }
                    func.addParameter(paramName.getStringValue(), typeLiteral);
                } while (tokens.matchAndConsume(COMMA));
            }
            require(RIGHT_PAREN, func);
            TypeLiteral typeLiteral = null;
            if (tokens.matchAndConsume(COLON)) {
                typeLiteral = parseTypeLiteral();
            }
            func.setType(typeLiteral);
            currentFunctionDefinition = func;
            require(LEFT_BRACE, func);
            LinkedList<Statement> statements = new LinkedList<>();
            while(!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()) {
                statements.add(parseProgramStatement());
            }
            currentFunctionDefinition = null;
            func.setBody(statements);
            func.setEnd(require(RIGHT_BRACE, func));
            return func;
        } else {
            return null;
        }
    }
    // ==== DONE ====
    private TypeLiteral parseTypeLiteral() {
        if (tokens.match("int")) {
            TypeLiteral typeLiteral = new TypeLiteral();
            typeLiteral.setType(CatscriptType.INT);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("string")) {
            TypeLiteral typeLiteral = new TypeLiteral();
            typeLiteral.setType(CatscriptType.STRING);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("bool")) {
            TypeLiteral typeLiteral = new TypeLiteral();
            typeLiteral.setType(CatscriptType.BOOLEAN);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("object")) {
            TypeLiteral typeLiteral = new TypeLiteral();
            typeLiteral.setType(CatscriptType.OBJECT);
            typeLiteral.setToken(tokens.consumeToken());
            return typeLiteral;
        } else if (tokens.match("list")) {
            TypeLiteral typeLiteral = new TypeLiteral();
            typeLiteral.setType(CatscriptType.getListType(CatscriptType.OBJECT));
            typeLiteral.setToken(tokens.consumeToken());
            if (tokens.matchAndConsume(LESS)) {
                TypeLiteral componentType = parseTypeLiteral();
                typeLiteral.setType(CatscriptType.getListType(componentType.getType()));
                require(GREATER, typeLiteral);
            }
            return typeLiteral;
        }
        TypeLiteral typeLiteral = new TypeLiteral();
        typeLiteral.setType(CatscriptType.OBJECT);
        typeLiteral.setToken(tokens.consumeToken());
        typeLiteral.addError(ErrorType.BAD_TYPE_NAME);
        return typeLiteral;
    }
    // ==== DONE ====
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
    // ==== DONE ====
    private Statement parseReturnStatement() {
        if (tokens.match(RETURN)) {
            ReturnStatement returnStatement = new ReturnStatement();
            returnStatement.setFunctionDefinition(this.currentFunctionDefinition);
            returnStatement.setStart(tokens.consumeToken());
            if (!tokens.match(RIGHT_BRACE)) {
                final Expression expression = parseExpression();
                returnStatement.setExpression(expression);
            }
            returnStatement.setEnd(tokens.lastToken());
            return returnStatement;
        }
        return null;
    }
    // ==== DONE ====
    private Statement parseVarStatement() {
        if (tokens.match(VAR)) {
            VariableStatement varStatement = new VariableStatement();
            varStatement.setStart(tokens.consumeToken());
            Token name = require(IDENTIFIER, varStatement);
            varStatement.setVariableName(name.getStringValue());
            if (tokens.match(COLON)) {
                tokens.consumeToken();
                TypeLiteral typeLiteral = parseTypeLiteral();
                varStatement.setExplicitType(typeLiteral.getType());
            }
            require(EQUAL, varStatement);
            varStatement.setExpression(parseExpression());
            varStatement.setEnd(tokens.lastToken());
            return varStatement;
        }
        return null;
    }

    //============================================================
    //  Expressions
    //============================================================
    private Expression parseExpression() {
        return parseEqualityExpression();
    }
    // highest in the logic tree
    // uses ComparisonExpression from below.
    // all logic trickles down from here and recursively works its way up
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
    // fifth in the logic tree
    // uses AdditiveExpression below
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
    // fourth in the logic tree
    // uses FactorExpression below
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
    // third in the logic tree for parsing
    // uses UnaryExpression from below
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
    // second in the bottom of the logic tree for parsing
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
    // bottom of the logic tree for parsing
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
        /*Handles test cases
        * parseIdentifierExpression()
        * parseFunctionCallExpression()
        * parseNoArgFunctionCallExpression()
        * parseUnterminatedFunctionCallExpression()*/
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
        } else if (tokens.match(TRUE)) { // handles parseTrueExpression() test case
            Token booToken = tokens.consumeToken();
            BooleanLiteralExpression booExpression = new BooleanLiteralExpression(true);
            booExpression.setToken(booToken);
            return booExpression;
        } else if (tokens.match(FALSE)) { // handles parseFalseExpression() test case
            Token booToken = tokens.consumeToken();
            BooleanLiteralExpression booExpression = new BooleanLiteralExpression(false);
            booExpression.setToken(booToken);
            return booExpression;
        } else if (tokens.match(NULL)) { // handles parseNullExpression() test case
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExpression = new NullLiteralExpression();
            nullExpression.setToken(nullToken);
            return nullExpression;
        /* the below else if statement handles test cases
        * parseListLiteralExpression()
        * parseEmptyListLiteralExpression()
        * parseUnterminatedListLiteralExpression()*/
        } else if (tokens.match(LEFT_BRACKET)) {
            Token llToken = tokens.consumeToken(); // list literal variable
            List<Expression> listExpression = new LinkedList<>(); // linked list for listExpressions
            ErrorType error = null; // error variable placeholder to handle unterminated/empty
            while (!tokens.match(RIGHT_BRACKET)) { // as long as the end bracket isn't hit, continue looping
                if (tokens.matchAndConsume(COMMA)) { // consume commas & continue
                    continue;
                } else if(tokens.match(EOF)){ // END OF FILE get correct errortype & break out
                    error = (ErrorType.UNTERMINATED_LIST);
                    break;
                } else { // parse expressions if above conditions aren't met and add them to linked list
                    Expression expression = parseExpression();
                    listExpression.add(expression);
                }
            }
            if(tokens.match(RIGHT_BRACKET)){
                tokens.consumeToken();
            }
            ListLiteralExpression llExpression = new ListLiteralExpression(listExpression);
            llExpression.setStart(llToken); // set start | handles out of bounds issues
            llExpression.setEnd(tokens.getCurrentToken()); // set end | handles out of bounds issues
            tokens.matchAndConsume(RIGHT_BRACKET); // consume right bracket after all above logic is finished
            if(error != null){ // error handle if value is no longer null
                llExpression.addError(error);
            }
            return llExpression;
        } else if (tokens.match(LEFT_PAREN)) { // handles additiveExpressionsCanBeParenthesized() test
            tokens.consumeToken();
            ParenthesizedExpression parExpression = new ParenthesizedExpression(parseExpression());
            //Token operator = tokens.consumeToken();
            //StringLiteralExpression stringExpression = new StringLiteralExpression(operator.getStringValue());
            if (tokens.match(RIGHT_PAREN)){
                tokens.consumeToken();
                return parExpression;
            } else {
                return null;
            }
            //return new ParenthesizedExpression(stringExpression);
        } else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            //added from lecture .setToken()
            syntaxErrorExpression.setToken(tokens.consumeToken());
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
