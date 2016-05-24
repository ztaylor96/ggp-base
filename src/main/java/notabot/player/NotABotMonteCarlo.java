package notabot.player;

import notabot.NotABot;
import notabot.gametree.GameTree;

import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class NotABotMonteCarlo extends NotABot {

	GameTree tree;

	@Override
	protected void runMetaGame() {
		System.out.println("RUNNING METAGAME");

		// initialize tree
		tree = new GameTree(getStateMachine(), getCurrentState(), getRole());

		// run samples
		sampleUntilTimeout();
		System.out.println();
	}

	@Override
	protected Move getBestMove() throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {
		// update tree with moves from last turn
		tree.traverse(getCurrentState());
		//previewBestMove();

		// run samples
		sampleUntilTimeout();

		// compute best move
		Move move = tree.getBestMove(true);
		if (move == null){
			System.out.println("TREE RETURNED NULL MOVE");
			move = getStateMachine().getLegalMoves(getCurrentState(), getRole()).get(0);
		}
		return move;
	}

	private void sampleUntilTimeout(){
		// run samples until time runs out
		int numSamples = 0;
		tree.resetDepthChargeCounter();
		while (!NotABot.hasTimedOut()){
			tree.runSample();
			numSamples++;
		}
		System.out.println("NUM SAMPLES RAN: " + numSamples);
		System.out.println("NUM DEPTH CHARGES: " + tree.getNumDepthCharges());

	}

	private void previewBestMove(){
		Move currBestMove = tree.getBestMove(false);
		System.out.println("CURRENT BEST MOVE: " + currBestMove);
	}

}
