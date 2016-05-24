package notabot;

import java.util.Stack;

import org.ggp.base.util.statemachine.Move;

public class MovePath {

	private final Stack<Move> moveStack;
	private final int endStateGoal;

	public MovePath(int endStateGoal){
		moveStack = new Stack<Move>();
		this.endStateGoal = endStateGoal;
	}

	public void pushMove(Move move){
		moveStack.push(move);
	}

	public Move popMove(){
		return moveStack.pop();
	}

	public int getEndStateGoal(){
		return endStateGoal;
	}

}
