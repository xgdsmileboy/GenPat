/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */
package mfix.core.node.parser;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Comment;
import mfix.core.node.ast.expr.MethodRef;
import mfix.core.node.ast.expr.*;
import mfix.core.node.ast.stmt.*;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;


/**
 * @author: Jiajun
 * @date: 2018/9/21
 */
public class NodeParser {

    private CompilationUnit _cunit;
    private String _fileName;

    public NodeParser() {

    }

    public NodeParser setCompilationUnit(String fileName, CompilationUnit unit) {
        _cunit = unit;
        _fileName = fileName;
        return this;
    }

    /************************** visit start : MethodDeclaration ***********************/
    private MethDecl visit(MethodDeclaration node, VScope scope) {
        int start = _cunit.getLineNumber(node.getStartPosition());
        int end = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        MethDecl methDecl = new MethDecl(_fileName, start, end, node);

        List<String> modifiers = new ArrayList<>(5);
        for(Object object: node.modifiers()) {
            modifiers.add(object.toString());
        }
        methDecl.setModifiers(modifiers);
        if(node.getReturnType2() != null) {
            methDecl.setRetType(typeFromBinding(node.getAST(), node.getReturnType2().resolveBinding()));
        }
        SName name = (SName) process(node.getName(), scope);
        name.setParent(methDecl);
        methDecl.setName(name);
        List<Expr> params = new ArrayList<>(11);
        for (Object arg : node.parameters()) {
            Expr param = (Expr) process((ASTNode) arg, scope);
            param.setParent(methDecl);
            params.add(param);
        }
        methDecl.setArguments(params);

        List<String> throwTypes = new ArrayList<>(7);
        for(Object object : node.thrownExceptionTypes()) {
            Type throwType = typeFromBinding(node.getAST(), ((Type) object).resolveBinding());
            if(throwType == null) {
                throwTypes.add(object.toString());
            } else {
                throwTypes.add(throwType.toString());
            }
        }

        methDecl.setThrows(throwTypes);

        Block body = node.getBody();
        if (body != null) {
            Blk bBlk = (Blk) process(body, scope);
            bBlk.setParent(methDecl);
            methDecl.setBody(bBlk);
        }

        return methDecl;
    }

    /************************** visit start : Statement ***********************/
    private AssertStmt visit(AssertStatement node, VScope scope) {
        int start = _cunit.getLineNumber(node.getStartPosition());
        int end = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AssertStmt assertStmt = new AssertStmt(_fileName, start, end, node);
        assertStmt.setControldependency(scope.peekStructure());
        return assertStmt;
    }

    private BreakStmt visit(BreakStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        BreakStmt breakStmt = new BreakStmt(_fileName, startLine, endLine, node);
        if (node.getLabel() != null) {
            SName sName = new SName(_fileName, startLine, endLine, node.getLabel());
            sName.setName(node.getLabel().getFullyQualifiedName());
            sName.setParent(breakStmt);
            breakStmt.setIdentifier(sName);
            sName.setDataDependency(scope.getDefines(sName.getName()));
        }
        breakStmt.setControldependency(scope.peekStructure());
        return breakStmt;
    }

    private Blk visit(Block node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Blk blk = new Blk(_fileName, startLine, endLine, node);
        List<Stmt> stmts = new ArrayList<>();
        VScope newScope = new VScope(scope);
        for (Object object : node.statements()) {
            Stmt stmt = (Stmt) process((ASTNode) object, newScope);
            stmt.setParent(blk);
            stmts.add(stmt);
        }
        blk.setStatement(stmts);
        return blk;
    }

    private ConstructorInv visit(ConstructorInvocation node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ConstructorInv constructorInv = new ConstructorInv(_fileName, startLine, endLine, node);
        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr expr = (Expr) process((ASTNode) object, scope);
            expr.setParent(exprList);
            arguments.add(expr);
        }
        exprList.setExprs(arguments);
        exprList.setParent(constructorInv);
        constructorInv.setArguments(exprList);
        constructorInv.setControldependency(scope.peekStructure());
        return constructorInv;
    }

    private ContinueStmt visit(ContinueStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ContinueStmt continueStmt = new ContinueStmt(_fileName, startLine, endLine, node);
        if (node.getLabel() != null) {
            SName sName = new SName(_fileName, startLine, endLine, node.getLabel());
            sName.setName(node.getLabel().getFullyQualifiedName());
            sName.setParent(continueStmt);
            continueStmt.setIdentifier(sName);
            sName.setDataDependency(scope.getDefines(sName.getName()));
        }
        continueStmt.setControldependency(scope.peekStructure());
        return continueStmt;
    }

    private DoStmt visit(DoStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        DoStmt doStmt = new DoStmt(_fileName, startLine, endLine, node);
        doStmt.setControldependency(scope.peekStructure());
        VScope newScope = new VScope(scope);

        Expr expression = (Expr) process(node.getExpression(), newScope);
        expression.setParent(doStmt);
        doStmt.setExpression(expression);

        newScope.pushStructure(expression);
        Stmt stmt = wrapBlock(node.getBody(), newScope);
        stmt.setParent(doStmt);
        doStmt.setBody(stmt);

        newScope.popStructure();
        return doStmt;
    }

    private EmptyStmt visit(EmptyStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        EmptyStmt emptyStmt = new EmptyStmt(_fileName, startLine, endLine, node);
        emptyStmt.setControldependency(scope.peekStructure());
        return emptyStmt;
    }

    private EnhancedForStmt visit(EnhancedForStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        EnhancedForStmt enhancedForStmt = new EnhancedForStmt(_fileName, startLine, endLine, node);
        enhancedForStmt.setDataDependency(scope.peekStructure());

        VScope newScope = new VScope(scope);
        Svd svd = (Svd) process(node.getParameter(), newScope);
        svd.setParent(enhancedForStmt);
        enhancedForStmt.setParameter(svd);

        Expr expression = (Expr) process(node.getExpression(), newScope);
        expression.setParent(enhancedForStmt);
        enhancedForStmt.setExpression(expression);

        newScope.pushStructure(expression);
        Stmt body = wrapBlock(node.getBody(), newScope);
        body.setParent(enhancedForStmt);
        enhancedForStmt.setBody(body);
        newScope.popStructure();

        return enhancedForStmt;
    }

    private ExpressionStmt visit(ExpressionStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ExpressionStmt expressionStmt = new ExpressionStmt(_fileName, startLine, endLine, node);

        Expr expression = (Expr) process(node.getExpression(), scope);
        expression.setParent(expressionStmt);
        expressionStmt.setExpression(expression);
        expressionStmt.setControldependency(scope.peekStructure());

        return expressionStmt;
    }

    private ForStmt visit(ForStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ForStmt forStmt = new ForStmt(_fileName, startLine, endLine, node);
        forStmt.setControldependency(scope.peekStructure());
        VScope newScope = new VScope(scope);

        ExprList initExprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> initializers = new ArrayList<>();
        if (!node.initializers().isEmpty()) {
            for (Object object : node.initializers()) {
                Expr initializer = (Expr) process((ASTNode) object, newScope);
                initializer.setParent(initExprList);
                initializers.add(initializer);
            }
        }
        initExprList.setExprs(initializers);
        initExprList.setParent(forStmt);
        forStmt.setInitializer(initExprList);

        if (node.getExpression() != null) {
            Expr condition = (Expr) process(node.getExpression(), newScope);
            condition.setParent(forStmt);
            forStmt.setCondition(condition);
            newScope.pushStructure(condition);
        }

        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> updaters = new ArrayList<>();
        if (!node.updaters().isEmpty()) {
            for (Object object : node.updaters()) {
                Expr update = (Expr) process((ASTNode) object, newScope);
                update.setParent(exprList);
                updaters.add(update);
            }
        }
        exprList.setExprs(updaters);
        exprList.setParent(forStmt);
        forStmt.setUpdaters(exprList);

        Stmt body = wrapBlock(node.getBody(), newScope);
        body.setParent(forStmt);
        forStmt.setBody(body);
        if (node.getExpression() != null) {
            newScope.popStructure();
        }

        return forStmt;
    }

    private IfStmt visit(IfStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        IfStmt ifStmt = new IfStmt(_fileName, startLine, endLine, node);
        ifStmt.setControldependency(scope.peekStructure());

        VScope newScope = new VScope(scope);

        Expr condition = (Expr) process(node.getExpression(), newScope);
        condition.setParent(ifStmt);
        ifStmt.setCondition(condition);

        newScope.pushStructure(condition);

        VScope lScope = new VScope(newScope);
        Stmt then = wrapBlock(node.getThenStatement(), lScope);
        then.setParent(ifStmt);
        ifStmt.setThen(then);

        if (node.getElseStatement() != null) {
            VScope rScope = new VScope(newScope);
            Stmt els = wrapBlock(node.getElseStatement(), rScope);
            els.setParent(ifStmt);
            ifStmt.setElse(els);
        }
        newScope.popStructure();
        return ifStmt;
    }

    private LabeledStmt visit(LabeledStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        LabeledStmt labeledStmt = new LabeledStmt(_fileName, startLine, endLine, node);
        labeledStmt.setControldependency(scope.peekStructure());
        return labeledStmt;
    }

    private ReturnStmt visit(ReturnStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ReturnStmt returnStmt = new ReturnStmt(_fileName, startLine, endLine, node);
        returnStmt.setControldependency(scope.peekStructure());
        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression(), scope);
            expression.setParent(returnStmt);
            returnStmt.setExpression(expression);
        }

        return returnStmt;
    }

    private SuperConstructorInv visit(SuperConstructorInvocation node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperConstructorInv superConstructorInv = new SuperConstructorInv(_fileName, startLine, endLine, node);
        superConstructorInv.setControldependency(scope.peekStructure());

        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression(), scope);
            expression.setParent(superConstructorInv);
            superConstructorInv.setExpression(expression);
        }

        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr arg = (Expr) process((ASTNode) object, scope);
            arg.setParent(exprList);
            arguments.add(arg);
        }
        exprList.setExprs(arguments);
        exprList.setParent(superConstructorInv);
        superConstructorInv.setArguments(exprList);

        return superConstructorInv;
    }

    private SwCase visit(SwitchCase node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SwCase swCase = new SwCase(_fileName, startLine, endLine, node);
        swCase.setControldependency(scope.peekStructure());
        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression(), scope);
            expression.setParent(swCase);
            swCase.setExpression(expression);
        }

        return swCase;
    }

    private SwitchStmt visit(SwitchStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SwitchStmt switchStmt = new SwitchStmt(_fileName, startLine, endLine, node);
        switchStmt.setControldependency(scope.peekStructure());
        VScope newScope = new VScope(scope);

        Expr expression = (Expr) process(node.getExpression(), newScope);
        expression.setParent(switchStmt);
        switchStmt.setExpression(expression);

        int push = 0;
        List<Stmt> statements = new ArrayList<>();
        for (Object object : node.statements()) {
            Stmt stmt = (Stmt) process((ASTNode) object, newScope);
            stmt.setParent(switchStmt);
            statements.add(stmt);
            if(stmt instanceof SwCase) {
                newScope.pushStructure(stmt);
                push ++;
            }
        }
        while(push > 0) {
            newScope.popStructure();
            push --;
        }
        switchStmt.setStatements(statements);
        newScope.popStructure();

        return switchStmt;
    }

    private SynchronizedStmt visit(SynchronizedStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SynchronizedStmt synchronizedStmt = new SynchronizedStmt(_fileName, startLine, endLine, node);
        synchronizedStmt.setControldependency(scope.peekStructure());

        VScope newScope = new VScope(scope);
        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression(), newScope);
            expression.setParent(synchronizedStmt);
            synchronizedStmt.setExpression(expression);
        }

        Blk blk = (Blk) process(node.getBody(), newScope);
        blk.setParent(synchronizedStmt);
        synchronizedStmt.setBlock(blk);

        return synchronizedStmt;
    }

    private ThrowStmt visit(ThrowStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ThrowStmt throwStmt = new ThrowStmt(_fileName, startLine, endLine, node);
        throwStmt.setControldependency(scope.peekStructure());

        Expr expression = (Expr) process(node.getExpression(), scope);
        expression.setParent(throwStmt);
        throwStmt.setExpression(expression);

        return throwStmt;
    }

    private TryStmt visit(TryStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TryStmt tryStmt = new TryStmt(_fileName, startLine, endLine, node);
        tryStmt.setControldependency(scope.peekStructure());

        VScope newScope = new VScope(scope);

        if (node.resources() != null) {
            List<VarDeclarationExpr> resourceList = new ArrayList<>(node.resources().size());
            for (Object object : node.resources()) {
                VariableDeclarationExpression resource = (VariableDeclarationExpression) object;
                VarDeclarationExpr vdExpr = (VarDeclarationExpr) process(resource, newScope);
                vdExpr.setParent(tryStmt);
                resourceList.add(vdExpr);
            }
            tryStmt.setResource(resourceList);
        }
        Blk blk = (Blk) process(node.getBody(), newScope);
        blk.setParent(tryStmt);
        tryStmt.setBody(blk);

        newScope = new VScope(scope);
        List<CatClause> catches = new ArrayList<>(node.catchClauses().size());
        for (Object object : node.catchClauses()) {
            CatchClause catchClause = (CatchClause) object;
            CatClause catClause = (CatClause) process(catchClause, newScope);
            catClause.setParent(tryStmt);
            catches.add(catClause);
        }
        tryStmt.setCatchClause(catches);

        newScope = new VScope(scope);
        if (node.getFinally() != null) {
            Blk finallyBlk = (Blk) process(node.getFinally(), newScope);
            finallyBlk.setParent(tryStmt);
            tryStmt.setFinallyBlock(finallyBlk);
        }

        return tryStmt;
    }

    private CatClause visit(CatchClause node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CatClause catClause = new CatClause(_fileName, startLine, endLine, node);
        catClause.setControldependency(scope.peekStructure());

        VScope newScope = new VScope(scope);
        Svd svd = (Svd) process(node.getException(), newScope);
        svd.setParent(catClause);
        catClause.setException(svd);
        Blk body = (Blk) process(node.getBody(), newScope);
        body.setParent(catClause);
        catClause.setBody(body);
        return catClause;
    }

    private TypeDeclarationStmt visit(TypeDeclarationStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TypeDeclarationStmt typeDeclarationStmt = new TypeDeclarationStmt(_fileName, startLine, endLine, node);
        typeDeclarationStmt.setControldependency(scope.peekStructure());
        return typeDeclarationStmt;
    }

    private VarDeclarationStmt visit(VariableDeclarationStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        VarDeclarationStmt varDeclarationStmt = new VarDeclarationStmt(_fileName, startLine, endLine, node);
        varDeclarationStmt.setControldependency(scope.peekStructure());
        String modifier = "";
        if (node.modifiers() != null && node.modifiers().size() > 0) {
            for (Object object : node.modifiers()) {
                modifier += " " + object.toString();
            }
        }
        if (modifier.length() > 0) {
            varDeclarationStmt.setModifier(modifier);
        }

        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        mType.setType(typeFromBinding(node.getAST(), node.getType().resolveBinding()));
        mType.setParent(varDeclarationStmt);
        varDeclarationStmt.setDeclType(mType);

        List<Vdf> fragments = new ArrayList<>();
        for (Object object : node.fragments()) {
            Vdf vdf = (Vdf) process((ASTNode) object, scope);
            vdf.setParent(varDeclarationStmt);
            fragments.add(vdf);
        }
        varDeclarationStmt.setFragments(fragments);

        return varDeclarationStmt;
    }

    private WhileStmt visit(WhileStatement node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        WhileStmt whileStmt = new WhileStmt(_fileName, startLine, endLine, node);
        whileStmt.setControldependency(scope.peekStructure());

        VScope newScope = new VScope(scope);

        Expr expression = (Expr) process(node.getExpression(), newScope);
        expression.setParent(whileStmt);
        whileStmt.setExpression(expression);

        newScope.pushStructure(expression);

        Stmt body = wrapBlock(node.getBody(), newScope);
        body.setParent(whileStmt);
        whileStmt.setBody(body);

        newScope.popStructure();

        return whileStmt;
    }

    /*********************** Visit Expression *********************************/
    private Comment visit(Annotation node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Comment comment = new Comment(_fileName, startLine, endLine, node);
        return comment;
    }

    private AryAcc visit(ArrayAccess node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AryAcc aryAcc = new AryAcc(_fileName, startLine, endLine, node);

        Expr array = (Expr) process(node.getArray(), scope);
        array.setParent(aryAcc);
        aryAcc.setArray(array);

        Expr indexExpr = (Expr) process(node.getIndex(), scope);
        indexExpr.setParent(aryAcc);
        aryAcc.setIndex(indexExpr);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        aryAcc.setType(type);

        return aryAcc;
    }

    private AryCreation visit(ArrayCreation node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AryCreation aryCreation = new AryCreation(_fileName, startLine, endLine, node);
        MType mType = new MType(_fileName, startLine, endLine, node.getType().getElementType());
        mType.setType(typeFromBinding(node.getAST(), node.getType().getElementType().resolveBinding()));
        mType.setParent(aryCreation);
        aryCreation.setArrayType(mType);
        aryCreation.setType(node.getType());

        List<Expr> dimension = new ArrayList<>();
        for (Object object : node.dimensions()) {
            Expr dim = (Expr) process((ASTNode) object, scope);
            dim.setParent(aryCreation);
            dimension.add(dim);
        }
        aryCreation.setDimension(dimension);

        if (node.getInitializer() != null) {
            AryInitializer arrayInitializer = (AryInitializer) process(node.getInitializer(), scope);
            arrayInitializer.setParent(aryCreation);
            aryCreation.setInitializer(arrayInitializer);
        }

        return aryCreation;
    }

    private AryInitializer visit(ArrayInitializer node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AryInitializer aryInitializer = new AryInitializer(_fileName, startLine, endLine, node);

        List<Expr> expressions = new ArrayList<>();
        for (Object object : node.expressions()) {
            Expr expr = (Expr) process((ASTNode) object, scope);
            expr.setParent(aryInitializer);
            expressions.add(expr);
        }
        aryInitializer.setExpressions(expressions);

        return aryInitializer;
    }

    private Assign visit(Assignment node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Assign assign = new Assign(_fileName, startLine, endLine, node);

        Expr lhs = (Expr) process(node.getLeftHandSide(), scope);
        lhs.setParent(assign);
        assign.setLeftHandSide(lhs);

        Expr rhs = (Expr) process(node.getRightHandSide(), scope);
        rhs.setParent(assign);
        assign.setRightHandSide(rhs);

        AssignOperator assignOperator = new AssignOperator(_fileName, startLine, endLine, null);
        assignOperator.setOperator(node.getOperator());
        assignOperator.setParent(assign);
        assign.setOperator(assignOperator);

        scope.addDefine(lhs.toSrcString().toString(), assign);

        return assign;
    }

    private BoolLiteral visit(BooleanLiteral node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        BoolLiteral literal = new BoolLiteral(_fileName, startLine, endLine, node);
        literal.setValue(node.booleanValue());
        AST ast = AST.newAST(AST.JLS8);
        Type type = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
        literal.setType(type);

        return literal;
    }

    private CastExpr visit(CastExpression node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CastExpr castExpr = new CastExpr(_fileName, startLine, endLine, node);
        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        mType.setType(typeFromBinding(node.getAST(), node.getType().resolveBinding()));
        mType.setParent(castExpr);
        castExpr.setCastType(mType);
        Expr expression = (Expr) process(node.getExpression(), scope);
        expression.setParent(castExpr);
        castExpr.setExpression(expression);
        castExpr.setType(node.getType());

        return castExpr;
    }

    private CharLiteral visit(CharacterLiteral node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CharLiteral charLiteral = new CharLiteral(_fileName, startLine, endLine, node);

        charLiteral.setValue(node.charValue());

        AST ast = AST.newAST(AST.JLS8);
        Type type = ast.newPrimitiveType(PrimitiveType.CHAR);
        charLiteral.setType(type);

        return charLiteral;
    }

    private ClassInstCreation visit(ClassInstanceCreation node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ClassInstCreation classInstCreation = new ClassInstCreation(_fileName, startLine, endLine, node);

        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression(), scope);
            expression.setParent(classInstCreation);
            classInstCreation.setExpression(expression);
        }

        if (node.getAnonymousClassDeclaration() != null) {
            AnonymousClassDecl anonymousClassDecl = (AnonymousClassDecl) process(node.getAnonymousClassDeclaration(),
                    scope);
            anonymousClassDecl.setParent(classInstCreation);
            classInstCreation.setAnonymousClassDecl(anonymousClassDecl);
        }

        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr arg = (Expr) process((ASTNode) object, scope);
            arg.setParent(exprList);
            arguments.add(arg);
        }
        exprList.setExprs(arguments);
        exprList.setParent(classInstCreation);
        classInstCreation.setArguments(exprList);

        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        Type type = typeFromBinding(node.getAST(), node.getType().resolveBinding());
        mType.setType(type);
        mType.setParent(classInstCreation);
        classInstCreation.setClassType(mType);
        classInstCreation.setType(type);

        return classInstCreation;
    }

    private AnonymousClassDecl visit(AnonymousClassDeclaration node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AnonymousClassDecl anonymousClassDecl = new AnonymousClassDecl(_fileName, startLine, endLine, node);
        return anonymousClassDecl;
    }

    private ConditionalExpr visit(ConditionalExpression node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ConditionalExpr conditionalExpr = new ConditionalExpr(_fileName, startLine, endLine, node);

        Expr condition = (Expr) process(node.getExpression(), scope);
        condition.setParent(conditionalExpr);
        conditionalExpr.setCondition(condition);

        Expr first = (Expr) process(node.getThenExpression(), scope);
        first.setParent(conditionalExpr);
        conditionalExpr.setFirst(first);
        first.setControldependency(condition);

        Expr snd = (Expr) process(node.getElseExpression(), scope);
        snd.setParent(conditionalExpr);
        conditionalExpr.setSecond(snd);
        snd.setControldependency(condition);

        if (first.getType() != null) {
            conditionalExpr.setType(first.getType());
        } else {
            conditionalExpr.setType(snd.getType());
        }

        return conditionalExpr;
    }

    private CreationRef visit(CreationReference node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CreationRef creationRef = new CreationRef(_fileName, startLine, endLine, node);
        return creationRef;
    }

    private ExpressionMethodRef visit(ExpressionMethodReference node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ExpressionMethodRef expressionMethodRef = new ExpressionMethodRef(_fileName, startLine, endLine, node);
        return expressionMethodRef;
    }

    private FieldAcc visit(FieldAccess node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        FieldAcc fieldAcc = new FieldAcc(_fileName, startLine, endLine, node);

        Expr expression = (Expr) process(node.getExpression(), scope);
        expression.setParent(fieldAcc);
        fieldAcc.setExpression(expression);

        SName identifier = (SName) process(node.getName(), scope);
        identifier.setParent(fieldAcc);
        fieldAcc.setIdentifier(identifier);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        fieldAcc.setType(type);

        fieldAcc.setDataDependency(scope.getDefines(fieldAcc.toSrcString().toString()));
        return fieldAcc;
    }

    private InfixExpr visit(InfixExpression node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        InfixExpr infixExpr = new InfixExpr(_fileName, startLine, endLine, node);

        Expr lhs = (Expr) process(node.getLeftOperand(), scope);
        lhs.setParent(infixExpr);
        infixExpr.setLeftHandSide(lhs);

        Expr rhs = (Expr) process(node.getRightOperand(), scope);
        rhs.setParent(infixExpr);
        infixExpr.setRightHandSide(rhs);

        InfixOperator infixOperator = new InfixOperator(_fileName, startLine, endLine, null);
        infixOperator.setOperator(node.getOperator());
        infixOperator.setParent(infixExpr);
        infixExpr.setOperator(infixOperator);

        infixExpr.setType(NodeUtils.parseExprType(lhs, node.getOperator().toString(), rhs));

        return infixExpr;
    }

    private InstanceofExpr visit(InstanceofExpression node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        InstanceofExpr instanceofExpr = new InstanceofExpr(_fileName, startLine, endLine, node);

        Expr expression = (Expr) process(node.getLeftOperand(), scope);
        expression.setParent(instanceofExpr);
        instanceofExpr.setExpression(expression);

        MType mType = new MType(_fileName, startLine, endLine, node.getRightOperand());
        mType.setType(node.getRightOperand());
        mType.setParent(instanceofExpr);
        instanceofExpr.setInstanceType(mType);

        AST ast = AST.newAST(AST.JLS8);
        Type exprType = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
        instanceofExpr.setType(exprType);

        return instanceofExpr;
    }

    private LambdaExpr visit(LambdaExpression node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        LambdaExpr lambdaExpr = new LambdaExpr(_fileName, startLine, endLine, node);
        return lambdaExpr;
    }

    private MethodInv visit(MethodInvocation node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        MethodInv methodInv = new MethodInv(_fileName, startLine, endLine, node);

        Expr expression = null;
        if (node.getExpression() != null) {
            expression = (Expr) process(node.getExpression(), scope);
            expression.setParent(methodInv);
            methodInv.setExpression(expression);
        }

        SName sName = new SName(_fileName, startLine, endLine, node.getName());
        sName.setName(node.getName().getFullyQualifiedName());
        sName.setParent(methodInv);
        methodInv.setName(sName);

        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr expr = (Expr) process((ASTNode) object, scope);
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

    private MethodRef visit(MethodReference node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        MethodRef methodRef = new MethodRef(_fileName, startLine, endLine, node);
        return methodRef;
    }

    private Label visit(Name node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Label expr = null;
        if (node instanceof SimpleName) {
            SName sName = new SName(_fileName, startLine, endLine, node);

            String name = node.getFullyQualifiedName();
            sName.setName(name);

            Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
            sName.setType(type);
            expr = sName;
        } else if (node instanceof QualifiedName) {
            QualifiedName qualifiedName = (QualifiedName) node;
//			System.out.println(qualifiedName.toString());
            QName qName = new QName(_fileName, startLine, endLine, node);
            SName sname = (SName) process(qualifiedName.getName(), scope);
            sname.setParent(qName);
            Label label = (Label) process(qualifiedName.getQualifier(), scope);
            label.setParent(qName);
            qName.setName(label, sname);
            qName.setType(sname.getType());

            expr = qName;
        }
        expr.setDataDependency(scope.getDefines(expr.toSrcString().toString()));
        return expr;
    }

    private NillLiteral visit(NullLiteral node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        NillLiteral nillLiteral = new NillLiteral(_fileName, startLine, endLine, node);
        return nillLiteral;
    }

    private NumLiteral visit(NumberLiteral node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        String token = node.getToken();
        NumLiteral expr = null;
        try {
            Integer value = Integer.parseInt(token);
            IntLiteral literal = new IntLiteral(_fileName, startLine, endLine, node);
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
                LongLiteral literal = new LongLiteral(_fileName, startLine, endLine, node);
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
                FloatLiteral literal = new FloatLiteral(_fileName, startLine, endLine, node);
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
                DoubleLiteral literal = new DoubleLiteral(_fileName, startLine, endLine, node);
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
            NumLiteral literal = new NumLiteral(_fileName, startLine, endLine, node);
            literal.setValue(token);
            // simply set as int type
            AST ast = AST.newAST(AST.JLS8);
            Type type = ast.newPrimitiveType(PrimitiveType.INT);
            literal.setType(type);
            expr = literal;
        }

        return expr;
    }

    private ParenthesiszedExpr visit(ParenthesizedExpression node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());

        ParenthesiszedExpr parenthesiszedExpr = new ParenthesiszedExpr(_fileName, startLine, endLine, node);
        Expr expression = (Expr) process(node.getExpression(), scope);
        expression.setParent(parenthesiszedExpr);
        parenthesiszedExpr.setExpr(expression);
        parenthesiszedExpr.setType(expression.getType());

        return parenthesiszedExpr;
    }

    private PostfixExpr visit(PostfixExpression node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        PostfixExpr postfixExpr = new PostfixExpr(_fileName, startLine, endLine, node);

        Expr expression = (Expr) process(node.getOperand(), scope);
        expression.setParent(postfixExpr);
        postfixExpr.setExpression(expression);

        PostOperator postOperator = new PostOperator(_fileName, startLine, endLine, null);
        postOperator.setOperator(node.getOperator());
        postOperator.setParent(postfixExpr);
        postfixExpr.setOperator(postOperator);

        Type exprType = NodeUtils.parseExprType(expression, node.getOperator().toString(), null);

        postfixExpr.setType(exprType);

        switch (postOperator.toSrcString().toString()) {
            case "++":
            case "--":
                scope.addDefine(expression.toSrcString().toString(), postfixExpr);
        }

        return postfixExpr;
    }

    private PrefixExpr visit(PrefixExpression node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        PrefixExpr prefixExpr = new PrefixExpr(_fileName, startLine, endLine, node);

        Expr expression = (Expr) process(node.getOperand(), scope);
        expression.setParent(prefixExpr);
        prefixExpr.setExpression(expression);

        PrefixOperator prefixOperator = new PrefixOperator(_fileName, startLine, endLine, null);
        prefixOperator.setOperator(node.getOperator());
        prefixOperator.setParent(prefixExpr);
        prefixExpr.setOperator(prefixOperator);

        Type type = NodeUtils.parseExprType(null, node.getOperator().toString(), expression);
        prefixExpr.setType(type);

        switch (prefixExpr.toSrcString().toString()) {
            case "++":
            case "--":
                scope.addDefine(expression.toSrcString().toString(), prefixExpr);
        }

        return prefixExpr;
    }

    private StrLiteral visit(StringLiteral node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        StrLiteral literal = new StrLiteral(_fileName, startLine, endLine, node);

        literal.setValue(node.getLiteralValue());

        AST ast = AST.newAST(AST.JLS8);
        Type type = ast.newSimpleType(ast.newSimpleName("String"));
        literal.setType(type);

        return literal;
    }

    private SuperFieldAcc visit(SuperFieldAccess node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperFieldAcc superFieldAcc = new SuperFieldAcc(_fileName, startLine, endLine, node);

        SName identifier = (SName) process(node.getName(), scope);
        identifier.setParent(superFieldAcc);
        superFieldAcc.setIdentifier(identifier);
        identifier.setDataDependency(null);

        if (node.getQualifier() != null) {
            Label name = (Label) process(node.getQualifier(), scope);
            name.setParent(superFieldAcc);
            superFieldAcc.setName(name);
            name.setDataDependency(null);
        }

        Type exprType = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        superFieldAcc.setType(exprType);
        superFieldAcc.setDataDependency(scope.getDefines(superFieldAcc.toSrcString().toString()));

        return superFieldAcc;
    }

    private SuperMethodInv visit(SuperMethodInvocation node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperMethodInv superMethodInv = new SuperMethodInv(_fileName, startLine, endLine, node);

        SName sName = new SName(_fileName, startLine, endLine, node.getName());
        sName.setName(node.getName().getFullyQualifiedName());
        sName.setParent(superMethodInv);
        superMethodInv.setName(sName);
        sName.setDataDependency(null);

        if (node.getQualifier() != null) {
            Label label = (Label) process(node.getQualifier(), scope);
            label.setParent(superMethodInv);
            superMethodInv.setLabel(label);
        }

        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr expr = (Expr) process((ASTNode) object, scope);
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

    private SuperMethodRef visit(SuperMethodReference node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperMethodRef superMethodRef = new SuperMethodRef(_fileName, startLine, endLine, node);

        return superMethodRef;
    }

    private ThisExpr visit(ThisExpression node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ThisExpr thisExpr = new ThisExpr(_fileName, startLine, endLine, node);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        thisExpr.setType(type);

        return thisExpr;
    }

    private TyLiteral visit(TypeLiteral node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TyLiteral tyLiteral = new TyLiteral(_fileName, startLine, endLine, node);
        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        Type type = typeFromBinding(node.getAST(), node.getType().resolveBinding());
        mType.setType(type);
        mType.setParent(tyLiteral);
        tyLiteral.setValue(mType);
        tyLiteral.setType(type);

        return tyLiteral;
    }

    private TypeMethodRef visit(TypeMethodReference node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TypeMethodRef typeMethodRef = new TypeMethodRef(_fileName, startLine, endLine, node);
        return typeMethodRef;
    }

    private VarDeclarationExpr visit(VariableDeclarationExpression node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        VarDeclarationExpr varDeclarationExpr = new VarDeclarationExpr(_fileName, startLine, endLine, node);

        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        mType.setType(typeFromBinding(node.getAST(), node.getType().resolveBinding()));
        mType.setParent(varDeclarationExpr);
        varDeclarationExpr.setDeclType(mType);

        List<Vdf> vdfs = new ArrayList<>();
        for (Object object : node.fragments()) {
            Vdf vdf = (Vdf) process((ASTNode) object, scope);
            vdf.setParent(varDeclarationExpr);
            vdfs.add(vdf);
        }
        varDeclarationExpr.setVarDeclFrags(vdfs);

        return varDeclarationExpr;
    }

    private Vdf visit(VariableDeclarationFragment node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Vdf vdf = new Vdf(_fileName, startLine, endLine, node);

        SName identifier = (SName) process(node.getName(), scope);
        identifier.setParent(vdf);
        vdf.setName(identifier);
        identifier.setDataDependency(null);

        vdf.setDimensions(node.getExtraDimensions());

        if (node.getInitializer() != null) {
            Expr expression = (Expr) process(node.getInitializer(), scope);
            expression.setParent(vdf);
            vdf.setExpression(expression);
        }
        scope.addDefine(identifier.getName(), vdf);

        return vdf;
    }

    private Svd visit(SingleVariableDeclaration node, VScope scope) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Svd svd = new Svd(_fileName, startLine, endLine, node);

        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        mType.setType(typeFromBinding(node.getAST(), node.getType().resolveBinding()));
        mType.setParent(svd);
        svd.setDecType(mType);
        if (node.getInitializer() != null) {
            Expr initializer = (Expr) process(node.getInitializer(), scope);
            initializer.setParent(svd);
            svd.setInitializer(initializer);
        }

        SName name = (SName) process(node.getName(), scope);
        name.setParent(svd);
        svd.setName(name);
        name.setDataDependency(null);

        scope.addDefine(name.getName(), svd);
        return svd;
    }

    public Node process(ASTNode node) {
        return process(node, new VScope(null));
    }

    public Node process(ASTNode node, VScope scope) {
        if (node == null) {
            return null;
        }
        if (node instanceof AssertStatement) {
            return visit((AssertStatement) node, scope);
        } else if (node instanceof Block) {
            return visit((Block) node, scope);
        } else if (node instanceof BreakStatement) {
            return visit((BreakStatement) node, scope);
        } else if (node instanceof ConstructorInvocation) {
            return visit((ConstructorInvocation) node, scope);
        } else if (node instanceof ContinueStatement) {
            return visit((ContinueStatement) node, scope);
        } else if (node instanceof DoStatement) {
            return visit((DoStatement) node, scope);
        } else if (node instanceof EmptyStatement) {
            return visit((EmptyStatement) node, scope);
        } else if (node instanceof EnhancedForStatement) {
            return visit((EnhancedForStatement) node, scope);
        } else if (node instanceof ExpressionStatement) {
            return visit((ExpressionStatement) node, scope);
        } else if (node instanceof ForStatement) {
            return visit((ForStatement) node, scope);
        } else if (node instanceof IfStatement) {
            return visit((IfStatement) node, scope);
        } else if (node instanceof LabeledStatement) {
            return visit((LabeledStatement) node, scope);
        } else if (node instanceof ReturnStatement) {
            return visit((ReturnStatement) node, scope);
        } else if (node instanceof SuperConstructorInvocation) {
            return visit((SuperConstructorInvocation) node, scope);
        } else if (node instanceof SwitchCase) {
            return visit((SwitchCase) node, scope);
        } else if (node instanceof SwitchStatement) {
            return visit((SwitchStatement) node, scope);
        } else if (node instanceof SynchronizedStatement) {
            return visit((SynchronizedStatement) node, scope);
        } else if (node instanceof ThrowStatement) {
            return visit((ThrowStatement) node, scope);
        } else if (node instanceof TryStatement) {
            return visit((TryStatement) node, scope);
        } else if (node instanceof TypeDeclarationStatement) {
            return visit((TypeDeclarationStatement) node, scope);
        } else if (node instanceof VariableDeclarationStatement) {
            return visit((VariableDeclarationStatement) node, scope);
        } else if (node instanceof WhileStatement) {
            return visit((WhileStatement) node, scope);
        } else if (node instanceof Annotation) {
            return visit((Annotation) node, scope);
        } else if (node instanceof ArrayAccess) {
            return visit((ArrayAccess) node, scope);
        } else if (node instanceof ArrayCreation) {
            return visit((ArrayCreation) node, scope);
        } else if (node instanceof ArrayInitializer) {
            return visit((ArrayInitializer) node, scope);
        } else if (node instanceof Assignment) {
            return visit((Assignment) node, scope);
        } else if (node instanceof BooleanLiteral) {
            return visit((BooleanLiteral) node, scope);
        } else if (node instanceof CastExpression) {
            return visit((CastExpression) node, scope);
        } else if (node instanceof CharacterLiteral) {
            return visit((CharacterLiteral) node, scope);
        } else if (node instanceof ClassInstanceCreation) {
            return visit((ClassInstanceCreation) node, scope);
        } else if (node instanceof ConditionalExpression) {
            return visit((ConditionalExpression) node, scope);
        } else if (node instanceof CreationReference) {
            return visit((CreationReference) node, scope);
        } else if (node instanceof ExpressionMethodReference) {
            return visit((ExpressionMethodReference) node, scope);
        } else if (node instanceof FieldAccess) {
            return visit((FieldAccess) node, scope);
        } else if (node instanceof InfixExpression) {
            return visit((InfixExpression) node, scope);
        } else if (node instanceof InstanceofExpression) {
            return visit((InstanceofExpression) node, scope);
        } else if (node instanceof LambdaExpression) {
            return visit((LambdaExpression) node, scope);
        } else if (node instanceof MethodInvocation) {
            return visit((MethodInvocation) node, scope);
        } else if (node instanceof MethodReference) {
            return visit((MethodReference) node, scope);
        } else if (node instanceof Name) {
            return visit((Name) node, scope);
        } else if (node instanceof NullLiteral) {
            return visit((NullLiteral) node, scope);
        } else if (node instanceof NumberLiteral) {
            return visit((NumberLiteral) node, scope);
        } else if (node instanceof ParenthesizedExpression) {
            return visit((ParenthesizedExpression) node, scope);
        } else if (node instanceof PostfixExpression) {
            return visit((PostfixExpression) node, scope);
        } else if (node instanceof PrefixExpression) {
            return visit((PrefixExpression) node, scope);
        } else if (node instanceof StringLiteral) {
            return visit((StringLiteral) node, scope);
        } else if (node instanceof SuperFieldAccess) {
            return visit((SuperFieldAccess) node, scope);
        } else if (node instanceof SuperMethodInvocation) {
            return visit((SuperMethodInvocation) node, scope);
        } else if (node instanceof SuperMethodReference) {
            return visit((SuperMethodReference) node, scope);
        } else if (node instanceof ThisExpression) {
            return visit((ThisExpression) node, scope);
        } else if (node instanceof TypeLiteral) {
            return visit((TypeLiteral) node, scope);
        } else if (node instanceof TypeMethodReference) {
            return visit((TypeMethodReference) node, scope);
        } else if (node instanceof VariableDeclarationExpression) {
            return visit((VariableDeclarationExpression) node, scope);
        } else if (node instanceof AnonymousClassDeclaration) {
            return visit((AnonymousClassDeclaration) node, scope);
        } else if (node instanceof VariableDeclarationFragment) {
            return visit((VariableDeclarationFragment) node, scope);
        } else if (node instanceof SingleVariableDeclaration) {
            return visit((SingleVariableDeclaration) node, scope);
        } else if (node instanceof MethodDeclaration) {
            return visit((MethodDeclaration) node, scope);
        } else if (node instanceof CatchClause) {
            return visit((CatchClause) node, scope);
        } else {
            LevelLogger.error("UNKNOWN ASTNode type : " + node.toString());
            return null;
        }
    }

    private static Type typeFromBinding(AST ast, ITypeBinding typeBinding) {
        if (typeBinding == null) {
            return ast.newWildcardType();
        }

        if (typeBinding.isPrimitive()) {
            return ast.newPrimitiveType(
                    PrimitiveType.toCode(typeBinding.getName()));
        }

        if (typeBinding.isCapture()) {
            ITypeBinding wildCard = typeBinding.getWildcard();
            WildcardType capType = ast.newWildcardType();
            ITypeBinding bound = wildCard.getBound();
            if (bound != null) {
                capType.setBound(typeFromBinding(ast, bound),
                        wildCard.isUpperbound());
            }
            return capType;
        }

        if (typeBinding.isArray()) {
            Type elType = typeFromBinding(ast, typeBinding.getElementType());
            return ast.newArrayType(elType, typeBinding.getDimensions());
        }

        if (typeBinding.isParameterizedType()) {
            ParameterizedType type = ast.newParameterizedType(
                    typeFromBinding(ast, typeBinding.getErasure()));

            @SuppressWarnings("unchecked")
            List<Type> newTypeArgs = type.typeArguments();
            for (ITypeBinding typeArg : typeBinding.getTypeArguments()) {
                newTypeArgs.add(typeFromBinding(ast, typeArg));
            }

            return type;
        }

        if (typeBinding.isWildcardType()) {
            WildcardType type = ast.newWildcardType();
            if (typeBinding.getBound() != null) {
                type.setBound(typeFromBinding(ast, typeBinding.getBound()));
            }
            return type;
        }

//        if(typeBinding.isGenericType()) {
//            System.out.println(typeBinding.toString());
//            return typeFromBinding(ast, typeBinding.getErasure());
//        }

        // simple or raw type
        String qualName = typeBinding.getQualifiedName();
        if ("".equals(qualName)) {
            return ast.newWildcardType();
        }
        try {
            return ast.newSimpleType(ast.newName(qualName));
        } catch (Exception e) {
            return ast.newWildcardType();
        }
    }

    private Blk wrapBlock(Statement node, VScope scope) {
        Blk blk;
        if(node instanceof Block) {
            blk = (Blk) process(node, scope);
        } else {
            int startLine = _cunit.getLineNumber(node.getStartPosition());
            int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
            blk = new Blk(_fileName, startLine, endLine, node);
            List<Stmt> stmts = new ArrayList<>();
            Stmt stmt = (Stmt) process(node, scope);
            stmt.setParent(blk);
            stmts.add(stmt);
            blk.setStatement(stmts);
        }
        return blk;
    }
}

class VScope {

    private VScope _parent;
    private Map<String, Node> _varDefines = new HashMap<>();
    private Map<String, Node> _varUsed = new HashMap<>();
    private static Stack<Node> _stucture = new Stack<>();

    public VScope(VScope parent) {
        _parent = parent;
    }

    public void addDefine(String name, Node node) {
        if (name == null || node == null) return;
        _varDefines.put(name, node);
    }

    public Node getDefines(String name) {
        if(name == null) {
            return null;
        }
        return gDefines(name);
    }

    private Node gDefines(String name) {
        Node node = _varDefines.get(name);
        if (node == null && _parent != null) {
            return _parent.gDefines(name);
        }
        return node;
    }

    public void addUse(String name, Node node) {
        if(name == null || node == null) return;
        _varUsed.put(name, node);
    }

    public Node getUse(String name) {
        if(name == null) {
            return null;
        }
        return gUse(name);
    }

    private Node gUse(String name) {
        Node node = _varUsed.get(name);
        if(name == null && _parent != null) {
            return gUse(name);
        }
        return node;
    }

    public void pushStructure(Node node) {
        _stucture.push(node);
    }

    public void popStructure() {
        if(!_stucture.isEmpty()) {
            _stucture.pop();
        }
    }

    public Node peekStructure() {
        if(_stucture.isEmpty()) {
            return null;
        }
        return _stucture.peek();
    }

}
