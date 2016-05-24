package notabot.player;

import java.util.ArrayList;
import java.util.List;

import notabot.MoveScore;
import notabot.NotABot;
import notabot.gametree.GameTreeFactoring;
import notabot.propnet.NotABotPropNetStateMachine;

import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class NotABotMonteCarloFactoring extends NotABot {

	GameTreeFactoring[] trees;
	NotABotPropNetStateMachine propNet;
	int numSubgames;

	@Override
	protected void runMetaGame() {
		System.out.println("RUNNING METAGAME");

		// ASSUMES USING CachedStateMachne CONTAINING NotABotPropNetStateMachine
		propNet = (NotABotPropNetStateMachine) ((CachedStateMachine) getStateMachine()).getBackingStateMachine();

		// build game tree for each subgame
		numSubgames = propNet.getNumSubgames();
		trees = new GameTreeFactoring[numSubgames];
		for (int i=0; i<numSubgames; i++){
			trees[i] = new GameTreeFactoring(propNet, getCurrentState(), getRole(), i);
		}

		sampleUntilTimeout();
	}

	@Override
	protected Move getBestMove() throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {

		System.out.println("________________________________________________\n");

		List<List<GdlTerm>> history = getMatch().getMoveHistory();
		if (history.size()>0){
			// Get list of moves from last turn
			List<Move> lastMoves = new ArrayList<Move>();
			for (GdlTerm term: history.get(history.size()-1)){
				lastMoves.add(new Move(term));
			}

			// traverse all trees
			for (int i=0; i<numSubgames; i++){
				trees[i].traverse(lastMoves);
			}
		}

		// sample trees until timeout
		sampleUntilTimeout();

		// compute best score among all trees
		MoveScore bestMoveScore = null;
		for (int i=0; i<numSubgames; i++) {
			System.out.println("SUBGAME ("+i+"):");
			MoveScore moveScore = trees[i].getBestMoveScore(true);

			if (moveScore.getMove()!=null){
				if (bestMoveScore == null || moveScore.getScore() > bestMoveScore.getScore()){
					bestMoveScore = moveScore;
				}
			}
			System.out.println("\t\tBEST MOVE: "+moveScore.getMove() + " : " + moveScore.getScore()+"\n");
		}

		if (bestMoveScore==null){
			return getStateMachine().getLegalMoves(getCurrentState(), getRole()).get(0);
		}

		return bestMoveScore.getMove();
	}


	private void sampleUntilTimeout(){
		// reset tree depth charges
		int[] numSamples = new int[numSubgames];
		for (int i=0; i<numSubgames; i++){
			trees[i].resetDepthChargeCounter();
			numSamples[i] = 0;
		}

		// iterate until timeout
		while (!NotABot.hasTimedOut()){
			for (int i=0; i<numSubgames; i++){
				trees[i].runSample();
				numSamples[i]++;
				if (NotABot.hasTimedOut()) break;
			}
		}
		// print out depth charge info
		for (int i=0; i<numSubgames; i++){
			System.out.println("SUBGAME ("+i+") - SAMPLES ("+numSamples[i]+") - CHARGES ("+trees[i].getNumDepthCharges()+")");
		}
		System.out.println();

	}

}
