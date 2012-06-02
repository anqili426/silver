package silAST.expressions

import silAST.source.SourceLocation
import silAST.domains.DomainPredicate
import terms._
import util.{GTermSequence, PTermSequence}
import silAST.symbols.logical.{UnaryConnective, BinaryConnective}
import silAST.programs.NodeFactory
import collection.Set
import collection.mutable.HashSet
import silAST.types.permissionType
import silAST.programs.symbols.{PredicateFactory, ProgramVariableSequence, ProgramVariable, Predicate}


trait PExpressionFactory extends NodeFactory with GExpressionFactory with PTermFactory {
  //////////////////////////////////////////////////////////////////////////
  protected[silAST] def migrate(e: PExpression) {
    if (expressions contains e)
      return

    e match {
      case ge: GExpression => super.migrate(ge)
      case ue: PUnaryExpression => {
        migrate(ue.operand1)
      }
      case be: PBinaryExpression => {
        migrate(be.operand1)
        migrate(be.operand2)
      }
      case dpe: PDomainPredicateExpression => {
        require(domainPredicates contains dpe.predicate)
        dpe.arguments.foreach(migrate(_))
      }
      case ee: PEqualityExpression => {
        migrate(ee.term1)
        migrate(ee.term2)
      }
      case pue: PUnfoldingExpression => {
        require(predicates contains pue.location.predicate)
        migrate(pue.expression)
        migrate(pue.permission)
        migrate(pue.location.receiver)
      }
    }
    addExpression(e)
  }

  //////////////////////////////////////////////////////////////////
  def makeProgramVariableSequence(vs: Seq[ProgramVariable],sourceLocation: SourceLocation,comment : List[String] = Nil): ProgramVariableSequence = {
    require(vs.forall(programVariables contains _))
    val result = new ProgramVariableSequence(vs)(sourceLocation,comment)
    programVariableSequences += result
    result
  }

  //////////////////////////////////////////////////////////////////////////
  def makePDomainPredicateExpression(p: DomainPredicate, args: PTermSequence,sourceLocation: SourceLocation,comment : List[String] = Nil): PDomainPredicateExpression = {
    require(domainPredicates contains p)
    args.foreach(migrate(_))

    (args) match {
      case (a: GTermSequence) => makeGDomainPredicateExpression(p, a,sourceLocation,comment)
      case _ => addExpression(new PDomainPredicateExpressionC(p, args)(sourceLocation,comment))
    }
  }
/*
  //////////////////////////////////////////////////////////////////////////
  def makePPredicateExpression(r: PTerm, p: Predicate,sourceLocation: SourceLocation,comment : List[String] = Nil): PPredicateExpression = {
    require(predicates contains p)
    migrate(r)

    addExpression(new PPredicateExpression(r, p)(sourceLocation,comment))
  }
  */
  //////////////////////////////////////////////////////////////////////////
  def makePUnaryExpression(op: UnaryConnective, e1: PExpression,sourceLocation: SourceLocation,comment : List[String] = Nil): PUnaryExpression = {
    migrate(e1)

    (e1) match {
      case (e1: GExpression) => makeGUnaryExpression(op, e1,sourceLocation,comment)
      case _ => addExpression(new PUnaryExpressionC(op, e1)(sourceLocation,comment))
    }
  }

  //////////////////////////////////////////////////////////////////////////
  def makePBinaryExpression(op: BinaryConnective, e1: PExpression, e2: PExpression,sourceLocation: SourceLocation,comment : List[String] = Nil): PBinaryExpression = {
    migrate(e1)
    migrate(e2)

    (e1, e2) match {
      case (e1: GExpression, e2: GExpression) => makeGBinaryExpression(op, e1, e2,sourceLocation,comment)
      case _ => addExpression(new PBinaryExpressionC(op, e1, e2)(sourceLocation,comment))
    }
  }

  //////////////////////////////////////////////////////////////////////////
  def makePEqualityExpression(t1: PTerm, t2: PTerm,sourceLocation: SourceLocation,comment : List[String] = Nil): PEqualityExpression = {
    migrate(t1)
    migrate(t2)

    (t1, t2) match {
      case (t1: GTerm, t2: GTerm) => makeGEqualityExpression(t1, t2,sourceLocation,comment)
      case _ => addExpression(new PEqualityExpressionC(t1, t2)(sourceLocation,comment))
    }
  }

  //////////////////////////////////////////////////////////////////////////
  def makePUnfoldingExpression(r : PTerm, pf: PredicateFactory, perm : PTerm, e: PExpression,sourceLocation: SourceLocation,comment : List[String] = Nil): UnfoldingExpression = {
    require(predicates contains pf.pPredicate)
    require(perm.dataType == permissionType)
    migrate(r)
    migrate(perm)
    migrate(e)

    addExpression(new PUnfoldingExpression(new PPredicateLocation(r,pf.pPredicate),perm,e)(sourceLocation,comment))
  }

  //////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////
  protected[silAST] def predicates: Set[Predicate]

  protected[silAST] val programVariableSequences = new HashSet[ProgramVariableSequence]
}