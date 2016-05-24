package notabot.visualizer;

import java.util.List;

import notabot.gametree.GameTreeNode;

import org.ggp.base.util.statemachine.Move;

import processing.core.PApplet;

@SuppressWarnings("serial")
public class NotABotTreeVisualizer extends PApplet{

	public static final int VIS_WIDTH = 800;
	public static final int VIS_HEIGHT = 600;
	public static final int VIS_FPS = 30;

	private GameTreeNode root;
	private List<Move> playerMoves;
	private int numMoves;
	private int numChildren;
	private int[] moveSamples;
	private float maxMoveSamples;


	public void setRoot(GameTreeNode newRoot){
		root = newRoot;
		playerMoves = root.getPlayerMoves();
		numMoves = playerMoves.size();
		numChildren = root.getNumChildren();
		moveSamples = new int[numMoves];
		maxMoveSamples = 1;

		for (int i=0; i<numMoves; i++){
			moveSamples[i] = getMoveVisits(i);
			maxMoveSamples = max(maxMoveSamples, moveSamples[i]);
		}
	}

	public void sample(int moveIndex){
		if (moveIndex >= numMoves || moveIndex < 0){
			System.out.println("VISUALIZER SAMPLED WITH BAD MOVE INDEX");
		}
		moveSamples[moveIndex]++;
		maxMoveSamples = max(maxMoveSamples, moveSamples[moveIndex]);
	}

	@Override
	public void setup(){
		size(VIS_WIDTH, VIS_HEIGHT);
		background(255);
		frameRate(VIS_FPS);
		fill(0);
		noStroke();
		textAlign(LEFT, TOP);
	}

	@Override
	public void draw(){
		if (root == null) return;
		background(255);

		float thick = (float) (height) / (numMoves);
		float offset = min(thick/4, 20);
		float bar = min(thick/2, 30);
		float y;
		float score;
		try{
			for (int i=0; i<numMoves; i++){
				Move m = playerMoves.get(i);
				y = thick*i;

				score = getMoveScore(i);
				fill(0,255,0);
				rect(0, y+offset, width/2*(moveSamples[i]/maxMoveSamples), bar);
				fill(255,0,0);
				rect(width/2, y+offset, width*score/200, bar);
				fill(0);
				line(0,y,width,y);
				if (m==null)
					text("MOVE: null", 0, y);
				else
					text("MOVE: "+m.toString(), 0, y);
				text("SAMPLES: "+moveSamples[i], width/4, y);
				text("SCORE: "+(int)score, width/2, y);
			}
			line(width/2,0,width/2,height);
		}
		catch (Exception e){
		}
	}

	private int getMoveVisits(int moveIndex){
		int numVisits = 0;
		int combo = moveIndex;
		int numOppMoves = numChildren/numMoves;
		for (int j=0; j<numOppMoves; j++){
			GameTreeNode child = root.getChild(combo);
			combo += numMoves;
			if (child!=null){
				numVisits += child.getNumVisits();
			}
		}
		return numVisits;
	}

	private float getMoveScore(int moveIndex){
		float score = 0;
		int combo = moveIndex;
		int numOppMoves = numChildren/numMoves;
		for (int j=0; j<numOppMoves; j++){
			GameTreeNode child = root.getChild(combo);
			combo += numMoves;
			if (child!=null && child.getNumVisits()!=0){
				score += child.getScore();
			}
		}
		return score/numOppMoves;
	}

}
