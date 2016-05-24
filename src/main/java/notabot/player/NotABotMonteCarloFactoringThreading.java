package notabot.player;

import java.util.ArrayList;
import java.util.List;

import notabot.MoveScore;
import notabot.NotABot;
import notabot.gametree.GameTree;
import notabot.gametree.GameTreeFactoring;
import notabot.propnet.NotABotPropNetStateMachine;

import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class NotABotMonteCarloFactoringThreading extends NotABot {
	GameTreeFactoring[] trees;
	NotABotPropNetStateMachine propNet;
	int numSubgames;

	Thread treeThread;
	boolean gameOn = true;

	boolean usingPropNet = true;
	GameTree backupTree;
	StateMachine backupSM;

	@Override
	protected void runMetaGame() {
		System.out.println("RUNNING METAGAME");

		// ASSUMES USING CachedStateMachne CONTAINING NotABotPropNetStateMachine
		StateMachine sm = ((CachedStateMachine) getStateMachine()).getBackingStateMachine();
		if (sm instanceof NotABotPropNetStateMachine){
			usingPropNet = true;
			propNet = (NotABotPropNetStateMachine) sm;

			// build game tree for each subgame
			numSubgames = propNet.getNumSubgames();
			trees = new GameTreeFactoring[numSubgames];
			for (int i=0; i<numSubgames; i++){
				trees[i] = new GameTreeFactoring(propNet, getCurrentState(), getRole(), i);
			}

			treeThread = new Thread(){
				@Override
				public void run(){
					while (gameOn){
						for (int i=0; i<numSubgames; i++){
							trees[i].runSample();
						}
					}
				}
			};
			treeThread.start();
		}
		else{
			usingPropNet = false;
			backupSM = sm;

			backupTree = new GameTree(backupSM, getCurrentState(), getRole());
			treeThread = new Thread(){
				@Override
				public void run(){
					while (gameOn){
						backupTree.runSample();
					}
				}
			};
			treeThread.start();
		}



		sleepUntilTimeout();
	}

	@Override
	protected Move getBestMove() throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {

		System.out.println("________________________________________________\n");

		if (!usingPropNet){
			return backupGetBestMove();
		}

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
		//sampleUntilTimeout();
		sleepUntilTimeout();

		// compute best score among all trees
		//System.out.println("ATTEMPT MUTEX");
		MoveScore bestMoveScore = null;
		for (int i=0; i<numSubgames; i++) {
			int numSamples = 0;
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

	private Move backupGetBestMove() throws MoveDefinitionException,
		TransitionDefinitionException, GoalDefinitionException {
		backupTree.traverse(getCurrentState());
		//previewBestMove();

		// run samples
		sleepUntilTimeout();

		// compute best move
		Move move = backupTree.getBestMove(true);
		if (move == null){
			System.out.println("TREE RETURNED NULL MOVE");
			move = getStateMachine().getLegalMoves(getCurrentState(), getRole()).get(0);
		}
		return move;
	}

	private void sleepUntilTimeout(){
		try {
			Thread.sleep(timeLeft());
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}



	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub
		gameOn = false;
		try {
			treeThread.join();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


}
