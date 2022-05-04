package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.SymbolTable;

import java.util.LinkedList;
import java.util.List;

public class ListLiteralExpression extends Expression {
    List<Expression> values;
    private CatscriptType type;

    public ListLiteralExpression(List<Expression> values) {
        this.values = new LinkedList<>();
        for (Expression value : values) {
            this.values.add(addChild(value));
        }
    }

    public List<Expression> getValues() {
        return values;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        for (Expression value : values) {
            value.validate(symbolTable);
        }
        if (values.size() > 0) {
            // inferred type set to NULL since we can assign NULL to anything.
            CatscriptType inferType = CatscriptType.NULL;
            for (Expression value : values) {
                // get the value of the componenentType
                CatscriptType componentType = value.getType();
                // not assignable to the NULL type.
                if (!inferType.isAssignableFrom(componentType)) {
                    if (inferType == CatscriptType.NULL) {
                        inferType = componentType;
                    } else {
                        inferType = CatscriptType.OBJECT;
                    }
                }
            }
            type = CatscriptType.getListType(values.get(0).getType());
        } else {
            type = CatscriptType.getListType(CatscriptType.OBJECT);
        }
    }

    @Override
    public CatscriptType getType() {
        return type;
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        // passess literalExpressionsEvaluatesProperly() test
        LinkedList list = new LinkedList();
        for (Expression value : values) {
            list.add(value.evaluate(runtime));
        }
        return list;
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        super.compile(code);
    }


}
