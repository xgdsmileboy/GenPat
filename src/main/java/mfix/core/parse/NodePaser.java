/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.parse;

import mfix.core.parse.node.MethDecl;
import mfix.core.parse.node.Node;
import mfix.core.parse.node.expr.ArrayAcc;
import mfix.core.parse.node.expr.ArrayCreate;
import mfix.core.parse.node.expr.ArrayInitial;
import mfix.core.parse.node.expr.Assign;
import mfix.core.parse.node.expr.AssignOperator;
import mfix.core.parse.node.expr.BoolLiteral;
import mfix.core.parse.node.expr.CastExpr;
import mfix.core.parse.node.expr.CharLiteral;
import mfix.core.parse.node.expr.ClassInstanceCreate;
import mfix.core.parse.node.expr.Comment;
import mfix.core.parse.node.expr.ConditionalExpr;
import mfix.core.parse.node.expr.CreationRef;
import mfix.core.parse.node.expr.DoubleLiteral;
import mfix.core.parse.node.expr.Expr;
import mfix.core.parse.node.expr.ExprList;
import mfix.core.parse.node.expr.ExpressionMethodRef;
import mfix.core.parse.node.expr.FieldAcc;
import mfix.core.parse.node.expr.FloatLiteral;
import mfix.core.parse.node.expr.InfixExpr;
import mfix.core.parse.node.expr.InfixOperator;
import mfix.core.parse.node.expr.InstanceofExpr;
import mfix.core.parse.node.expr.IntLiteral;
import mfix.core.parse.node.expr.Label;
import mfix.core.parse.node.expr.LambdaExpr;
import mfix.core.parse.node.expr.LongLiteral;
import mfix.core.parse.node.expr.MType;
import mfix.core.parse.node.expr.MethodInv;
import mfix.core.parse.node.expr.MethodRef;
import mfix.core.parse.node.expr.NillLiteral;
import mfix.core.parse.node.expr.NumLiteral;
import mfix.core.parse.node.expr.ParenthesiszedExpr;
import mfix.core.parse.node.expr.PostOperator;
import mfix.core.parse.node.expr.PostfixExpr;
import mfix.core.parse.node.expr.PrefixExpr;
import mfix.core.parse.node.expr.PrefixOperator;
import mfix.core.parse.node.expr.QName;
import mfix.core.parse.node.expr.SName;
import mfix.core.parse.node.expr.StrLiteral;
import mfix.core.parse.node.expr.SuperFieldAcc;
import mfix.core.parse.node.expr.SuperMethodInv;
import mfix.core.parse.node.expr.SuperMethodRef;
import mfix.core.parse.node.expr.Svd;
import mfix.core.parse.node.expr.ThisExpr;
import mfix.core.parse.node.expr.TyLiteral;
import mfix.core.parse.node.expr.TypeMethodRef;
import mfix.core.parse.node.expr.VarDeclarationExpr;
import mfix.core.parse.node.expr.Vdf;
import mfix.core.parse.node.stmt.AnonymousClassDecl;
import mfix.core.parse.node.stmt.AssertStmt;
import mfix.core.parse.node.stmt.Blk;
import mfix.core.parse.node.stmt.BreakStmt;
import mfix.core.parse.node.stmt.CatClause;
import mfix.core.parse.node.stmt.ConstructorInv;
import mfix.core.parse.node.stmt.ContinueStmt;
import mfix.core.parse.node.stmt.DoStmt;
import mfix.core.parse.node.stmt.EmptyStmt;
import mfix.core.parse.node.stmt.EnhancedForStmt;
import mfix.core.parse.node.stmt.ExpressionStmt;
import mfix.core.parse.node.stmt.ForStmt;
import mfix.core.parse.node.stmt.IfStmt;
import mfix.core.parse.node.stmt.LabeledStmt;
import mfix.core.parse.node.stmt.ReturnStmt;
import mfix.core.parse.node.stmt.Stmt;
import mfix.core.parse.node.stmt.SuperConstructorInv;
import mfix.core.parse.node.stmt.SwCase;
import mfix.core.parse.node.stmt.SwitchStmt;
import mfix.core.parse.node.stmt.SynchronizedStmt;
import mfix.core.parse.node.stmt.ThrowStmt;
import mfix.core.parse.node.stmt.TryStmt;
import mfix.core.parse.node.stmt.TypeDeclarationStmt;
import mfix.core.parse.node.stmt.VarDeclarationStmt;
import mfix.core.parse.node.stmt.WhileStmt;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;


/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class NodePaser {

    private CompilationUnit _cunit;

    public NodePaser() {

    }

    public void setCompilationUnit(CompilationUnit unit) {
        _cunit = unit;
    }

    /************************** visit start : MethodDeclaration ***********************/
    private MethDecl visit(MethodDeclaration node) {
        int start = _cunit.getLineNumber(node.getStartPosition());
        int end = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        MethDecl methDecl = new MethDecl(start, end, node);

        methDecl.setModifiers(node.modifiers());
        methDecl.setRetType(node.getReturnType2());
        SName name = (SName) process(node.getName());
        name.setParent(methDecl);
        methDecl.setName(name);
        List<Expr> params = new ArrayList<>(11);
        for (Object arg : node.parameters()) {
            Expr param = (Expr) process((ASTNode) arg);
            param.setParent(methDecl);
            params.add(param);
        }
        methDecl.setArguments(params);

        methDecl.setThrows(node.thrownExceptionTypes());

        Block body = node.getBody();
        if (body != null) {
            Blk bBlk = (Blk) process(body);
            bBlk.setParent(methDecl);
            methDecl.setBody(bBlk);
        }

        return methDecl;
    }

    /************************** visit start : Statement ***********************/
    private AssertStmt visit(AssertStatement node) {
        int start = _cunit.getLineNumber(node.getStartPosition());
        int end = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AssertStmt assertStmt = new AssertStmt(start, end, node);
        return assertStmt;
    }

    private BreakStmt visit(BreakStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        BreakStmt breakStmt = new BreakStmt(startLine, endLine, node);
        if (node.getLabel() != null) {
            SName sName = new SName(startLine, endLine, node.getLabel());
            sName.setName(node.getLabel().getFullyQualifiedName());
            sName.setParent(breakStmt);
            breakStmt.setIdentifier(sName);
        }
        return breakStmt;
    }

    private Blk visit(Block node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Blk blk = new Blk(startLine, endLine, node);
        List<Stmt> stmts = new ArrayList<>();
        for (Object object : node.statements()) {
            Stmt stmt = (Stmt) process((ASTNode) object);
            stmt.setParent(blk);
            stmts.add(stmt);
        }
        blk.setStatement(stmts);
        return blk;
    }

    private ConstructorInv visit(ConstructorInvocation node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ConstructorInv constructorInv = new ConstructorInv(startLine, endLine, node);
        ExprList exprList = new ExprList(startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr expr = (Expr) process((ASTNode) object);
            expr.setParent(exprList);
            arguments.add(expr);
        }
        exprList.setExprs(arguments);
        exprList.setParent(constructorInv);
        constructorInv.setArguments(exprList);
        return constructorInv;
    }

    private ContinueStmt visit(ContinueStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ContinueStmt continueStmt = new ContinueStmt(startLine, endLine, node);
        if (node.getLabel() != null) {
            SName sName = new SName(startLine, endLine, node.getLabel());
            sName.setName(node.getLabel().getFullyQualifiedName());
            sName.setParent(continueStmt);
            continueStmt.setIdentifier(sName);
        }
        return continueStmt;
    }

    private DoStmt visit(DoStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        DoStmt doStmt = new DoStmt(startLine, endLine, node);

        Expr expression = (Expr) process(node.getExpression());
        expression.setParent(doStmt);
        doStmt.setExpression(expression);

        Stmt stmt = (Stmt) process(node.getBody());
        stmt.setParent(doStmt);
        doStmt.setBody(stmt);

        return doStmt;
    }

    private EmptyStmt visit(EmptyStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        EmptyStmt emptyStmt = new EmptyStmt(startLine, endLine, node);
        return emptyStmt;
    }

    private EnhancedForStmt visit(EnhancedForStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        EnhancedForStmt enhancedForStmt = new EnhancedForStmt(startLine, endLine, node);

        Svd svd = (Svd) process(node.getParameter());
        svd.setParent(enhancedForStmt);
        enhancedForStmt.setParameter(svd);

        Expr expression = (Expr) process(node.getExpression());
        expression.setParent(enhancedForStmt);
        enhancedForStmt.setExpression(expression);

        Stmt body = (Stmt) process(node.getBody());
        body.setParent(enhancedForStmt);
        enhancedForStmt.setBody(body);

        return enhancedForStmt;
    }

    private ExpressionStmt visit(ExpressionStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ExpressionStmt expressionStmt = new ExpressionStmt(startLine, endLine, node);

        Expr expression = (Expr) process(node.getExpression());
        expression.setParent(expressionStmt);
        expressionStmt.setExpression(expression);

        return expressionStmt;
    }

    private ForStmt visit(ForStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ForStmt forStmt = new ForStmt(startLine, endLine, node);

        if (node.getExpression() != null) {
            Expr condition = (Expr) process(node.getExpression());
            condition.setParent(forStmt);
            forStmt.setCondition(condition);
        }

        ExprList initExprList = new ExprList(startLine, endLine, null);
        List<Expr> initializers = new ArrayList<>();
        if (!node.initializers().isEmpty()) {
            for (Object object : node.initializers()) {
                Expr initializer = (Expr) process((ASTNode) object);
                initializer.setParent(initExprList);
                initializers.add(initializer);
            }
        }
        initExprList.setExprs(initializers);
        initExprList.setParent(forStmt);
        forStmt.setInitializer(initExprList);

        ExprList exprList = new ExprList(startLine, endLine, null);
        List<Expr> updaters = new ArrayList<>();
        if (!node.updaters().isEmpty()) {
            for (Object object : node.updaters()) {
                Expr update = (Expr) process((ASTNode) object);
                update.setParent(exprList);
                updaters.add(update);
            }
        }
        exprList.setExprs(updaters);
        exprList.setParent(forStmt);
        forStmt.setUpdaters(exprList);

        Stmt body = (Stmt) process(node.getBody());
        body.setParent(forStmt);
        forStmt.setBody(body);

        return forStmt;
    }

    private IfStmt visit(IfStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        IfStmt ifStmt = new IfStmt(startLine, endLine, node);

        Expr condition = (Expr) process(node.getExpression());
        condition.setParent(ifStmt);
        ifStmt.setCondition(condition);

        Stmt then = (Stmt) process(node.getThenStatement());
        then.setParent(ifStmt);
        ifStmt.setThen(then);

        if (node.getElseStatement() != null) {
            Stmt els = (Stmt) process(node.getElseStatement());
            els.setParent(ifStmt);
            ifStmt.setElse(els);
        }

        return ifStmt;
    }

    private LabeledStmt visit(LabeledStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        LabeledStmt labeledStmt = new LabeledStmt(startLine, endLine, node);
        return labeledStmt;
    }

    private ReturnStmt visit(ReturnStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ReturnStmt returnStmt = new ReturnStmt(startLine, endLine, node);

        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression());
            expression.setParent(returnStmt);
            returnStmt.setExpression(expression);
        }

        return returnStmt;
    }

    private SuperConstructorInv visit(SuperConstructorInvocation node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperConstructorInv superConstructorInv = new SuperConstructorInv(startLine, endLine, node);

        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression());
            expression.setParent(superConstructorInv);
            superConstructorInv.setExpression(expression);
        }

        ExprList exprList = new ExprList(startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr arg = (Expr) process((ASTNode) object);
            arg.setParent(exprList);
            arguments.add(arg);
        }
        exprList.setExprs(arguments);
        exprList.setParent(superConstructorInv);
        superConstructorInv.setArguments(exprList);

        return superConstructorInv;
    }

    private SwCase visit(SwitchCase node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SwCase swCase = new SwCase(startLine, endLine, node);

        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression());
            expression.setParent(swCase);
            swCase.setExpression(expression);
        }

        return swCase;
    }

    private SwitchStmt visit(SwitchStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SwitchStmt switchStmt = new SwitchStmt(startLine, endLine, node);

        Expr expression = (Expr) process(node.getExpression());
        expression.setParent(switchStmt);
        switchStmt.setExpression(expression);

        List<Stmt> statements = new ArrayList<>();
        for (Object object : node.statements()) {
            Stmt stmt = (Stmt) process((ASTNode) object);
            stmt.setParent(switchStmt);
            statements.add(stmt);
        }
        switchStmt.setStatements(statements);

        return switchStmt;
    }

    private SynchronizedStmt visit(SynchronizedStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SynchronizedStmt synchronizedStmt = new SynchronizedStmt(startLine, endLine, node);

        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression());
            expression.setParent(synchronizedStmt);
            synchronizedStmt.setExpression(expression);
        }

        Blk blk = (Blk) process(node.getBody());
        blk.setParent(synchronizedStmt);
        synchronizedStmt.setBlock(blk);

        return synchronizedStmt;
    }

    private ThrowStmt visit(ThrowStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ThrowStmt throwStmt = new ThrowStmt(startLine, endLine, node);

        Expr expression = (Expr) process(node.getExpression());
        expression.setParent(throwStmt);
        throwStmt.setExpression(expression);

        return throwStmt;
    }

    private TryStmt visit(TryStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TryStmt tryStmt = new TryStmt(startLine, endLine, node);
        if (node.resources() != null) {
            List<VarDeclarationExpr> resourceList = new ArrayList<>(node.resources().size());
            for (Object object : node.resources()) {
                VariableDeclarationExpression resource = (VariableDeclarationExpression) object;
                VarDeclarationExpr vdExpr = (VarDeclarationExpr) process(resource);
                vdExpr.setParent(tryStmt);
                resourceList.add(vdExpr);
            }
            tryStmt.setResource(resourceList);
        }
        Blk blk = (Blk) process(node.getBody());
        blk.setParent(tryStmt);
        tryStmt.setBody(blk);

        List<CatClause> catches = new ArrayList<>(node.catchClauses().size());
        for (Object object : node.catchClauses()) {
            CatchClause catchClause = (CatchClause) object;
            CatClause catClause = (CatClause) process(catchClause);
            catClause.setParent(tryStmt);
            catches.add(catClause);
        }
        tryStmt.setCatchClause(catches);

        if (node.getFinally() != null) {
            Blk finallyBlk = (Blk) process(node.getFinally());
            finallyBlk.setParent(tryStmt);
            tryStmt.setFinallyBlock(finallyBlk);
        }

        return tryStmt;
    }

    private CatClause visit(CatchClause node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CatClause catClause = new CatClause(startLine, endLine, node);
        Svd svd = (Svd) process(node.getException());
        svd.setParent(catClause);
        catClause.setException(svd);
        Blk body = (Blk) process(node.getBody());
        body.setParent(catClause);
        catClause.setBody(body);
        return catClause;
    }

    private TypeDeclarationStmt visit(TypeDeclarationStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TypeDeclarationStmt typeDeclarationStmt = new TypeDeclarationStmt(startLine, endLine, node);
        return typeDeclarationStmt;
    }

    private VarDeclarationStmt visit(VariableDeclarationStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        VarDeclarationStmt varDeclarationStmt = new VarDeclarationStmt(startLine, endLine, node);
        String modifier = "";
        if (node.modifiers() != null && node.modifiers().size() > 0) {
            for (Object object : node.modifiers()) {
                modifier += " " + object.toString();
            }
        }
        if (modifier.length() > 0) {
            varDeclarationStmt.setModifier(modifier);
        }

        MType mType = new MType(startLine, endLine, node.getType());
        mType.setType(node.getType());
        mType.setParent(varDeclarationStmt);
        varDeclarationStmt.setDeclType(mType);

        List<Vdf> fragments = new ArrayList<>();
        for (Object object : node.fragments()) {
            Vdf vdf = (Vdf) process((ASTNode) object);
            vdf.setParent(varDeclarationStmt);
            fragments.add(vdf);
        }
        varDeclarationStmt.setFragments(fragments);

        return varDeclarationStmt;
    }

    private WhileStmt visit(WhileStatement node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        WhileStmt whileStmt = new WhileStmt(startLine, endLine, node);

        Expr expression = (Expr) process(node.getExpression());
        expression.setParent(whileStmt);
        whileStmt.setExpression(expression);

        Stmt body = (Stmt) process(node.getBody());
        body.setParent(whileStmt);
        whileStmt.setBody(body);

        return whileStmt;
    }

    /*********************** Visit Expression *********************************/
    private Comment visit(Annotation node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Comment comment = new Comment(startLine, endLine, node);
        return comment;
    }

    private ArrayAcc visit(ArrayAccess node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ArrayAcc arrayAcc = new ArrayAcc(startLine, endLine, node);

        Expr array = (Expr) process(node.getArray());
        array.setParent(arrayAcc);
        arrayAcc.setArray(array);

        Expr indexExpr = (Expr) process(node.getIndex());
        indexExpr.setParent(arrayAcc);
        arrayAcc.setIndex(indexExpr);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        arrayAcc.setType(type);

        return arrayAcc;
    }

    private ArrayCreate visit(ArrayCreation node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ArrayCreate arrayCreate = new ArrayCreate(startLine, endLine, node);
        MType mType = new MType(startLine, endLine, node.getType().getElementType());
        mType.setType(node.getType().getElementType());
        mType.setParent(arrayCreate);
        arrayCreate.setArrayType(mType);
        arrayCreate.setType(node.getType());

        List<Expr> dimension = new ArrayList<>();
        for (Object object : node.dimensions()) {
            Expr dim = (Expr) process((ASTNode) object);
            dim.setParent(arrayCreate);
            dimension.add(dim);
        }
        arrayCreate.setDimension(dimension);

        if (node.getInitializer() != null) {
            ArrayInitial arrayInitializer = (ArrayInitial) process(node.getInitializer());
            arrayInitializer.setParent(arrayCreate);
            arrayCreate.setInitializer(arrayInitializer);
        }

        return arrayCreate;
    }

    private ArrayInitial visit(ArrayInitializer node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ArrayInitial arrayInitial = new ArrayInitial(startLine, endLine, node);

        List<Expr> expressions = new ArrayList<>();
        for (Object object : node.expressions()) {
            Expr expr = (Expr) process((ASTNode) object);
            expr.setParent(arrayInitial);
            expressions.add(expr);
        }
        arrayInitial.setExpressions(expressions);

        return arrayInitial;
    }

    private Assign visit(Assignment node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Assign assign = new Assign(startLine, endLine, node);

        Expr lhs = (Expr) process(node.getLeftHandSide());
        lhs.setParent(assign);
        assign.setLeftHandSide(lhs);

        Expr rhs = (Expr) process(node.getRightHandSide());
        rhs.setParent(assign);
        assign.setRightHandSide(rhs);

        AssignOperator assignOperator = new AssignOperator(startLine, endLine, null);
        assignOperator.setOperator(node.getOperator());
        assign.setParent(assign);
        assign.setOperator(assignOperator);

        return assign;
    }

    private BoolLiteral visit(BooleanLiteral node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        BoolLiteral literal = new BoolLiteral(startLine, endLine, node);
        literal.setValue(node.booleanValue());
        AST ast = AST.newAST(AST.JLS8);
        Type type = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
        literal.setType(type);

        return literal;
    }

    private CastExpr visit(CastExpression node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CastExpr castExpr = new CastExpr(startLine, endLine, node);
        MType mType = new MType(startLine, endLine, node.getType());
        mType.setType(node.getType());
        mType.setParent(castExpr);
        castExpr.setCastType(mType);
        Expr expression = (Expr) process(node.getExpression());
        expression.setParent(castExpr);
        castExpr.setExpression(expression);
        castExpr.setType(node.getType());

        return castExpr;
    }

    private CharLiteral visit(CharacterLiteral node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CharLiteral charLiteral = new CharLiteral(startLine, endLine, node);

        charLiteral.setValue(node.charValue());

        AST ast = AST.newAST(AST.JLS8);
        Type type = ast.newPrimitiveType(PrimitiveType.CHAR);
        charLiteral.setType(type);

        return charLiteral;
    }

    private ClassInstanceCreate visit(ClassInstanceCreation node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ClassInstanceCreate classInstanceCreate = new ClassInstanceCreate(startLine, endLine, node);

        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression());
            expression.setParent(classInstanceCreate);
            classInstanceCreate.setExpression(expression);
        }

        if (node.getAnonymousClassDeclaration() != null) {
            AnonymousClassDecl anonymousClassDecl = (AnonymousClassDecl) process(node.getAnonymousClassDeclaration());
            anonymousClassDecl.setParent(classInstanceCreate);
            classInstanceCreate.setAnonymousClassDecl(anonymousClassDecl);
        }

        ExprList exprList = new ExprList(startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr arg = (Expr) process((ASTNode) object);
            arg.setParent(exprList);
            arguments.add(arg);
        }
        exprList.setExprs(arguments);
        exprList.setParent(classInstanceCreate);
        classInstanceCreate.setArguments(exprList);

        MType mType = new MType(startLine, endLine, node.getType());
        mType.setType(node.getType());
        mType.setParent(classInstanceCreate);
        classInstanceCreate.setClassType(mType);
        classInstanceCreate.setType(node.getType());

        return classInstanceCreate;
    }

    private AnonymousClassDecl visit(AnonymousClassDeclaration node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AnonymousClassDecl anonymousClassDecl = new AnonymousClassDecl(startLine, endLine, node);
        return anonymousClassDecl;
    }

    private ConditionalExpr visit(ConditionalExpression node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ConditionalExpr conditionalExpr = new ConditionalExpr(startLine, endLine, node);

        Expr condition = (Expr) process(node.getExpression());
        condition.setParent(conditionalExpr);
        conditionalExpr.setCondition(condition);

        Expr first = (Expr) process(node.getThenExpression());
        first.setParent(conditionalExpr);
        conditionalExpr.setFirst(first);

        Expr snd = (Expr) process(node.getElseExpression());
        snd.setParent(conditionalExpr);
        conditionalExpr.setSecond(snd);

        if (first.getType() != null) {
            conditionalExpr.setType(first.getType());
        } else {
            conditionalExpr.setType(snd.getType());
        }

        return conditionalExpr;
    }

    private CreationRef visit(CreationReference node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CreationRef creationRef = new CreationRef(startLine, endLine, node);
        return creationRef;
    }

    private ExpressionMethodRef visit(ExpressionMethodReference node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ExpressionMethodRef expressionMethodRef = new ExpressionMethodRef(startLine, endLine, node);
        return expressionMethodRef;
    }

    private FieldAcc visit(FieldAccess node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        FieldAcc fieldAcc = new FieldAcc(startLine, endLine, node);

        Expr expression = (Expr) process(node.getExpression());
        expression.setParent(fieldAcc);
        fieldAcc.setExpression(expression);

        SName identifier = (SName) process(node.getName());
        identifier.setParent(fieldAcc);
        fieldAcc.setIdentifier(identifier);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        fieldAcc.setType(type);

        return fieldAcc;
    }

    private InfixExpr visit(InfixExpression node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        InfixExpr infixExpr = new InfixExpr(startLine, endLine, node);

        Expr lhs = (Expr) process(node.getLeftOperand());
        lhs.setParent(infixExpr);
        infixExpr.setLeftHandSide(lhs);

        Expr rhs = (Expr) process(node.getRightOperand());
        rhs.setParent(infixExpr);
        infixExpr.setRightHandSide(rhs);

        InfixOperator infixOperator = new InfixOperator(startLine, endLine, null);
        infixOperator.setOperator(node.getOperator());
        infixOperator.setParent(infixExpr);
        infixExpr.setOperator(infixOperator);

        infixExpr.setType(NodeUtils.parseExprType(lhs, node.getOperator().toString(), rhs));

        return infixExpr;
    }

    private InstanceofExpr visit(InstanceofExpression node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        InstanceofExpr instanceofExpr = new InstanceofExpr(startLine, endLine, node);

        Expr expression = (Expr) process(node.getLeftOperand());
        expression.setParent(instanceofExpr);
        instanceofExpr.setExpression(expression);

        MType mType = new MType(startLine, endLine, node.getRightOperand());
        mType.setType(node.getRightOperand());
        mType.setParent(instanceofExpr);
        instanceofExpr.setInstanceType(mType);

        AST ast = AST.newAST(AST.JLS8);
        Type exprType = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
        instanceofExpr.setType(exprType);

        return instanceofExpr;
    }

    private LambdaExpr visit(LambdaExpression node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        LambdaExpr lambdaExpr = new LambdaExpr(startLine, endLine, node);
        return lambdaExpr;
    }

    private MethodInv visit(MethodInvocation node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        MethodInv methodInv = new MethodInv(startLine, endLine, node);

        Expr expression = null;
        if (node.getExpression() != null) {
            expression = (Expr) process(node.getExpression());
            expression.setParent(methodInv);
            methodInv.setExpression(expression);
        }

        SName sName = new SName(startLine, endLine, node.getName());
        sName.setName(node.getName().getFullyQualifiedName());
        sName.setParent(methodInv);
        methodInv.setName(sName);

        ExprList exprList = new ExprList(startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr expr = (Expr) process((ASTNode) object);
            expr.setParent(exprList);
            arguments.add(expr);
        }
        exprList.setExprs(arguments);
        exprList.setParent(methodInv);
        methodInv.setArguments(exprList);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        methodInv.setType(type);

        return methodInv;
    }

    private MethodRef visit(MethodReference node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        MethodRef methodRef = new MethodRef(startLine, endLine, node);
        return methodRef;
    }

    private Label visit(Name node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Label expr = null;
        if (node instanceof SimpleName) {
            SName sName = new SName(startLine, endLine, node);

            String name = node.getFullyQualifiedName();
            sName.setName(name);

            Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
            sName.setType(type);
            expr = sName;
        } else if (node instanceof QualifiedName) {
            QualifiedName qualifiedName = (QualifiedName) node;
//			System.out.println(qualifiedName.toString());
            QName qName = new QName(startLine, endLine, node);
            SName sname = (SName) process(qualifiedName.getName());
            sname.setParent(qName);
            Label label = (Label) process(qualifiedName.getQualifier());
            label.setParent(qName);
            qName.setName(label, sname);
            qName.setType(sname.getType());

            expr = qName;
        }
        return expr;
    }

    private NillLiteral visit(NullLiteral node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        NillLiteral nillLiteral = new NillLiteral(startLine, endLine, node);
        return nillLiteral;
    }

    private NumLiteral visit(NumberLiteral node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        String token = node.getToken();
        NumLiteral expr = null;
        try {
            Integer value = Integer.parseInt(token);
            IntLiteral literal = new IntLiteral(startLine, endLine, node);
            literal.setValue(value);
            AST ast = AST.newAST(AST.JLS8);
            Type type = ast.newPrimitiveType(PrimitiveType.INT);
            literal.setType(type);
            expr = literal;
        } catch (Exception e) {
        }

        if (expr == null) {
            try {
                long value = Long.parseLong(token);
                LongLiteral literal = new LongLiteral(startLine, endLine, node);
                literal.setValue(value);
                AST ast = AST.newAST(AST.JLS8);
                Type type = ast.newPrimitiveType(PrimitiveType.LONG);
                literal.setType(type);
                expr = literal;
            } catch (Exception e) {
            }
        }

        if (expr == null) {
            try {
                float value = Float.parseFloat(token);
                FloatLiteral literal = new FloatLiteral(startLine, endLine, node);
                literal.setValue(value);
                AST ast = AST.newAST(AST.JLS8);
                Type type = ast.newPrimitiveType(PrimitiveType.FLOAT);
                literal.setType(type);
                expr = literal;
            } catch (Exception e) {
            }
        }

        if (expr == null) {
            try {
                double value = Double.parseDouble(token);
                DoubleLiteral literal = new DoubleLiteral(startLine, endLine, node);
                literal.setValue(value);
                AST ast = AST.newAST(AST.JLS8);
                Type type = ast.newPrimitiveType(PrimitiveType.DOUBLE);
                literal.setType(type);
                expr = literal;
            } catch (Exception e) {
            }
        }

        if (expr == null) {
            // should be hexadecimal number or octal number
            token = token.replace("X", "x");
            token = token.replace("F", "f");
            NumLiteral literal = new NumLiteral(startLine, endLine, node);
            literal.setValue(token);
            // simply set as int type
            AST ast = AST.newAST(AST.JLS8);
            Type type = ast.newPrimitiveType(PrimitiveType.INT);
            literal.setType(type);
            expr = literal;
        }

        return expr;
    }

    private ParenthesiszedExpr visit(ParenthesizedExpression node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());

        ParenthesiszedExpr parenthesiszedExpr = new ParenthesiszedExpr(startLine, endLine, node);
        Expr expression = (Expr) process(node.getExpression());
        expression.setParent(parenthesiszedExpr);
        parenthesiszedExpr.setExpr(expression);
        parenthesiszedExpr.setType(expression.getType());

        return parenthesiszedExpr;
    }

    private PostfixExpr visit(PostfixExpression node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        PostfixExpr postfixExpr = new PostfixExpr(startLine, endLine, node);

        Expr expression = (Expr) process(node.getOperand());
        expression.setParent(postfixExpr);
        postfixExpr.setExpression(expression);

        PostOperator postOperator = new PostOperator(startLine, endLine, null);
        postOperator.setOperator(node.getOperator());
        postOperator.setParent(postfixExpr);
        postfixExpr.setOperator(postOperator);

        Type exprType = NodeUtils.parseExprType(expression, node.getOperator().toString(), null);

        postfixExpr.setType(exprType);

        return postfixExpr;
    }

    private PrefixExpr visit(PrefixExpression node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        PrefixExpr prefixExpr = new PrefixExpr(startLine, endLine, node);

        Expr expression = (Expr) process(node.getOperand());
        expression.setParent(prefixExpr);
        prefixExpr.setExpression(expression);

        PrefixOperator prefixOperator = new PrefixOperator(startLine, endLine, null);
        prefixOperator.setOperator(node.getOperator());
        prefixOperator.setParent(prefixExpr);
        prefixExpr.setOperator(prefixOperator);

        Type type = NodeUtils.parseExprType(null, node.getOperator().toString(), expression);
        prefixExpr.setType(type);

        return prefixExpr;
    }

    private StrLiteral visit(StringLiteral node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        StrLiteral literal = new StrLiteral(startLine, endLine, node);

        literal.setValue(node.getLiteralValue());

        AST ast = AST.newAST(AST.JLS8);
        Type type = ast.newSimpleType(ast.newSimpleName("String"));
        literal.setType(type);

        return literal;
    }

    private SuperFieldAcc visit(SuperFieldAccess node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperFieldAcc superFieldAcc = new SuperFieldAcc(startLine, endLine, node);

        SName identifier = (SName) process(node.getName());
        identifier.setParent(superFieldAcc);
        superFieldAcc.setIdentifier(identifier);

        if (node.getQualifier() != null) {
            Label name = (Label) process(node.getQualifier());
            name.setParent(superFieldAcc);
            superFieldAcc.setName(name);
        }

        Type exprType = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        superFieldAcc.setType(exprType);

        return superFieldAcc;
    }

    private SuperMethodInv visit(SuperMethodInvocation node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperMethodInv superMethodInv = new SuperMethodInv(startLine, endLine, node);

        SName sName = new SName(startLine, endLine, node.getName());
        sName.setName(node.getName().getFullyQualifiedName());
        sName.setParent(superMethodInv);
        superMethodInv.setName(sName);

        if (node.getQualifier() != null) {
            Label label = (Label) process(node.getQualifier());
            label.setParent(superMethodInv);
            superMethodInv.setLabel(label);
        }

        ExprList exprList = new ExprList(startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr expr = (Expr) process((ASTNode) object);
            expr.setParent(exprList);
            arguments.add(expr);
        }
        exprList.setExprs(arguments);
        exprList.setParent(superMethodInv);
        superMethodInv.setArguments(exprList);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        superMethodInv.setType(type);

        return superMethodInv;
    }

    private SuperMethodRef visit(SuperMethodReference node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperMethodRef superMethodRef = new SuperMethodRef(startLine, endLine, node);

        return superMethodRef;
    }

    private ThisExpr visit(ThisExpression node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ThisExpr thisExpr = new ThisExpr(startLine, endLine, node);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        thisExpr.setType(type);

        return thisExpr;
    }

    private TyLiteral visit(TypeLiteral node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TyLiteral tyLiteral = new TyLiteral(startLine, endLine, node);
        MType mType = new MType(startLine, endLine, node.getType());
        mType.setType(node.getType());
        mType.setParent(tyLiteral);
        tyLiteral.setValue(mType);
        tyLiteral.setType(node.getType());

        return tyLiteral;
    }

    private TypeMethodRef visit(TypeMethodReference node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TypeMethodRef typeMethodRef = new TypeMethodRef(startLine, endLine, node);
        return typeMethodRef;
    }

    private VarDeclarationExpr visit(VariableDeclarationExpression node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        VarDeclarationExpr varDeclarationExpr = new VarDeclarationExpr(startLine, endLine, node);

        MType mType = new MType(startLine, endLine, node.getType());
        mType.setType(node.getType());
        mType.setParent(varDeclarationExpr);
        varDeclarationExpr.setDeclType(mType);

        List<Vdf> vdfs = new ArrayList<>();
        for (Object object : node.fragments()) {
            Vdf vdf = (Vdf) process((ASTNode) object);
            vdf.setParent(varDeclarationExpr);
            vdfs.add(vdf);
        }
        varDeclarationExpr.setVarDeclFrags(vdfs);

        return varDeclarationExpr;
    }

    private Vdf visit(VariableDeclarationFragment node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Vdf vdf = new Vdf(startLine, endLine, node);

        SName identifier = (SName) process(node.getName());
        identifier.setParent(vdf);
        vdf.setName(identifier);

        vdf.setDimensions(node.getExtraDimensions());

        if (node.getInitializer() != null) {
            Expr expression = (Expr) process(node.getInitializer());
            expression.setParent(vdf);
            vdf.setExpression(expression);
        }

        return vdf;
    }

    private Svd visit(SingleVariableDeclaration node) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Svd svd = new Svd(startLine, endLine, node);

        MType mType = new MType(startLine, endLine, node.getType());
        mType.setType(node.getType());
        mType.setParent(svd);
        svd.setDecType(mType);
        if (node.getInitializer() != null) {
            Expr initializer = (Expr) process(node.getInitializer());
            initializer.setParent(svd);
            svd.setInitializer(initializer);
        }

        SName name = (SName) process(node.getName());
        name.setParent(svd);
        svd.setName(name);

        return svd;
    }

    public Node process(ASTNode node) {
        if (node == null) {
            return null;
        }
        if (node instanceof AssertStatement) {
            return visit((AssertStatement) node);
        } else if (node instanceof Block) {
            return visit((Block) node);
        } else if (node instanceof BreakStatement) {
            return visit((BreakStatement) node);
        } else if (node instanceof ConstructorInvocation) {
            return visit((ConstructorInvocation) node);
        } else if (node instanceof ContinueStatement) {
            return visit((ContinueStatement) node);
        } else if (node instanceof DoStatement) {
            return visit((DoStatement) node);
        } else if (node instanceof EmptyStatement) {
            return visit((EmptyStatement) node);
        } else if (node instanceof EnhancedForStatement) {
            return visit((EnhancedForStatement) node);
        } else if (node instanceof ExpressionStatement) {
            return visit((ExpressionStatement) node);
        } else if (node instanceof ForStatement) {
            return visit((ForStatement) node);
        } else if (node instanceof IfStatement) {
            return visit((IfStatement) node);
        } else if (node instanceof LabeledStatement) {
            return visit((LabeledStatement) node);
        } else if (node instanceof ReturnStatement) {
            return visit((ReturnStatement) node);
        } else if (node instanceof SuperConstructorInvocation) {
            return visit((SuperConstructorInvocation) node);
        } else if (node instanceof SwitchCase) {
            return visit((SwitchCase) node);
        } else if (node instanceof SwitchStatement) {
            return visit((SwitchStatement) node);
        } else if (node instanceof SynchronizedStatement) {
            return visit((SynchronizedStatement) node);
        } else if (node instanceof ThrowStatement) {
            return visit((ThrowStatement) node);
        } else if (node instanceof TryStatement) {
            return visit((TryStatement) node);
        } else if (node instanceof TypeDeclarationStatement) {
            return visit((TypeDeclarationStatement) node);
        } else if (node instanceof VariableDeclarationStatement) {
            return visit((VariableDeclarationStatement) node);
        } else if (node instanceof WhileStatement) {
            return visit((WhileStatement) node);
        } else if (node instanceof Annotation) {
            return visit((Annotation) node);
        } else if (node instanceof ArrayAccess) {
            return visit((ArrayAccess) node);
        } else if (node instanceof ArrayCreation) {
            return visit((ArrayCreation) node);
        } else if (node instanceof ArrayInitializer) {
            return visit((ArrayInitializer) node);
        } else if (node instanceof Assignment) {
            return visit((Assignment) node);
        } else if (node instanceof BooleanLiteral) {
            return visit((BooleanLiteral) node);
        } else if (node instanceof CastExpression) {
            return visit((CastExpression) node);
        } else if (node instanceof CharacterLiteral) {
            return visit((CharacterLiteral) node);
        } else if (node instanceof ClassInstanceCreation) {
            return visit((ClassInstanceCreation) node);
        } else if (node instanceof ConditionalExpression) {
            return visit((ConditionalExpression) node);
        } else if (node instanceof CreationReference) {
            return visit((CreationReference) node);
        } else if (node instanceof ExpressionMethodReference) {
            return visit((ExpressionMethodReference) node);
        } else if (node instanceof FieldAccess) {
            return visit((FieldAccess) node);
        } else if (node instanceof InfixExpression) {
            return visit((InfixExpression) node);
        } else if (node instanceof InstanceofExpression) {
            return visit((InstanceofExpression) node);
        } else if (node instanceof LambdaExpression) {
            return visit((LambdaExpression) node);
        } else if (node instanceof MethodInvocation) {
            return visit((MethodInvocation) node);
        } else if (node instanceof MethodReference) {
            return visit((MethodReference) node);
        } else if (node instanceof Name) {
            return visit((Name) node);
        } else if (node instanceof NullLiteral) {
            return visit((NullLiteral) node);
        } else if (node instanceof NumberLiteral) {
            return visit((NumberLiteral) node);
        } else if (node instanceof ParenthesizedExpression) {
            return visit((ParenthesizedExpression) node);
        } else if (node instanceof PostfixExpression) {
            return visit((PostfixExpression) node);
        } else if (node instanceof PrefixExpression) {
            return visit((PrefixExpression) node);
        } else if (node instanceof StringLiteral) {
            return visit((StringLiteral) node);
        } else if (node instanceof SuperFieldAccess) {
            return visit((SuperFieldAccess) node);
        } else if (node instanceof SuperMethodInvocation) {
            return visit((SuperMethodInvocation) node);
        } else if (node instanceof SuperMethodReference) {
            return visit((SuperMethodReference) node);
        } else if (node instanceof ThisExpression) {
            return visit((ThisExpression) node);
        } else if (node instanceof TypeLiteral) {
            return visit((TypeLiteral) node);
        } else if (node instanceof TypeMethodReference) {
            return visit((TypeMethodReference) node);
        } else if (node instanceof VariableDeclarationExpression) {
            return visit((VariableDeclarationExpression) node);
        } else if (node instanceof AnonymousClassDeclaration) {
            return visit((AnonymousClassDeclaration) node);
        } else if (node instanceof VariableDeclarationFragment) {
            return visit((VariableDeclarationFragment) node);
        } else if (node instanceof SingleVariableDeclaration) {
            return visit((SingleVariableDeclaration) node);
        } else if (node instanceof MethodDeclaration) {
            return visit((MethodDeclaration) node);
        } else if (node instanceof CatchClause) {
            return visit((CatchClause) node);
        } else {
            System.out.println("UNKNOWN ASTNode type : " + node.toString());
            return null;
        }
    }

    public static Type typeFromBinding(AST ast, ITypeBinding typeBinding) {
        if(typeBinding == null) {
            return ast.newWildcardType();
        }

        if( typeBinding.isPrimitive() ) {
            return ast.newPrimitiveType(
                    PrimitiveType.toCode(typeBinding.getName()));
        }

        if( typeBinding.isCapture() ) {
            ITypeBinding wildCard = typeBinding.getWildcard();
            WildcardType capType = ast.newWildcardType();
            ITypeBinding bound = wildCard.getBound();
            if( bound != null ) {
                capType.setBound(typeFromBinding(ast, bound),
                        wildCard.isUpperbound());
            }
            return capType;
        }

        if( typeBinding.isArray() ) {
            Type elType = typeFromBinding(ast, typeBinding.getElementType());
            return ast.newArrayType(elType, typeBinding.getDimensions());
        }

        if( typeBinding.isParameterizedType() ) {
            ParameterizedType type = ast.newParameterizedType(
                    typeFromBinding(ast, typeBinding.getErasure()));

            @SuppressWarnings("unchecked")
            List<Type> newTypeArgs = type.typeArguments();
            for( ITypeBinding typeArg : typeBinding.getTypeArguments() ) {
                newTypeArgs.add(typeFromBinding(ast, typeArg));
            }

            return type;
        }

        // simple or raw type
        String qualName = typeBinding.getQualifiedName();
        if( "".equals(qualName) ) {
            return ast.newWildcardType();
        }
        return ast.newSimpleType(ast.newName(qualName));
    }

}
