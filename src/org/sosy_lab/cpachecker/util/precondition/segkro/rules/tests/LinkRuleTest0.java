/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.precondition.segkro.rules.tests;

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;

import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.LinkRule;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ArrayFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;

import com.google.common.collect.Lists;


public class LinkRuleTest0 extends AbstractRuleTest0 {

  private LinkRule lr;

  private IntegerFormula _0;
  private IntegerFormula _1;
  private IntegerFormula _i;
  private ArrayFormula<IntegerFormula, IntegerFormula> _b;

  private IntegerFormula _x;
  private IntegerFormula _i_plus_1;
  private BooleanFormula _b_at_x_NOTEQ_0;
  private IntegerFormula _j;
  private IntegerFormula _k;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    lr = new LinkRule(solver, matcher);

    _0 = ifm.makeNumber(0);
    _1 = ifm.makeNumber(1);
    _i = ifm.makeVariable("i");
    _j = ifm.makeVariable("j");
    _k = ifm.makeVariable("k");
    _x = ifm.makeVariable("x");
    _b = afm.makeArray("b", NumeralType.IntegerType, NumeralType.IntegerType);

    _i_plus_1 = ifm.add(_i, _1);

    _b_at_x_NOTEQ_0 = bfm.not(ifm.equal(afm.select(_b, _x), _0));
  }

  @Test
  public void testConclusion1() throws SolverException, InterruptedException {

    // forall x in [i+1] . b[x] != 0
    // forall x in [i] .   b[x] != 0

    final BooleanFormula _FORALL_i = qmgr.forall(
        Lists.newArrayList(_x),
        bfm.and(
            Lists.newArrayList(
              _b_at_x_NOTEQ_0,
              ifm.greaterOrEquals(_x, _j),
              ifm.lessOrEquals(_x, _i)
            )));

    final BooleanFormula _FORALL_i_plus_1 = qmgr.forall(
        Lists.newArrayList(_x),
        bfm.and(
            Lists.newArrayList(
              _b_at_x_NOTEQ_0,
              ifm.greaterOrEquals(_x, _i_plus_1),
              ifm.lessOrEquals(_x, _k)
            )));

    final Set<BooleanFormula> result = lr.applyWithInputRelatingPremises(
        Lists.newArrayList(
            _FORALL_i,
            _FORALL_i_plus_1));

    assertThat(result).isNotEmpty();
  }

}
