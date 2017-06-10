package ast;

import java.util.ArrayList;
import java.util.HashMap;

import lib.FOOLlib;
import util.Environment;
import util.SemanticError;

public class FunNode implements Node {

    private String id;
    private Type type;
    private ArrayList<Node> parlist = new ArrayList<Node>();
    private ArrayList<Node> declist;
    private Node body;

    public FunNode(String i, Type t) {
        id = i;
        type = t;
    }

    public void addDecBody(ArrayList<Node> d, Node b) {
        declist = d;
        body = b;
    }

    @Override
    public ArrayList<SemanticError> checkSemantics(Environment env) {
        //create result list
        ArrayList<SemanticError> res = new ArrayList<SemanticError>();

        //env.offset = -2;
        HashMap<String, SymbolTableEntry> hm = env.symTable.get(env.nestingLevel);
        SymbolTableEntry entry = new SymbolTableEntry(env.nestingLevel, env.offset--); //separo introducendo "entry"

        if (hm.put(id, entry) != null)
            res.add(new SemanticError("Fun id " + id + " already declared"));
        else {
            //creare una nuova hashmap per la symTable
            env.nestingLevel++;
            HashMap<String, SymbolTableEntry> hmn = new HashMap<String, SymbolTableEntry>();
            env.symTable.add(hmn);

            ArrayList<Type> parTypes = new ArrayList<Type>();
            int paroffset = 1;

            //check args
            for (Node a : parlist) {
                ParNode arg = (ParNode) a;
                parTypes.add(arg.getType());
                if (hmn.put(arg.getId(), new SymbolTableEntry(env.nestingLevel, arg.getType(), paroffset++)) != null)
                    System.out.println("Parameter id " + arg.getId() + " already declared");
            }

            //set func type
            entry.addType(new ArrowType(parTypes, type));

            //check semantics in the dec list
            if (declist.size() > 0) {
                env.offset = -2;
                //if there are children then check semantics for every child and save the results
                for (Node n : declist)
                    res.addAll(n.checkSemantics(env));
            }

            //check body
            res.addAll(body.checkSemantics(env));

            //close scope
            env.symTable.remove(env.nestingLevel--);
        }
        return res;
    }

    public void addPar(Node p) {
        parlist.add(p);
    }

    public String toPrint(String s) {
        String parlstr = "";
        for (Node par : parlist)
            parlstr += par.toPrint(s + "  ");
        String declstr = "";
        if (declist != null)
            for (Node dec : declist)
                declstr += dec.toPrint(s + "  ");
        return s + "Fun:" + id + "\n"
                + s + "  " + type + "\n"
                + parlstr
                + declstr
                + body.toPrint(s + "  ");
    }

    //valore di ritorno non utilizzato
    public Type typeCheck() {
        if (declist != null)
            for (Node dec : declist)
                dec.typeCheck();
        if (!(FOOLlib.isSubtype(body.typeCheck(), type))) {
            System.out.println("Wrong return type for function " + id);
            System.exit(0);
        }
        return null;
    }

    public String codeGeneration() {

        String declCode = "";
        if (declist != null) for (Node dec : declist)
            declCode += dec.codeGeneration();

        String popDecl = "";
        if (declist != null) for (Node dec : declist)
            popDecl += "pop\n";

        String popParl = "";
        for (Node dec : parlist)
            popParl += "pop\n";

        String funl = FOOLlib.freshFunLabel();
        FOOLlib.putCode(funl + ":\n" +
                "cfp\n" + //setta $fp a $sp
                "lra\n" + //inserimento return address
                declCode + //inserimento dichiarazioni locali
                body.codeGeneration() +
                "srv\n" + //pop del return value
                popDecl +
                "sra\n" + // pop del return address
                "pop\n" + // pop di AL
                popParl +
                "sfp\n" +  // setto $fp a valore del CL
                "lrv\n" + // risultato della funzione sullo stack
                "lra\n" + "js\n" // salta a $ra
        );

        return "push " + funl + "\n";
    }

    @Override
    public String toString(){
        return "Fun -> " + id + ": " + type;
    }

    @Override
    public ArrayList<Node> getChilds() {
        ArrayList<Node> childs = new ArrayList<>();

        if(parlist != null && parlist.size()>0) {
            for (Node child : parlist) {
                childs.add(child);
            }
        }

        if(declist != null && declist.size()>0) {
            for (Node child : declist) {
                childs.add(child);
            }
        }

        childs.add(body);

        return childs;
    }
}  