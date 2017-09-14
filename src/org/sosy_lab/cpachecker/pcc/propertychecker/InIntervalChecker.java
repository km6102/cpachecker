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
package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.interval.IntegerInterval;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.cpa.interval.UnifyAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public class InIntervalChecker extends PerElementPropertyChecker {

  private final String label;
  private final String varName;
  private final NumberInterface allowedValues;

  public InIntervalChecker(final String pVariableName, final String pLabel,
      // Necessary because called reflectively
      // TODO Better usability would be to pass Configuration object
      // and define all user-specified parameters as individual @Option fields in this class.
      @SuppressWarnings("unused") final String pMode,
      final String pMin,
      final String pMax) {
    label = pLabel;
    varName = pVariableName;
    allowedValues = new IntegerInterval(Long.parseLong(pMin), Long.parseLong(pMax));

  }

  public InIntervalChecker(final String pVariableName, final String pLabel, final String pMode, final String pValue) {
    this(pVariableName, pLabel, pMode, Integer.parseInt(pMode) == 0 ? pValue : Long.toString(Long.MIN_VALUE),
        Integer.parseInt(pMode) == 0 ? Long.toString(Long.MAX_VALUE) : pValue);
  }

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck) throws UnsupportedOperationException {
    CFANode node = AbstractStates.extractLocation(pElemToCheck);
    if (node instanceof CLabelNode && ((CLabelNode) node).getLabel().equals(label)) {
        UnifyAnalysisState state = AbstractStates.extractStateByType(pElemToCheck, UnifyAnalysisState.class);
      if (state != null) {
        NumberInterface interval = state.getElement(MemoryLocation.valueOf(varName));
        //TODO Number instead Long
        if (interval != null && interval.getHigh().longValue() <= allowedValues.getHigh().longValue()
            && interval.getLow().longValue() >= allowedValues.getLow().longValue()) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

}
