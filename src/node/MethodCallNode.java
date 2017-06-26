package node;

import exception.TypeException;
import exception.UndeclaredMethodException;
import exception.UndeclaredVarException;
import grammar.FOOLParser;
import main.SemanticError;
import symbol_table.Environment;
import symbol_table.SymbolTableEntry;
import type.ClassType;
import type.FunType;
import type.InstanceType;
import type.Type;
import util.CodegenUtils;

import java.util.ArrayList;

public class MethodCallNode extends FunCallNode {

    private String classId;
    private int objectOffset;
    private int objectNestingLevel;
    private int methodOffset;
    private int nestinglevel;

    private String objectId;
    private String methodId;
    private ArgumentsNode args;
    private Type methodType;

    public MethodCallNode(FOOLParser.MethodExpContext ctx, String objectId, String methodId, ArgumentsNode args) {
        super(ctx.funcall(), methodId, args.getChilds());
        this.objectId = objectId;
        this.methodId = methodId;
        this.args = args;
    }

    @Override
    public ArrayList<SemanticError> checkSemantics(Environment env) {
        ArrayList<SemanticError> res = new ArrayList<>();

        try {

            ClassType classType = null;
            if (objectId.equals("this")) {
                Type objectType = env.getLatestEntry().getType();
                // TODO object offset e nestinglevel
                if (objectType instanceof ClassType) {
                    classType = (ClassType) objectType;
                } else {
                    res.add(new SemanticError("Can't call this outside a class"));
                }
            } else {
                SymbolTableEntry objectSEntry = env.getLatestEntryOf(objectId);
                Type objectType = objectSEntry.getType();
                this.objectOffset = objectSEntry.getOffset();
                this.objectNestingLevel = objectSEntry.getNestinglevel();
                this.nestinglevel = env.getNestingLevel();
                // Controllo che il metodo sia stato chiamato su un oggetto
                if (objectType instanceof InstanceType) {
                    classType = ((InstanceType) objectType).getClassType();
                } else {
                    res.add(new SemanticError("Method " + methodId + " called on a non-object type"));
                }
            }

            // Se il metodo viene chiamato su this, vuol dire che stiamo facendo la semantic analysis
            // della classe, quindi prendo l'ultima entry aggiunta alla symbol table
            SymbolTableEntry classEntry = objectId.equals("this")
                    ? env.getLatestEntry()
                    : env.getLatestEntryOf(classType.getClassID());

            ClassType objectClass = (ClassType) classEntry.getType();
            this.classId = objectClass.getClassID();
            this.methodOffset = objectClass.getOffsetOfMethod(methodId);
            this.methodType = objectClass.getTypeOfMethod(methodId);
            // Controllo che il metodo esista all'interno della classe
            if (this.methodType == null) {
                res.add(new SemanticError("Object " + objectId + " doesn't have a " + methodId + " method."));
            }

        } catch (UndeclaredVarException | UndeclaredMethodException e) {
            res.add(new SemanticError(e.getMessage()));
        }

        args.checkSemantics(env);

        return res;
    }

    @Override
    public Type type() throws TypeException {
        FunType t = (FunType) this.methodType;

        ArrayList<Type> p = t.getParams();
        if (!(p.size() == params.size())) {
            throw new TypeException("Wrong number of parameters in the invocation of " + id, ctx);
        }
        for (int i = 0; i < params.size(); i++)
            if (!params.get(i).type().isSubTypeOf(p.get(i))) {
                throw new TypeException("Wrong type for " + (i + 1) + "-th parameter in the invocation of " + id, ctx);
            }
        return t.getReturnType();
    }

    @Override
    public String codeGeneration() {
        StringBuilder parCode = new StringBuilder();
        for (int i = params.size() - 1; i >= 0; i--)
            parCode.append(params.get(i).codeGeneration());

        StringBuilder getAR = new StringBuilder();
        for (int i = 0; i < nestinglevel - objectNestingLevel; i++)
            getAR.append("lw\n");

        return "lfp\n" + //CL
                parCode +
                "push " + objectOffset + "\n" +
                "lfp\n" + getAR +
                "add\n" +
                "lw\n" +
                CodegenUtils.getDispatchTablePointer(this.classId) + "\n" +
                "push " + methodOffset + "\n" +
                "add" + "\n" +
                "lw\n" +
                "js\n";
    }

    @Override
    public String toString() {
        return objectId + "." + methodId + "()";
    }

}
