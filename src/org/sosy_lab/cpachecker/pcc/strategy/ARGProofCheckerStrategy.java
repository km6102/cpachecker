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
package org.sosy_lab.cpachecker.pcc.strategy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.pcc.propertychecker.NoTargetStateChecker;

@Options
public class ARGProofCheckerStrategy extends AbstractARGStrategy {

  private final ProofChecker checker;
  private Set<ARGState> postponedStates;
  private Set<ARGState> waitingForUnexploredParents;
  private Set<ARGState> inWaitlist;


  public ARGProofCheckerStrategy(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final ProofChecker pChecker)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pChecker instanceof PropertyCheckerCPA ? ((PropertyCheckerCPA) pChecker).getPropChecker()
        : new NoTargetStateChecker(), pShutdownNotifier);
    checker = pChecker;
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {


   if(!super.checkCertificate(pReachedSet)){
     return false;
   }

   // TODO later remove them
   ARGState initialState = (ARGState) pReachedSet.popFromWaitlist();
   Precision initialPrecision = pReachedSet.getPrecision(initialState);

    Set<ARGState> waitingForUnexploredParents = new HashSet<>();
    Set<ARGState> inWaitlist = new HashSet<>();
    inWaitlist.add(root);

    boolean unexploredParent;

    do {
      prepareNextWaitlistIteration(pReachedSet);

      while (pReachedSet.hasWaitingState()) {
        shutdownNotifier.shutdownIfNecessary();

        stats.countIterations++;
        ARGState state = (ARGState) pReachedSet.popFromWaitlist();
        inWaitlist.remove(state);

        logger.log(Level.FINE, "Looking at state", state);

        stats.propertyCheckingTimer.start();
        if (!propChecker.satisfiesProperty(state)) {
          stats.propertyCheckingTimer.stop();
          return false;
        }
        stats.propertyCheckingTimer.stop();

        if (state.isCovered()) {

          logger.log(Level.FINER, "State is covered by another abstract state; checking coverage");
          ARGState coveringState = state.getCoveringState();

          if (!pReachedSet.contains(coveringState)) {
            postponedStates.add(state);
            continue;
          }

          stats.stopTimer.start();
          if (!isCoveringCycleFree(state)) {
            stats.stopTimer.stop();
            logger.log(Level.WARNING, "Found cycle in covering relation for state", state);
            return false;
          }
          if (!checkCovering(state, coveringState, initialPrecision)) {
            stats.stopTimer.stop();
            logger.log(Level.WARNING, "State", state, "is not covered by", coveringState);
            return false;
          }
          stats.stopTimer.stop();
        } else {


          stats.transferTimer.start();
          Collection<ARGState> successors = state.getChildren();
          logger.log(Level.FINER, "Checking abstract successors", successors);

          if (!checker.areAbstractSuccessors(state, null, successors)) {
            stats.transferTimer.stop();
            logger.log(Level.WARNING, "State", state, "has other successors than", successors);
            return false;
          }
          stats.transferTimer.stop();

          for (ARGState e : successors) {
            unexploredParent = false;
            for (ARGState p : e.getParents()) {
              if (!pReachedSet.contains(p) || inWaitlist.contains(p)) {
                waitingForUnexploredParents.add(e);
                unexploredParent = true;
                break;
              }
            }
            if (unexploredParent) {
              continue;
            }
            if (pReachedSet.contains(e)) {
              // state unknown parent of e
              logger.log(Level.WARNING, "State", e, "has other parents than", e.getParents());
              return false;
            } else {
              waitingForUnexploredParents.remove(e);
              pReachedSet.add(e, initialPrecision);
              inWaitlist.add(e);
            }
          }
        }
      }
    } while (!isCheckComplete());

    return waitingForUnexploredParents.isEmpty();
  }

  @Override
  protected void initChecking(final ARGState pRoot) {
    postponedStates = new HashSet<>();
    waitingForUnexploredParents = new HashSet<>();
    inWaitlist = new HashSet<>();
    inWaitlist.add(pRoot);
  }

  @Override
  protected boolean checkForStatePropertyAndOtherStateActions(ARGState pState) {
    inWaitlist.remove(pState);
    return super.checkForStatePropertyAndOtherStateActions(pState);
  }

  @Override
  protected boolean checkCovering(final ARGState pCovered, final ARGState pCovering, final Precision pPrecision) throws CPAException,
      InterruptedException {
    return checker.isCoveredBy(pCovered, pCovering);
  }

  @Override
  protected boolean isCheckSuccessful() {
    return waitingForUnexploredParents.isEmpty();
  }

  @Override
  protected boolean isCheckComplete() {
    return postponedStates.isEmpty();
  }

  @Override
  protected boolean prepareNextWaitlistIteration(final ReachedSet pReachedSet) {
    for (ARGState e : postponedStates) {
      if (!pReachedSet.contains(e.getCoveringState())) {
        logger.log(Level.WARNING, "Covering state", e.getCoveringState(), "was not found in reached set");
        return false;
      }
      pReachedSet.reAddToWaitlist(e);
    }
    postponedStates.clear();
    return true;
  }

  @Override
  protected boolean checkSuccessors(final ARGState pPredecessor, final Collection<ARGState> pSuccessors,
      final Precision pPrecision) throws CPATransferException, InterruptedException {
    return checker.areAbstractSuccessors(pPredecessor, null, pSuccessors);
  }

  @Override
  protected boolean addSuccessors(final Collection<ARGState> pSuccessors, final ReachedSet pReachedSet, final Precision pPrecision) {
    boolean unexploredParent;
    for (ARGState e : pSuccessors) {
      unexploredParent = false;
      for (ARGState p : e.getParents()) {
        if (!pReachedSet.contains(p) || inWaitlist.contains(p)) {
          waitingForUnexploredParents.add(e);
          unexploredParent = true;
          break;
        }
      }
      if (unexploredParent) {
        continue;
      }
      if (pReachedSet.contains(e)) {
        // state unknown parent of e
        logger.log(Level.WARNING, "State", e, "has other parents than", e.getParents());
        return false;
      } else {
        waitingForUnexploredParents.remove(e);
        pReachedSet.add(e, pPrecision);
        inWaitlist.add(e);
      }
    }
    return true;
  }

  @Override
  protected boolean treatStateIfCoveredByUnkownState(ARGState pCovered, ARGState pCoveringState, ReachedSet pReachedSet,
      Precision pPrecision) {
    postponedStates.add(pCovered);
    return true;
  }
}
