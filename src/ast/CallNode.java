package ast;

import lib.FOOLlib;
import util.Environment;
import util.SemanticError;

import java.util.ArrayList;

public class CallNode implements Node {

    private String id;
    private ArrayList<Node> params = new ArrayList<Node>();
    private STentry entry;
    private int nestingLevel;

    public CallNode(String id, ArrayList<Node> params, STentry entry, int nestingLevel) {
        this.id = id;
        this.params = params;
        this.entry = entry;
        this.nestingLevel = nestingLevel;
    }

    public CallNode(String id, ArrayList<Node> params) {
        this.id = id;
        this.params = params;
    }

    public String toPrint(String indent) {  //
        String paramsToString = params.stream()
                .map(param -> param.toPrint(indent + "  "))
                .reduce("", String::concat);
        return indent + "Call method: " + id
                + " at nesting level " + nestingLevel + "\n"
                + entry.toPrint(indent + "  ")
                + paramsToString;
    }

    @Override
    public ArrayList<SemanticError> checkSemantics(Environment env) {
        //create the result
        ArrayList<SemanticError> res = new ArrayList<SemanticError>();

        int j = env.nestingLevel;
        STentry tmp = null;
        while (j >= 0 && tmp == null)
            tmp = (env.symTable.get(j--)).get(id);
        if (tmp == null)
            res.add(new SemanticError("Id " + id + " not declared"));

        else {
            this.entry = tmp;
            this.nestingLevel = env.nestingLevel;

            for (Node arg : params)
                res.addAll(arg.checkSemantics(env));
        }
        return res;
    }

    public Node typeCheck() {  //
        ArrowTypeNode t = null;
        if (entry.getType() instanceof ArrowTypeNode) {
            t = (ArrowTypeNode) entry.getType();
        } else {
            System.out.println("Invocation of a non-function " + id);
            System.exit(0);
        }

        ArrayList<Node> p = t.getParList();
        if (!(p.size() == params.size())) {
            System.out.println("Wrong number of parameters in the invocation of " + id);
            System.exit(0);
        }
        for (int i = 0; i < params.size(); i++)
            if (!(FOOLlib.isSubtype((params.get(i)).typeCheck(), p.get(i)))) {
                System.out.println("Wrong type for " + (i + 1) + "-th parameter in the invocation of " + id);
                System.exit(0);
            }
        return t.getRet();
    }

    public String codeGeneration() {
        String parCode = "";
        for (int i = params.size() - 1; i >= 0; i--)
            parCode += params.get(i).codeGeneration();

        String getAR = "";
        for (int i = 0; i < nestingLevel - entry.getNestinglevel(); i++)
            getAR += "lw\n";

        return "lfp\n" + //CL
                parCode +
                "lfp\n" + getAR + //setto AL risalendo la catena statica
                // ora recupero l'indirizzo a cui saltare e lo metto sullo stack
                "push " + entry.getOffset() + "\n" + //metto offset sullo stack
                "lfp\n" + getAR + //risalgo la catena statica
                "add\n" +
                "lw\n" + //carico sullo stack il valore all'indirizzo ottenuto
                "js\n";
    }


}  