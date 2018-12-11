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

import java.util.LinkedList;
import java.util.List;

/**
 * @author: Jiajun
 * @date: 2018/12/4
 */
public class PatternExtraction {

    private static PatternExtraction patternExtraction = new PatternExtraction();
    private final List<Relation> emptyRelations = new LinkedList<>();
    private PatternExtraction(){}

    public static Pattern extract(Node oldNode, Node newNode) {
        Pattern pattern = new Pattern();
        pattern.setOldRelationFlag(true);
        patternExtraction.process(oldNode, pattern);
        pattern.setOldRelationFlag(false);
        patternExtraction.process(newNode, pattern);
        return pattern;
    }

    private void processChild(RStruct structure, int index, Node node, Pattern pattern) {
        if(node != null) {
            List<Relation> children = process(node, pattern);
            for(Relation r : children) {
                RKid kid = new RKid(structure);
                kid.setChild(r);
                kid.setIndex(index);
                pattern.addRelation(kid);
            }
        }
    }

    private void processChild(RStruct structure, int index, List<Node> nodes, Pattern pattern) {
        for(Node child : nodes) {
            List<Relation> children = process(child, pattern);
            for(Relation r : children) {
                RKid kid = new RKid(structure);
                kid.setChild(r);
                kid.setIndex(index);
                pattern.addRelation(kid);
            }
        }
    }

    private void processArg(ROpt operation, int index, Node node, Pattern pattern) {
        List<Relation> children = process(node, pattern);
        assert children.size() == 1;
        RArg arg = new RArg(operation);
        arg.setIndex(index);
        arg.setArgument((ObjRelation) children.get(0));
        pattern.addRelation(arg);
    }

    private void processArg(RMcall mcall, int index, Node node, Pattern pattern) {
        List<Relation> children = process(node, pattern);
        assert children.size() == 1;
        RArg arg = new RArg(mcall);
        arg.setIndex(index);
        arg.setArgument((ObjRelation) children.get(0));
        pattern.addRelation(arg);
    }

    public List<Relation> visit(AnonymousClassDecl node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(AssertStmt node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(Blk node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        for (Node child : node.getAllChildren()) {
            result.addAll(process(child, pattern));
        }
        return result;
    }

    public List<Relation> visit(BreakStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSBreak());
        result.add(struct);
        pattern.addRelation(struct);
        processChild(struct, 0, node.getAllChildren(), pattern);

        return result;
    }

    public List<Relation> visit(CatClause node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSCatch());
        result.add(struct);
        pattern.addRelation(struct);
        processChild(struct, 0, node.getAllChildren(), pattern);

        return result;
    }

    public List<Relation> visit(ConstructorInv node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RMcall constructor = new RMcall(RMcall.MCallType.INIT_CALL);
        constructor.setMethodName(node.getClassStr());
        result.add(constructor);

        int index = 1;
        for(Relation r : process(node.getArguments(), pattern)) {
            RArg arg = new RArg(constructor);
            arg.setIndex(index ++);
            arg.setArgument((ObjRelation) r);
            pattern.addRelation(arg);
        }

        pattern.addRelation(constructor);
        return result;
    }

    public List<Relation> visit(ContinueStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSContinue());
        result.add(struct);
        pattern.addRelation(struct);
        processChild(struct, 0, node.getAllChildren(), pattern);

        return result;
    }

    public List<Relation> visit(DoStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSDo());
        result.add(struct);
        pattern.addRelation(struct);

        processChild(struct, RSDo.POS_CHILD_COND, node.getExpression(), pattern);
        processChild(struct, RSDo.POS_CHILD_BODY, node.getBody(), pattern);

        return result;
    }

    public List<Relation> visit(EmptyStmt node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(EnhancedForStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSEnhancedFor());
        result.add(struct);
        pattern.addRelation(struct);

        processChild(struct, RSEnhancedFor.POS_CHILD_PRAM, node.getParameter(), pattern);
        processChild(struct, RSEnhancedFor.POS_CHILD_EXPR, node.getExpression(), pattern);
        processChild(struct, RSEnhancedFor.POS_CHILD_BODY, node.getBody(), pattern);

        return result;
    }

    public List<Relation> visit(ExpressionStmt node, Pattern pattern) {
        return process(node.getExpression(), pattern);
    }

    public List<Relation> visit(ForStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSFor());
        result.add(struct);
        pattern.addRelation(struct);

        processChild(struct, RSFor.POS_CHILD_INIT, node.getInitializer(), pattern);
        processChild(struct, RSFor.POS_CHILD_COND, node.getCondition(), pattern);
        processChild(struct, RSFor.POS_CHILD_UPD, node.getUpdaters(), pattern);
        processChild(struct, RSFor.POS_CHILD_BODY, node.getBody(), pattern);

        return result;
    }

    public List<Relation> visit(IfStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSIf());
        result.add(struct);
        pattern.addRelation(struct);

        processChild(struct, RSIf.POS_CHILD_COND, node.getCondition(), pattern);
        processChild(struct, RSIf.POS_CHILD_THEN, node.getThen(), pattern);
        processChild(struct, RSIf.POS_CHILD_ELSE, node.getElse(), pattern);

        return result;
    }

    public List<Relation> visit(LabeledStmt node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(ReturnStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSRet());
        result.add(struct);
        pattern.addRelation(struct);

        processChild(struct, 0, node.getExpression(), pattern);

        return result;
    }

    public List<Relation> visit(SuperConstructorInv node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.SUPER_INIT_CALL);
        result.add(mcall);
        List<Relation> relations = process(node.getExpression(), pattern);
        if(relations.size() > 0) mcall.setReciever((ObjRelation) relations.get(0));

        relations = process(node.getArgument(), pattern);
        int index = 1;
        for(Relation r : relations) {
            RArg arg = new RArg(mcall);
            arg.setIndex(index ++);
            arg.setArgument((ObjRelation) r);
        }

        pattern.addRelation(mcall);
        return result;
    }

    public List<Relation> visit(SwCase node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        return result;
    }

    public List<Relation> visit(SwitchStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct ss = new RStruct(new RSwitchStmt());
        result.add(ss);
        pattern.addRelation(ss);

        processChild(ss, RSwitchStmt.POS_CHILD_VAR, node.getExpression(), pattern);

        RStruct swcase = null;
        for(Stmt stmt : node.getStatements()) {
            if(stmt instanceof  SwCase) {
                SwCase ca = (SwCase) stmt;
                swcase = new RStruct(new RSwCase());
                pattern.addRelation(swcase);

                processChild(swcase, RSwCase.POS_CHILD_CONST, ca.getExpression(), pattern);

                RKid kid = new RKid(ss);
                kid.setIndex(RSwitchStmt.POS_CHILD_CASE);
                kid.setChild(swcase);
                pattern.addRelation(kid);
            } else {
                List<Relation> relations = process(stmt, pattern);
                for(Relation r : relations) {
                    RKid kid = new RKid(swcase);
                    kid.setChild(r);
                    pattern.addRelation(kid);
                }
            }
        }

        return result;
    }

    public List<Relation> visit(SynchronizedStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSync());
        result.add(struct);
        pattern.addRelation(struct);

        processChild(struct, RSync.POS_CHILD_RES, node.getExpression(), pattern);
        processChild(struct, RSync.POS_CHILD_BODY, node.getBody(), pattern);

        return result;
    }

    public List<Relation> visit(ThrowStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSThrow());
        result.add(struct);
        pattern.addRelation(struct);
        processChild(struct, 0, node.getExpression(), pattern);
        return result;
    }

    public List<Relation> visit(TryStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSTry());
        result.add(struct);
        pattern.addRelation(struct);

        if(node.getResource() != null && !node.getResource().isEmpty()) {
            for(VarDeclarationExpr expr : node.getResource()) {
                processChild(struct, RSTry.POS_CHILD_RES, expr, pattern);
            }
        }
        processChild(struct, RSTry.POS_CHILD_BODY, node.getBody(), pattern);
        if(node.getCatches() != null) {
            for (CatClause cc : node.getCatches()) {
                processChild(struct, RSTry.POS_CHILD_CATCH, cc, pattern);
            }
        }
        processChild(struct, RSTry.POS_CHILD_FINALLY, node.getFinally(), pattern);

        return result;
    }

    public List<Relation> visit(TypeDeclarationStmt node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(VarDeclarationStmt node, Pattern pattern) {
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
                List<Relation> relations = process(vdf.getExpression(), pattern);
                if(!relations.isEmpty()) {
                    vardef.setInitializer((ObjRelation) relations.get(0));
                }
            }
            result.add(vardef);
            pattern.addRelation(vardef);
        }
        return result;
    }

    public List<Relation> visit(WhileStmt node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSWhile());
        result.add(struct);
        pattern.addRelation(struct);
        processChild(struct, RSWhile.POS_CHILD_COND, node.getExpression(), pattern);
        processChild(struct, RSWhile.POS_CHILD_BODY, node.getBody(), pattern);
        return result;
    }

    public List<Relation> visit(MethDecl node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RStruct struct = new RStruct(new RSMethod());
        result.add(struct);
        pattern.addRelation(struct);

        for(Expr expr : node.getArguments()) {
            processChild(struct, RSMethod.POS_CHILD_PARAM, expr, pattern);
        }

        processChild(struct, RSMethod.POS_CHILD_BODY, node.getBody(), pattern);

        return result;
    }

    // expression bellow
    public List<Relation> visit(AryAcc node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        ROpt opt = new ROpt(new OpArrayAcc());
        result.add(opt);
        pattern.addRelation(opt);
        processArg(opt, OpArrayAcc.POSITION_LHS, node.getArray(), pattern);
        processArg(opt, OpArrayAcc.POSITION_RHS, node.getIndex(), pattern);
        return result;
    }

    public List<Relation> visit(AryCreation node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.NEW_ARRAY);
        mcall.setMethodName(node.getElementType().typeStr());
        result.add(mcall);
        int index = 1;
        for(Expr expr : node.getDimention()) {
            processArg(mcall, index++, expr, pattern);
        }
        pattern.addRelation(mcall);
        return result;
    }

    //TODO: should parse array initializer
    public List<Relation> visit(AryInitializer node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(Assign node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        String op = node.getOperator().getOperatorStr();
        if(op.length() > 1) {
            String computeOp = op.substring(0, op.length() - 1);
            ROpt opt = new ROpt(OperationFactory.createOperation(computeOp));
            pattern.addRelation(opt);

            processArg(opt, BinaryOp.POSITION_LHS, node.getLhs(), pattern);
            processArg(opt, BinaryOp.POSITION_RHS, node.getRhs(), pattern);

            List<Relation> relations = process(node.getLhs(), pattern);
            RAssign assign = new RAssign((ObjRelation) relations.get(0));
            assign.setRhs(opt);
            pattern.addRelation(assign);
            result.add(assign);
        } else {
            List<Relation> relations = process(node.getLhs(), pattern);
            RAssign assign = new RAssign((ObjRelation) relations.get(0));
            relations = process(node.getRhs(), pattern);
            assign.setRhs((ObjRelation) relations.get(0));
            pattern.addRelation(assign);
            result.add(assign);
        }
        return result;
    }

    public List<Relation> visit(AssignOperator node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(BoolLiteral node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualDef = new RVDef();
        virtualDef.setValue(node.getValue());
        virtualDef.setTypeStr("boolean");
        pattern.addRelation(virtualDef);
        result.add(virtualDef);
        return result;
    }

    public List<Relation> visit(CastExpr node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.CAST);
        mcall.setMethodName(node.getCastType().typeStr());
        result.add(mcall);

        processArg(mcall, 0, node.getExpresion(), pattern);
        pattern.addRelation(mcall);

        return result;
    }

    public List<Relation> visit(CharLiteral node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setTypeStr("char");
        virtualdef.setName(node.getStringValue());
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(ClassInstCreation node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.INIT_CALL);
        if(node.getExpression() != null) {
            List<Relation> relations = process(node.getExpression(), pattern);
            mcall.setReciever((ObjRelation) relations.get(0));
        }
        mcall.setMethodName(node.getClassType().typeStr());
        // TODO : currently we do not consider anonymous class declaration
        result.add(mcall);
        pattern.addRelation(mcall);

        int index = 1;
        for(Expr expr : node.getArguments().getExpr()) {
            processArg(mcall, index++, expr, pattern);
        }

        return result;
    }

    public List<Relation> visit(Comment node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(ConditionalExpr node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        ROpt opt = new ROpt(new CopCond());
        result.add(opt);
        pattern.addRelation(opt);

        processArg(opt, CopCond.POSITION_CONDITION, node.getCondition(), pattern);
        processArg(opt, CopCond.POSITION_THEN, node.getfirst(), pattern);
        processArg(opt, CopCond.POSITION_ELSE, node.getSecond(), pattern);

        return result;
    }

    public List<Relation> visit(CreationRef node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(DoubleLiteral node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualDef = new RVDef();
        virtualDef.setTypeStr("double");
        virtualDef.setValue(node.getValue());
        pattern.addRelation(virtualDef);
        result.add(virtualDef);
        return result;
    }

    public List<Relation> visit(ExpressionMethodRef node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(ExprList node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        for(Expr expr : node.getExpr()) {
            result.addAll(process(expr, pattern));
        }
        return result;
    }

    public List<Relation> visit(FieldAcc node, Pattern pattern) {
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
        }
        result.add(virtualDef);
        return result;
    }

    public List<Relation> visit(FloatLiteral node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setTypeStr("float");
        virtualdef.setValue(node.getValue());
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(InfixExpr node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        ROpt opt = new ROpt(OperationFactory.createOperation(node.getOperator().getOperatorStr()));
        result.add(opt);
        pattern.addRelation(opt);

        processArg(opt, BinaryOp.POSITION_LHS, node.getLhs(), pattern);
        processArg(opt, BinaryOp.POSITION_RHS, node.getRhs(), pattern);

        return result;
    }

    public List<Relation> visit(InfixOperator node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(InstanceofExpr node, Pattern pattern) {
        List<Relation> relations = new LinkedList<>();
        ROpt opt = new ROpt(new CopInstof());
        relations.add(opt);
        pattern.addRelation(opt);

        processArg(opt, CopInstof.POSITION_LHS, node.getExpression(), pattern);
        processArg(opt, CopInstof.POSITION_RHS, node.getInstanceofType(), pattern);

        return relations;
    }

    public List<Relation> visit(IntLiteral node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setTypeStr("int");
        virtualdef.setValue(node.getValue());
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(LambdaExpr node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(LongLiteral node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setTypeStr("long");
        virtualdef.setValue(node.getValue());
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(MethodInv node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.NORM_MCALL);
        List<Relation> relations = process(node.getExpression(), pattern);
        if(relations.size() > 0) {
            mcall.setReciever((ObjRelation) relations.get(0));
        }
        mcall.setMethodName(node.getName().getName());
        int index = 1;
        for(Expr expr : node.getArguments().getExpr()) {
            processArg(mcall, index++, expr, pattern);
        }
        result.add(mcall);
        pattern.addRelation(mcall);
        return result;
    }

    public List<Relation> visit(MethodRef node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(MType node, Pattern pattern) {
        List<Relation> relations = new LinkedList<>();
        RVDef def = new RVDef();
        def.setValue(node.typeStr());
        def.setTypeStr(node.typeStr());
        pattern.addRelation(def);
        relations.add(def);
        return relations;
    }

    public List<Relation> visit(NillLiteral node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setValue("null");
        virtualdef.setTypeStr(null);
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(NumLiteral node, Pattern pattern) {
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

    public List<Relation> visit(Operator node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(ParenthesiszedExpr node, Pattern pattern) {
        return process(node.getExpression(), pattern);
    }

    public List<Relation> visit(PostfixExpr node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        ROpt opt = new ROpt(OperationFactory.createOperation(node.getOperator().getOperatorStr()));
        result.add(opt);
        pattern.addRelation(opt);

        processArg(opt, BinaryOp.POSITION_LHS, node.getExpression(), pattern);

        return result;
    }

    public List<Relation> visit(PostOperator node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(PrefixExpr node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        ROpt opt = new ROpt(OperationFactory.createOperation(node.getOperator().getOperatorStr()));
        result.add(opt);
        pattern.addRelation(opt);
        processArg(opt, BinaryOp.POSITION_RHS, node.getExpression(), pattern);
        return result;
    }

    public List<Relation> visit(PrefixOperator node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(QName node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        //TODO : to improve
        String name = node.toSrcString().toString();
        RDef def = pattern.getVarDefine(name);
        if(def == null) {
            def = new RVDef();
            def.setName(name);
            def.setTypeStr(node.getTypeString());
            pattern.addRelation(def);
        }
        result.add(def);
        return result;
    }

    public List<Relation> visit(SName node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        String name = node.getName();
        RDef def = pattern.getVarDefine(name);
        if(def == null) {
            def = new RVDef();
            def.setName(name);
            def.setTypeStr(node.getTypeString());
            pattern.addRelation(def);
        }
        result.add(def);
        return result;
    }

    public List<Relation> visit(StrLiteral node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setValue(node.toSrcString().toString());
        virtualdef.setTypeStr("String");
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(SuperFieldAcc node, Pattern pattern) {
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

    public List<Relation> visit(SuperMethodInv node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RMcall mcall = new RMcall(RMcall.MCallType.SUPER_MCALL);
        mcall.setMethodName(node.getMethodName().getName());
        int index = 1;
        for(Expr expr : node.getArguments().getExpr()) {
            processArg(mcall, index++, expr, pattern);
        }
        pattern.addRelation(mcall);
        result.add(mcall);
        return result;
    }

    public List<Relation> visit(SuperMethodRef node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(Svd node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RDef def = new RDef();
        String typeStr = node.getDeclType().typeStr();
        def.setTypeStr(typeStr);
        def.setName(node.getName().getName());
        if(node.getInitializer() != null) {
            List<Relation> relations = process(node, pattern);
            if(!relations.isEmpty()) {
                def.setInitializer((ObjRelation) relations.get(0));
            }
        }
        result.add(def);
        pattern.addRelation(def);
        return result;
    }

    public List<Relation> visit(ThisExpr node, Pattern pattern) {
        List<Relation> relations = new LinkedList<>();
        RVDef def = new RVDef();
        def.setName("this");
        def.setTypeStr(node.getTypeString());
        relations.add(def);
        pattern.addRelation(def);
        return relations;
    }

    public List<Relation> visit(TyLiteral node, Pattern pattern) {
        List<Relation> result = new LinkedList<>();
        RVDef virtualdef = new RVDef();
        virtualdef.setTypeStr(node.getDeclType().typeStr());
        virtualdef.setValue(node.getDeclType().typeStr());
        pattern.addRelation(virtualdef);
        result.add(virtualdef);
        return result;
    }

    public List<Relation> visit(TypeMethodRef node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> visit(VarDeclarationExpr node, Pattern pattern) {
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
                List<Relation> relations = process(vdf.getExpression(), pattern);
                if(!relations.isEmpty()) {
                    vardef.setInitializer((ObjRelation) relations.get(0));
                }
            }
            result.add(vardef);
            pattern.addRelation(vardef);
        }
        return result;
    }

    public List<Relation> visit(Vdf node, Pattern pattern) {
        return emptyRelations;
    }

    public List<Relation> process(Node node, Pattern pattern) {
        if(node == null) return emptyRelations;
        switch (node.getNodeType()) {
            case METHDECL:
                return visit((MethDecl) node, pattern);
            case EXPRLST:
                return visit((ExprList) node, pattern);
            case ARRACC:
                return visit((AryAcc) node, pattern);
            case ARRCREAT:
                return visit((AryCreation) node, pattern);
            case ARRINIT:
                return visit((AryInitializer) node, pattern);
            case ASSIGN:
                return visit((Assign) node, pattern);
            case BLITERAL:
                return visit((BoolLiteral) node, pattern);
            case CAST:
                return visit((CastExpr) node, pattern);
            case CLITERAL:
                return visit((CharLiteral) node, pattern);
            case CLASSCREATION:
                return visit((ClassInstCreation) node, pattern);
            case COMMENT:
                return visit((Comment) node, pattern);
            case CONDEXPR:
                return visit((ConditionalExpr) node, pattern);
            case DLITERAL:
                return visit((DoubleLiteral) node, pattern);
            case FIELDACC:
                return visit((FieldAcc) node, pattern);
            case FLITERAL:
                return visit((FloatLiteral) node, pattern);
            case INFIXEXPR:
                return visit((InfixExpr) node, pattern);
            case INSTANCEOF:
                return visit((InstanceofExpr) node, pattern);
            case INTLITERAL:
                return visit((IntLiteral) node, pattern);
            case LLITERAL:
                return visit((LongLiteral) node, pattern);
            case MINVOCATION:
                return visit((MethodInv) node, pattern);
            case NULL:
                return visit((NillLiteral) node, pattern);
            case NUMBER:
                return visit((NumLiteral) node, pattern);
            case PARENTHESISZED:
                return visit((ParenthesiszedExpr) node, pattern);
            case EXPRSTMT:
                return visit((ExpressionStmt) node, pattern);
            case POSTEXPR:
                return visit((PostfixExpr) node, pattern);
            case PREEXPR:
                return visit((PrefixExpr) node, pattern);
            case QNAME:
                return visit((QName) node, pattern);
            case SNAME:
                return visit((SName) node, pattern);
            case SLITERAL:
                return visit((StrLiteral) node, pattern);
            case SFIELDACC:
                return visit((SuperFieldAcc) node, pattern);
            case SMINVOCATION:
                return visit((SuperMethodInv) node, pattern);
            case SINGLEVARDECL:
                return visit((Svd) node, pattern);
            case THIS:
                return visit((ThisExpr) node, pattern);
            case TLITERAL:
                return visit((TyLiteral) node, pattern);
            case VARDECLEXPR:
                return visit((VarDeclarationExpr) node, pattern);
            case VARDECLFRAG:
                return visit((Vdf) node, pattern);
            case ANONYMOUSCDECL:
                return visit((AnonymousClassDecl) node, pattern);
            case ASSERT:
                return visit((AssertStmt) node, pattern);
            case BLOCK:
                return visit((Blk) node, pattern);
            case BREACK:
                return visit((BreakStmt) node, pattern);
            case CONSTRUCTORINV:
                return visit((ConstructorInv) node, pattern);
            case CONTINUE:
                return visit((ContinueStmt) node, pattern);
            case DO:
                return visit((DoStmt) node, pattern);
            case EFOR:
                return visit((EnhancedForStmt) node, pattern);
            case FOR:
                return visit((ForStmt) node, pattern);
            case IF:
                return visit((IfStmt) node, pattern);
            case RETURN:
                return visit((ReturnStmt) node, pattern);
            case SCONSTRUCTORINV:
                return visit((SuperConstructorInv) node, pattern);
            case SWCASE:
                return visit((SwCase) node, pattern);
            case SWSTMT:
                return visit((SwitchStmt) node, pattern);
            case SYNC:
                return visit((SynchronizedStmt) node, pattern);
            case THROW:
                return visit((ThrowStmt) node, pattern);
            case TRY:
                return visit((TryStmt) node, pattern);
            case CATCHCLAUSE:
                return visit((CatClause) node, pattern);
            case TYPEDECL:
                return visit((TypeDeclarationStmt) node, pattern);
            case VARDECLSTMT:
                return visit((VarDeclarationStmt) node, pattern);
            case WHILE:
                return visit((WhileStmt) node, pattern);
            case POSTOPERATOR:
                return visit((PostOperator) node, pattern);
            case INFIXOPERATOR:
                return visit((InfixOperator) node, pattern);
            case PREFIXOPERATOR:
                return visit((PrefixOperator) node, pattern);
            case ASSIGNOPERATOR:
                return visit((AssignOperator) node, pattern);
            case TYPE:
                return visit((MType) node, pattern);
            case UNKNOWN:
                LevelLogger.warn("Found an unknown node type ! ");
            default:
                LevelLogger.fatal("Cannot parse node type ! ");
        }
        return emptyRelations;
    }

}
