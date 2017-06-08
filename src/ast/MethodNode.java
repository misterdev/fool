package ast;

import util.Environment;
import util.SemanticError;

import java.util.ArrayList;

public class MethodNode implements Node {

    private String id;
    private ArrayList<Node> params = new ArrayList<Node>();
    private STentry entry;
    private int nestingLevel;

    public MethodNode(String id, ArrayList<Node> params, STentry entry, int nestingLevel) {
        this.id = id;
        this.params = params;
        this.entry = entry;
        this.nestingLevel = nestingLevel;
    }

    public MethodNode(String id, ArrayList<Node> params) {
        this.id = id;
        this.params = params;
    }

    @Override
    public String toPrint(String indent) {
        String paramsToString = params.stream()
                .map(param -> param.toPrint(indent + "  "))
                .reduce("", String::concat);
        return indent + "Call method: " + id
                + " at nesting level " + nestingLevel + "\n"
                + entry.toPrint(indent + "  ")
                + paramsToString;
    }

    @Override
    public Node typeCheck() {
        return new CallNode(id, params, entry, nestingLevel).typeCheck();
    }

    @Override
    public ArrayList<SemanticError> checkSemantics(Environment env) {
        return new CallNode(id, params, entry, nestingLevel).checkSemantics(env);
    }

    @Override
    public String codeGeneration() {
        return "";
    }

}
