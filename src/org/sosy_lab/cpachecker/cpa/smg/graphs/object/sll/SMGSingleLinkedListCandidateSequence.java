/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import com.google.common.collect.Iterables;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractListCandidateSequence;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinSubSMGsForAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGMemoryPath;

public class SMGSingleLinkedListCandidateSequence extends SMGAbstractListCandidateSequence<SMGSingleLinkedListCandidate> {

  public SMGSingleLinkedListCandidateSequence(SMGSingleLinkedListCandidate pCandidate,
      int pLength, SMGJoinStatus pSmgJoinStatus, boolean pIncludesSll) {
    super(pCandidate, pLength, pSmgJoinStatus, pIncludesSll);
  }

  @Override
  public CLangSMG execute(CLangSMG pSMG, SMGState pSmgState) throws SMGInconsistentException {
    SMGObject prevObject = candidate.getStartObject();
    long nfo = candidate.getShape().getNfo();

    pSmgState.pruneUnreachable();

    // Abstraction not reachable
    if(!pSMG.getHeapObjects().contains(prevObject)) {
      return pSMG;
    }

    for (int i = 1; i < length; i++) {

      SMGEdgeHasValue nextEdge = Iterables.getOnlyElement(pSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(prevObject).filterAtOffset(nfo)));
      SMGObject nextObject = pSMG.getPointer(nextEdge.getValue()).getObject();

      if (length > 1) {
        SMGJoinSubSMGsForAbstraction jointest =
            new SMGJoinSubSMGsForAbstraction(new CLangSMG(pSMG), prevObject, nextObject, candidate,
                pSmgState);

        if (!jointest.isDefined()) {
          return pSMG;
        }
      }

      SMGJoinSubSMGsForAbstraction join =
          new SMGJoinSubSMGsForAbstraction(pSMG, prevObject, nextObject, candidate, pSmgState);

      if(!join.isDefined()) {
        throw new AssertionError("Unexpected join failure while abstracting longest mergeable sequence");
      }

      SMGObject newAbsObj = join.getNewAbstractObject();

      for (SMGEdgePointsTo pte : SMGUtils.getPointerToThisObject(nextObject, pSMG)) {
        pSMG.removePointsToEdge(pte.getValue());

        if (pte.getTargetSpecifier() == SMGTargetSpecifier.ALL) {
          SMGEdgePointsTo newPte = new SMGEdgePointsTo(pte.getValue(), newAbsObj, pte.getOffset(),
              SMGTargetSpecifier.ALL);
          pSMG.addPointsToEdge(newPte);
        }
      }

      addPointsToEdges(pSMG, prevObject, newAbsObj, SMGTargetSpecifier.FIRST);

      SMGEdgeHasValue nextObj2hve = Iterables.getOnlyElement(pSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(nextObject).filterAtOffset(nfo)));

      for (SMGObject obj : join.getNonSharedObjectsFromSMG1()) {
        pSMG.removeHeapObjectAndEdges(obj);
      }

      for (SMGObject obj : join.getNonSharedObjectsFromSMG2()) {
        pSMG.removeHeapObjectAndEdges(obj);
      }

      pSMG.removeHeapObjectAndEdges(nextObject);
      pSMG.removeHeapObjectAndEdges(prevObject);
      prevObject = newAbsObj;

      SMGEdgeHasValue nfoHve = new SMGEdgeHasValue(nextObj2hve.getType(), nextObj2hve.getOffset(), newAbsObj, nextObj2hve.getValue());
      pSMG.addHasValueEdge(nfoHve);
      pSmgState.pruneUnreachable();
    }

    return pSMG;
  }

  @Override
  public String toString() {
    return "SMGSingleLinkedListCandidateSequence [candidate=" + candidate + ", length=" + length + "]";
  }

  @Override
  public SMGAbstractionBlock createAbstractionBlock(SMGState pSmgState) {
    Map<SMGObject, SMGMemoryPath> map = pSmgState.getHeapObjectMemoryPaths();
    SMGMemoryPath pPointerToStartObject = map.get(candidate.getStartObject());
    return new SMGSingleLinkedListCandidateSequenceBlock(candidate.getShape(), length,
        pPointerToStartObject);
  }
}