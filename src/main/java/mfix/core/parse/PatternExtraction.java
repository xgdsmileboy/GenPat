/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package mfix.core.parse;

import mfix.common.util.LevelLogger;
import mfix.core.parse.node.MethDecl;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.*;
import mfix.core.parse.node.stmt.*;
import mfix.core.parse.relation.*;
import mfix.core.parse.relation.op.BinaryOp;
import mfix.core.parse.relation.op.CopCond;
import mfix.core.parse.relation.op.CopInstof;
import mfix.core.parse.relation.op.OpArrayAcc;
import mfix.core.parse.relation.op.OperationFactory;
import mfix.core.parse.relation.struct.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author: Jiajun
 * @date: 2018/12/4
 */
public class PatternExtraction {

    private static PatternExtraction patternExtraction = new PatternExtraction();
    private final List<Relation> emptyRelations = new LinkedList<>();
    private PatternExtraction(){}

    public static Pattern extract(Node oldNode, Node newNode) {
        if(oldNode == null || newNode == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        if(oldNode.toSrcString().toString().equals(newNode.toSrcString().toString())) {
            return null;
        }
        Pattern pattern = new Pattern();
        pattern.setOldRelationFlag(true);
        patternExtraction.process(oldNode, pattern, new Scope(null));
        pattern.setOldRelationFlag(false);
        patternExtraction.process(newNode, pattern, new Scope(null));
        return pattern;
    }

    private void processChild(RStruct structure, int index, Node node, Pattern pattern, Scope scope) {
        if(node != null) {
            List<Relation> children = process(node, pattern, scope);
            for(Relation r : children) {
                structure.addDependency(scope.getDefines(r));
                RKid kid = new RKid(structure);
                kid.setChild(r);
                kid.setIndex(index);
                pattern.addRelation(kid);
            }
        }
    }

    private void processChild(RStruct structure, int index, List<Node> nodes, Pattern pattern, Scope scope) {
        for(Node child : nodes) {
            List<Relation> children = process(child, pattern, scope);
            for(Relation r : children) {
                structure.addDependency(scope.getDefines(r));
                RKid kid = new RKid(structure);
                kid.setChild(r);
                kid.setIndex(index);
                pattern.addRelation(kid);
            }
        }
    }

    private void processArg(ROpt operation, int index, Node node, Pattern pattern, Scope scope) {
        List<Relation> children = process(node, pattern, scope);
        assert children.size() == 1;
        RArg arg = new RArg(operation);
        arg.setIndex(index);
        arg.setArgument((ObjRelation) children.get(0));
        pattern.addRelation(arg);
        operation.addDependency(scope.getDefines(children.get(0)));
    }

    private void processArg(RMcall mcall, int index, Node node, Pattern pattern, Scope scope) {
        List<Relation> children = process(node, pattern, scope);
        assert children.size() == 1;
        RArg arg = new RArg(mcall);
        arg.setIndex(index);
        arg.setArgument((ObjRelation) children.get(0));
        pattern.addRelation(arg);
        mcall.addDependency(scope.getDefines(children.get(0)));
    }

    public List<Relation> visit(AnonymousClassDecl node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(AssertStmt node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(Blk node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        Scope newScope = new Scope(scope);
        for (Node child : node.getAllChildren()) {
            result.addAll(process(child, pattern, newScope));
        }
        return result;
    }

    public List<Relation> visit(BreakStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSBreak());
        result.add(struct);
        pattern.addRelation(struct);
        processChild(struct, 0, node.getAllChildren(), pattern, scope);
        return result;
    }

    public List<Relation> visit(CatClause node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSCatch());
        result.add(struct);
        pattern.addRelation(struct);
        Scope newScope = new Scope(scope);
        processChild(struct, 0, node.getAllChildren(), pattern, newScope);
        return result;
    }

    public List<Relation> visit(ConstructorInv node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RMcall constructor = new RMcall(RMcall.MCallType.INIT_CALL);
        constructor.setMethodName(node.getClassStr());
        result.add(constructor);

        int index = 1;
        for(Relation r : process(node.getArguments(), pattern, scope)) {
            RArg arg = new RArg(constructor);
            arg.setIndex(index ++);
            arg.setArgument((ObjRelation) r);
            pattern.addRelation(arg);
            constructor.addDependency(scope.getDefines(r));
        }

        pattern.addRelation(constructor);
        return result;
    }

    public List<Relation> visit(ContinueStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSContinue());
        result.add(struct);
        pattern.addRelation(struct);
        processChild(struct, 0, node.getAllChildren(), pattern, scope);

        return result;
    }

    public List<Relation> visit(DoStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSDo());
        result.add(struct);
        pattern.addRelation(struct);
        Scope newScope = new Scope(scope);
        processChild(struct, RSDo.POS_CHILD_COND, node.getExpression(), pattern, newScope);
        processChild(struct, RSDo.POS_CHILD_BODY, node.getBody(), pattern, new Scope(newScope));

        return result;
    }

    public List<Relation> visit(EmptyStmt node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(EnhancedForStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSEnhancedFor());
        result.add(struct);
        pattern.addRelation(struct);

        Scope newScope = new Scope(scope);
        processChild(struct, RSEnhancedFor.POS_CHILD_PRAM, node.getParameter(), pattern, newScope);
        processChild(struct, RSEnhancedFor.POS_CHILD_EXPR, node.getExpression(), pattern, newScope);
        processChild(struct, RSEnhancedFor.POS_CHILD_BODY, node.getBody(), pattern, new Scope(newScope));

        return result;
    }

    public List<Relation> visit(ExpressionStmt node, Pattern pattern, Scope scope) {
        return process(node.getExpression(), pattern, scope);
    }

    public List<Relation> visit(ForStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSFor());
        result.add(struct);
        pattern.addRelation(struct);
        Scope newScope = new Scope(scope);
        processChild(struct, RSFor.POS_CHILD_INIT, node.getInitializer(), pattern, newScope);
        processChild(struct, RSFor.POS_CHILD_COND, node.getCondition(), pattern, newScope);
        processChild(struct, RSFor.POS_CHILD_UPD, node.getUpdaters(), pattern, newScope);
        processChild(struct, RSFor.POS_CHILD_BODY, node.getBody(), pattern, new Scope(newScope));

        return result;
    }

    public List<Relation> visit(IfStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSIf());
        result.add(struct);
        pattern.addRelation(struct);
        Scope newScope = new Scope(scope);
        processChild(struct, RSIf.POS_CHILD_COND, node.getCondition(), pattern, newScope);
        processChild(struct, RSIf.POS_CHILD_THEN, node.getThen(), pattern, new Scope(newScope));
        processChild(struct, RSIf.POS_CHILD_ELSE, node.getElse(), pattern, new Scope(newScope));

        return result;
    }

    public List<Relation> visit(LabeledStmt node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(ReturnStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSRet());
        result.add(struct);
        pattern.addRelation(struct);

        processChild(struct, 0, node.getExpression(), pattern, scope);

        return result;
    }

    public List<Relation> visit(SuperConstructorInv node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.SUPER_INIT_CALL);
        result.add(mcall);
        List<Relation> relations = process(node.getExpression(), pattern, scope);
        if(relations.size() > 0) mcall.setReciever((ObjRelation) relations.get(0));

        relations = process(node.getArgument(), pattern, scope);
        int index = 1;
        for(Relation r : relations) {
            RArg arg = new RArg(mcall);
            arg.setIndex(index ++);
            arg.setArgument((ObjRelation) r);
            mcall.addDependency(scope.getDefines(r));
        }

        pattern.addRelation(mcall);
        return result;
    }

    public List<Relation> visit(SwCase node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        return result;
    }

    public List<Relation> visit(SwitchStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct ss = new RStruct(new RSwitchStmt());
        result.add(ss);
        pattern.addRelation(ss);
        Scope newScope = new Scope(scope);
        processChild(ss, RSwitchStmt.POS_CHILD_VAR, node.getExpression(), pattern, newScope);

        RStruct swcase = null;
        Scope childScope = newScope;
        for(Stmt stmt : node.getStatements()) {
            if(stmt instanceof  SwCase) {
                SwCase ca = (SwCase) stmt;
                swcase = new RStruct(new RSwCase());
                pattern.addRelation(swcase);
                childScope = new Scope(newScope);
                processChild(swcase, RSwCase.POS_CHILD_CONST, ca.getExpression(), pattern, childScope);

                RKid kid = new RKid(ss);
                kid.setIndex(RSwitchStmt.POS_CHILD_CASE);
                kid.setChild(swcase);
                pattern.addRelation(kid);
            } else {
                List<Relation> relations = process(stmt, pattern, childScope);
                for(Relation r : relations) {
                    RKid kid = new RKid(swcase);
                    kid.setChild(r);
                    pattern.addRelation(kid);
                    swcase.addDependency(childScope.getDefines(r));
                }
            }
        }

        return result;
    }

    public List<Relation> visit(SynchronizedStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSync());
        result.add(struct);
        pattern.addRelation(struct);
        Scope newScope = new Scope(scope);
        processChild(struct, RSync.POS_CHILD_RES, node.getExpression(), pattern, newScope);
        processChild(struct, RSync.POS_CHILD_BODY, node.getBody(), pattern, new Scope(newScope));

        return result;
    }

    public List<Relation> visit(ThrowStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSThrow());
        result.add(struct);
        pattern.addRelation(struct);
        processChild(struct, 0, node.getExpression(), pattern, scope);
        return result;
    }

    public List<Relation> visit(TryStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSTry());
        result.add(struct);
        pattern.addRelation(struct);
        Scope newScope = new Scope(scope);
        if(node.getResource() != null && !node.getResource().isEmpty()) {
            for(VarDeclarationExpr expr : node.getResource()) {
                processChild(struct, RSTry.POS_CHILD_RES, expr, pattern, newScope);
            }
        }
        processChild(struct, RSTry.POS_CHILD_BODY, node.getBody(), pattern, newScope);
        if(node.getCatches() != null) {
            for (CatClause cc : node.getCatches()) {
                processChild(struct, RSTry.POS_CHILD_CATCH, cc, pattern, scope);
            }
        }
        processChild(struct, RSTry.POS_CHILD_FINALLY, node.getFinally(), pattern, scope);

        return result;
    }

    public List<Relation> visit(TypeDeclarationStmt node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(VarDeclarationStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        for(Vdf vdf : node.getFragments()) {
            RDef vardef = new RDef();
            vardef.setModifiers(node.getModifier());
            vardef.setName(vdf.getName());
            String typeStr = node.getDeclType().typeStr();
            for(int i = 0; i < vdf.getDimension(); i++) {
                typeStr += "[]";
            }
            vardef.setTypeStr(typeStr);
            
            if(vdf.getExpression() != null) {
                List<Relation> relations = process(vdf.getExpression(), pattern, scope);
                if(!relations.isEmpty()) {
                    vardef.setInitializer((ObjRelation) relations.get(0));
                }
            }
            scope.addDefine(vardef, vardef);
            result.add(vardef);
            pattern.addRelation(vardef);
        }
        return result;
    }

    public List<Relation> visit(WhileStmt node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSWhile());
        result.add(struct);
        pattern.addRelation(struct);
        Scope newScope = new Scope(scope);
        processChild(struct, RSWhile.POS_CHILD_COND, node.getExpression(), pattern, newScope);
        processChild(struct, RSWhile.POS_CHILD_BODY, node.getBody(), pattern, new Scope(newScope));
        return result;
    }

    public List<Relation> visit(MethDecl node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSMethod());
        result.add(struct);
        pattern.addRelation(struct);

        for(Expr expr : node.getArguments()) {
            processChild(struct, RSMethod.POS_CHILD_PARAM, expr, pattern, scope);
        }

        processChild(struct, RSMethod.POS_CHILD_BODY, node.getBody(), pattern, scope);

        return result;
    }

    // expression bellow
    public List<Relation> visit(AryAcc node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        ROpt opt = new ROpt(new OpArrayAcc());
        result.add(opt);
        pattern.addRelation(opt);
        processArg(opt, OpArrayAcc.POSITION_LHS, node.getArray(), pattern, scope);
        processArg(opt, OpArrayAcc.POSITION_RHS, node.getIndex(), pattern, scope);
        return result;
    }

    public List<Relation> visit(AryCreation node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.NEW_ARRAY);
        mcall.setMethodName(node.getElementType().typeStr());
        result.add(mcall);
        int index = 1;
        for(Expr expr : node.getDimention()) {
            processArg(mcall, index++, expr, pattern, scope);
        }
        pattern.addRelation(mcall);
        return result;
    }

    //TODO: should parse array initializer
    public List<Relation> visit(AryInitializer node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(Assign node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        String op = node.getOperator().getOperatorStr();
        if(op.length() > 1) {
            String computeOp = op.substring(0, op.length() - 1);
            ROpt opt = new ROpt(OperationFactory.createOperation(computeOp));
            pattern.addRelation(opt);

            processArg(opt, BinaryOp.POSITION_LHS, node.getLhs(), pattern, scope);
            processArg(opt, BinaryOp.POSITION_RHS, node.getRhs(), pattern, scope);

            List<Relation> relations = process(node.getLhs(), pattern, scope);
            RAssign assign = new RAssign((ObjRelation) relations.get(0));
            scope.addDefine((RDef)relations.get(0), assign);
            assign.setRhs(opt);
            pattern.addRelation(assign);
            result.add(assign);
        } else {
            List<Relation> relations = process(node.getRhs(), pattern, scope);
            RAssign assign = new RAssign(null);
            assign.setRhs((ObjRelation) relations.get(0));
            relations = process(node.getLhs(), pattern, scope);
            assign.setLhs((ObjRelation) relations.get(0));
            scope.addDefine((RDef) relations.get(0), assign);

            pattern.addRelation(assign);
            result.add(assign);
        }
        return result;
    }

    public List<Relation> visit(AssignOperator node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(BoolLiteral node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualDef = new RVDef();
        virtualDef.setValue(node.getValue());
        virtualDef.setTypeStr("boolean");
        pattern.addRelation(virtualDef);
        result.add(virtualDef);
        return result;
    }

    public List<Relation> visit(CastExpr node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.CAST);
        mcall.setMethodName(node.getCastType().typeStr());
        result.add(mcall);

        processArg(mcall, 0, node.getExpresion(), pattern, scope);
        pattern.addRelation(mcall);

        return result;
    }

    public List<Relation> visit(CharLiteral node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setTypeStr("char");
        virtualdef.setName(node.getStringValue());
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(ClassInstCreation node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.INIT_CALL);
        if(node.getExpression() != null) {
            List<Relation> relations = process(node.getExpression(), pattern, scope);
            mcall.setReciever((ObjRelation) relations.get(0));
        }
        mcall.setMethodName(node.getClassType().typeStr());
        // TODO : currently we do not consider anonymous class declaration
        result.add(mcall);
        pattern.addRelation(mcall);

        int index = 1;
        for(Expr expr : node.getArguments().getExpr()) {
            processArg(mcall, index++, expr, pattern, scope);
        }

        return result;
    }

    public List<Relation> visit(Comment node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(ConditionalExpr node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        ROpt opt = new ROpt(new CopCond());
        result.add(opt);
        pattern.addRelation(opt);

        processArg(opt, CopCond.POSITION_CONDITION, node.getCondition(), pattern, scope);
        processArg(opt, CopCond.POSITION_THEN, node.getfirst(), pattern, scope);
        processArg(opt, CopCond.POSITION_ELSE, node.getSecond(), pattern, scope);

        return result;
    }

    public List<Relation> visit(CreationRef node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(DoubleLiteral node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualDef = new RVDef();
        virtualDef.setTypeStr("double");
        virtualDef.setValue(node.getValue());
        pattern.addRelation(virtualDef);
        result.add(virtualDef);
        return result;
    }

    public List<Relation> visit(ExpressionMethodRef node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(ExprList node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        for(Expr expr : node.getExpr()) {
            result.addAll(process(expr, pattern, scope));
        }
        return result;
    }

    public List<Relation> visit(FieldAcc node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        String name;
        if(node.getExpression() != null) {
            name = node.getExpression().toString() + "." + node.getIdentifier().getName();
        } else {
            name = "this." + node.getIdentifier().getName();
        }
        RDef virtualDef = pattern.getVarDefine(name);
        if(virtualDef == null) {
            virtualDef = new RVDef();
            virtualDef.setName(name);
            virtualDef.setTypeStr(node.getTypeString());
            pattern.addRelation(virtualDef);
            scope.addDefine(virtualDef, virtualDef);
        }
        result.add(virtualDef);
        return result;
    }

    public List<Relation> visit(FloatLiteral node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setTypeStr("float");
        virtualdef.setValue(node.getValue());
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(InfixExpr node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        ROpt opt = new ROpt(OperationFactory.createOperation(node.getOperator().getOperatorStr()));
        result.add(opt);
        pattern.addRelation(opt);

        processArg(opt, BinaryOp.POSITION_LHS, node.getLhs(), pattern, scope);
        processArg(opt, BinaryOp.POSITION_RHS, node.getRhs(), pattern, scope);

        return result;
    }

    public List<Relation> visit(InfixOperator node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(InstanceofExpr node, Pattern pattern, Scope scope) {
        List<Relation> relations = new LinkedList<>();
        ROpt opt = new ROpt(new CopInstof());
        relations.add(opt);
        pattern.addRelation(opt);

        processArg(opt, CopInstof.POSITION_LHS, node.getExpression(), pattern, scope);
        processArg(opt, CopInstof.POSITION_RHS, node.getInstanceofType(), pattern, scope);

        return relations;
    }

    public List<Relation> visit(IntLiteral node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setTypeStr("int");
        virtualdef.setValue(node.getValue());
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(LambdaExpr node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(LongLiteral node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setTypeStr("long");
        virtualdef.setValue(node.getValue());
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(MethodInv node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.NORM_MCALL);
        List<Relation> relations = process(node.getExpression(), pattern, scope);
        if(relations.size() > 0) {
            mcall.setReciever((ObjRelation) relations.get(0));
            mcall.addDependency(scope.getDefines(relations.get(0)));
        }
        mcall.setMethodName(node.getName().getName());
        int index = 1;
        for(Expr expr : node.getArguments().getExpr()) {
            processArg(mcall, index++, expr, pattern, scope);
        }
        result.add(mcall);
        pattern.addRelation(mcall);
        return result;
    }

    public List<Relation> visit(MethodRef node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(MType node, Pattern pattern, Scope scope) {
        List<Relation> relations = new LinkedList<>();
        RVDef def = new RVDef();
        def.setValue(node.typeStr());
        def.setTypeStr(node.typeStr());
        pattern.addRelation(def);
        relations.add(def);
        return relations;
    }

    public List<Relation> visit(NillLiteral node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setValue("null");
        virtualdef.setTypeStr(null);
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(NumLiteral node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        // if failed to parse the value of a number
        // set double type as default (0.0)
        // this should not happen
        virtualdef.setTypeStr("double");
        virtualdef.setValue(0.0);
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(Operator node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(ParenthesiszedExpr node, Pattern pattern, Scope scope) {
        return process(node.getExpression(), pattern, scope);
    }

    public List<Relation> visit(PostfixExpr node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        ROpt opt = new ROpt(OperationFactory.createOperation(node.getOperator().getOperatorStr()));
        result.add(opt);
        pattern.addRelation(opt);

        processArg(opt, BinaryOp.POSITION_LHS, node.getExpression(), pattern, scope);

        return result;
    }

    public List<Relation> visit(PostOperator node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(PrefixExpr node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        ROpt opt = new ROpt(OperationFactory.createOperation(node.getOperator().getOperatorStr()));
        result.add(opt);
        pattern.addRelation(opt);
        processArg(opt, BinaryOp.POSITION_RHS, node.getExpression(), pattern, scope);
        return result;
    }

    public List<Relation> visit(PrefixOperator node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(QName node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        //TODO : to improve
        String name = node.toSrcString().toString();
        RDef def = pattern.getVarDefine(name);
        if(def == null) {
            def = new RVDef();
            def.setName(name);
            def.setTypeStr(node.getTypeString());
            pattern.addRelation(def);
            scope.addDefine(def, def);
        }
        result.add(def);
        return result;
    }

    public List<Relation> visit(SName node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        String name = node.getName();
        RDef def = pattern.getVarDefine(name);
        if(def == null) {
            def = new RVDef();
            def.setName(name);
            def.setTypeStr(node.getTypeString());
            pattern.addRelation(def);
            scope.addDefine(def, def);
        }
        result.add(def);
        return result;
    }

    public List<Relation> visit(StrLiteral node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setValue(node.toSrcString().toString());
        virtualdef.setTypeStr("String");
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(SuperFieldAcc node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        String name = "super." + node.getIdentifier();
        RDef virtualDef = pattern.getVarDefine(name);
        if(virtualDef == null) {
            virtualDef = new RVDef();
            virtualDef.setName(name);
            virtualDef.setTypeStr(node.getTypeString());
            pattern.addRelation(virtualDef);
        }
        result.add(virtualDef);
        return result;
    }

    public List<Relation> visit(SuperMethodInv node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.SUPER_MCALL);
        mcall.setMethodName(node.getMethodName().getName());
        int index = 1;
        for(Expr expr : node.getArguments().getExpr()) {
            processArg(mcall, index++, expr, pattern, scope);
        }
        pattern.addRelation(mcall);
        result.add(mcall);
        return result;
    }

    public List<Relation> visit(SuperMethodRef node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(Svd node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RDef def = new RDef();
        String typeStr = node.getDeclType().typeStr();
        def.setTypeStr(typeStr);
        def.setName(node.getName().getName());
        if(node.getInitializer() != null) {
            List<Relation> relations = process(node, pattern, scope);
            if(!relations.isEmpty()) {
                def.setInitializer((ObjRelation) relations.get(0));
            }
        }
        scope.addDefine(def, def);
        result.add(def);
        pattern.addRelation(def);
        return result;
    }

    public List<Relation> visit(ThisExpr node, Pattern pattern, Scope scope) {
        List<Relation> relations = new LinkedList<>();
        RVDef def = new RVDef();
        def.setName("this");
        def.setTypeStr(node.getTypeString());
        relations.add(def);
        pattern.addRelation(def);
        return relations;
    }

    public List<Relation> visit(TyLiteral node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setTypeStr(node.getDeclType().typeStr());
        virtualdef.setValue(node.getDeclType().typeStr());
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(TypeMethodRef node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> visit(VarDeclarationExpr node, Pattern pattern, Scope scope) {
        List<Relation> result = new LinkedList<>();

        String typeStr = node.getDeclType().typeStr();
        for(Vdf vdf : node.getFragments()) {
            RDef vardef = new RDef();
            vardef.setName(vdf.getName());
            for(int i = 0; i < vdf.getDimension(); i++) {
                typeStr += "[]";
            }
            vardef.setTypeStr(typeStr);
            if(vdf.getExpression() != null) {
                List<Relation> relations = process(vdf.getExpression(), pattern, scope);
                if(!relations.isEmpty()) {
                    vardef.setInitializer((ObjRelation) relations.get(0));
                }
            }
            scope.addDefine(vardef, vardef);
            result.add(vardef);
            pattern.addRelation(vardef);
        }
        return result;
    }

    public List<Relation> visit(Vdf node, Pattern pattern, Scope scope) {
        return emptyRelations;
    }

    public List<Relation> process(Node node, Pattern pattern, Scope scope) {
        if(node == null) return emptyRelations;
        switch (node.getNodeType()) {
            case METHDECL:
                return visit((MethDecl) node, pattern, scope);
            case EXPRLST:
                return visit((ExprList) node, pattern, scope);
            case ARRACC:
                return visit((AryAcc) node, pattern, scope);
            case ARRCREAT:
                return visit((AryCreation) node, pattern, scope);
            case ARRINIT:
                return visit((AryInitializer) node, pattern, scope);
            case ASSIGN:
                return visit((Assign) node, pattern, scope);
            case BLITERAL:
                return visit((BoolLiteral) node, pattern, scope);
            case CAST:
                return visit((CastExpr) node, pattern, scope);
            case CLITERAL:
                return visit((CharLiteral) node, pattern, scope);
            case CLASSCREATION:
                return visit((ClassInstCreation) node, pattern, scope);
            case COMMENT:
                return visit((Comment) node, pattern, scope);
            case CONDEXPR:
                return visit((ConditionalExpr) node, pattern, scope);
            case DLITERAL:
                return visit((DoubleLiteral) node, pattern, scope);
            case FIELDACC:
                return visit((FieldAcc) node, pattern, scope);
            case FLITERAL:
                return visit((FloatLiteral) node, pattern, scope);
            case INFIXEXPR:
                return visit((InfixExpr) node, pattern, scope);
            case INSTANCEOF:
                return visit((InstanceofExpr) node, pattern, scope);
            case INTLITERAL:
                return visit((IntLiteral) node, pattern, scope);
            case LLITERAL:
                return visit((LongLiteral) node, pattern, scope);
            case MINVOCATION:
                return visit((MethodInv) node, pattern, scope);
            case NULL:
                return visit((NillLiteral) node, pattern, scope);
            case NUMBER:
                return visit((NumLiteral) node, pattern, scope);
            case PARENTHESISZED:
                return visit((ParenthesiszedExpr) node, pattern, scope);
            case EXPRSTMT:
                return visit((ExpressionStmt) node, pattern, scope);
            case POSTEXPR:
                return visit((PostfixExpr) node, pattern, scope);
            case PREEXPR:
                return visit((PrefixExpr) node, pattern, scope);
            case QNAME:
                return visit((QName) node, pattern, scope);
            case SNAME:
                return visit((SName) node, pattern, scope);
            case SLITERAL:
                return visit((StrLiteral) node, pattern, scope);
            case SFIELDACC:
                return visit((SuperFieldAcc) node, pattern, scope);
            case SMINVOCATION:
                return visit((SuperMethodInv) node, pattern, scope);
            case SINGLEVARDECL:
                return visit((Svd) node, pattern, scope);
            case THIS:
                return visit((ThisExpr) node, pattern, scope);
            case TLITERAL:
                return visit((TyLiteral) node, pattern, scope);
            case VARDECLEXPR:
                return visit((VarDeclarationExpr) node, pattern, scope);
            case VARDECLFRAG:
                return visit((Vdf) node, pattern, scope);
            case ANONYMOUSCDECL:
                return visit((AnonymousClassDecl) node, pattern, scope);
            case ASSERT:
                return visit((AssertStmt) node, pattern, scope);
            case BLOCK:
                return visit((Blk) node, pattern, scope);
            case BREACK:
                return visit((BreakStmt) node, pattern, scope);
            case CONSTRUCTORINV:
                return visit((ConstructorInv) node, pattern, scope);
            case CONTINUE:
                return visit((ContinueStmt) node, pattern, scope);
            case DO:
                return visit((DoStmt) node, pattern, scope);
            case EFOR:
                return visit((EnhancedForStmt) node, pattern, scope);
            case FOR:
                return visit((ForStmt) node, pattern, scope);
            case IF:
                return visit((IfStmt) node, pattern, scope);
            case RETURN:
                return visit((ReturnStmt) node, pattern, scope);
            case SCONSTRUCTORINV:
                return visit((SuperConstructorInv) node, pattern, scope);
            case SWCASE:
                return visit((SwCase) node, pattern, scope);
            case SWSTMT:
                return visit((SwitchStmt) node, pattern, scope);
            case SYNC:
                return visit((SynchronizedStmt) node, pattern, scope);
            case THROW:
                return visit((ThrowStmt) node, pattern, scope);
            case TRY:
                return visit((TryStmt) node, pattern, scope);
            case CATCHCLAUSE:
                return visit((CatClause) node, pattern, scope);
            case TYPEDECL:
                return visit((TypeDeclarationStmt) node, pattern, scope);
            case VARDECLSTMT:
                return visit((VarDeclarationStmt) node, pattern, scope);
            case WHILE:
                return visit((WhileStmt) node, pattern, scope);
            case POSTOPERATOR:
                return visit((PostOperator) node, pattern, scope);
            case INFIXOPERATOR:
                return visit((InfixOperator) node, pattern, scope);
            case PREFIXOPERATOR:
                return visit((PrefixOperator) node, pattern, scope);
            case ASSIGNOPERATOR:
                return visit((AssignOperator) node, pattern, scope);
            case TYPE:
                return visit((MType) node, pattern, scope);
            case UNKNOWN:
                LevelLogger.warn("Found an unknown node type ! ");
            default:
                LevelLogger.fatal("Cannot parse node type ! ");
        }
        return emptyRelations;
    }

}

class Scope{
    private Scope _parent;
    private Map<RDef, ObjRelation> _varDefines = new HashMap<>();

    public Scope(Scope parent) {
        _parent = parent;
    }

    public void addDefine(RDef def, ObjRelation relation) {
        if(def == null || relation == null) return;
        _varDefines.put(def, relation);
    }

    public ObjRelation getDefines(Relation def) {
        if(def instanceof RDef) {
            return getDefines((RDef) def);
        }
        return null;
    }

    public ObjRelation getDefines(RDef def) {
        if(def == null) return null;
        ObjRelation relation = _varDefines.get(def);
        if(relation == null && _parent != null) {
            return _parent.getDefines(def);
        }
        return relation;
    }
}
