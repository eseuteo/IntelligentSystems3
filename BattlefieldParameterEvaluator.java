package neural;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.train.MLTrain;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;

import robocode.BattleResults;
import robocode.control.*;
import robocode.control.events.*;

public class BattlefieldParameterEvaluator {
	// Minimum allowable battlefield size is 400
	final static int MAXBATTLEFIELDSIZE = 4000;
	
	// Minimum allowable gun cooling rate is 0.1
	final static double MAXGUNCOOLINGRATE = 10;
	
	final static int NUMBATTLEFIELDSIZES = 601;
	final static int NUMCOOLINGRATES = 501;
	final static int NUMSAMPLES = 1000; // Number of inputs for the multilayer perceptron (size of the input vectors)
	final static int NUM_NN_INPUTS = 2; // Number of hidden neurons of the neural network
	final static int NUM_NN_HIDDEN_UNITS = 50; // Number of epochs for training
	final static int NUM_TRAINING_EPOCHS = 100000;
	
	final static int MIN_TRAINING_EPOCHS = 50;
	static int NdxBattle;
	
	static double[] FinalScore1;
	static double[] FinalScore2;

public static void main(String[] args) {
	double[] BattlefieldSize=new double[NUMSAMPLES];
	double[] GunCoolingRate=new double[NUMSAMPLES];
	FinalScore1 = new double[NUMSAMPLES];
	FinalScore2 = new double[NUMSAMPLES];
	Random rng = new Random(15L);

	// Disable log messages from Robocode
	RobocodeEngine.setLogMessagesEnabled(false);
	
	// Create the RobocodeEngine 
	// Run from C:/Robocode 
	RobocodeEngine engine= new RobocodeEngine(new java.io.File("C:/Robocode"));
	
	// Add our own battle listener to the RobocodeEngine
	engine.addBattleListener(new BattleObserver());
	
	// Show the Robocode battle view 
	engine.setVisible(false);
	
	// Setup the battle specification
	// Setup battle parameters 
	int numberOfRounds =1;
	long inactivityTime = 100;

	int sentryBorderSize = 50;
	boolean hideEnemyNames = false;
	
	//Get the robots and set up their initial states
	RobotSpecification[] competingRobots = engine.getLocalRepository("sample.RamFire,sample.TrackFire");
	RobotSetup[] robotSetups = new RobotSetup[2];
	
	for(NdxBattle=0; NdxBattle < NUMSAMPLES; NdxBattle++){
		//Choose the battlefield size and guncooling rate 
		BattlefieldSize[NdxBattle] = MAXBATTLEFIELDSIZE * (0.1+0.9 * rng.nextDouble());
		GunCoolingRate[NdxBattle] = MAXGUNCOOLINGRATE * (0.1+0.9 * rng.nextDouble());

		//Create the battlefield
		BattlefieldSpecification battlefield = new BattlefieldSpecification((int)BattlefieldSize[NdxBattle], (int)BattlefieldSize[NdxBattle]);

		//Set the robot positions
		robotSetups[0] = new RobotSetup(BattlefieldSize[NdxBattle]/2.0, BattlefieldSize[NdxBattle]/3.0,0.0);
		robotSetups[1] = new RobotSetup(BattlefieldSize[NdxBattle]/2.0, 2.0 * BattlefieldSize[NdxBattle]/3.0,0.0);

		//Prepare the battle specification
		BattleSpecification battleSpec = new
		BattleSpecification(battlefield, numberOfRounds, inactivityTime, GunCoolingRate[NdxBattle], sentryBorderSize, hideEnemyNames, competingRobots, robotSetups);
		//Run our specified battle and let it run till it is over
		engine.runBattle(battleSpec, true);

		//waits till the battle finishes
	}
	
	//Cleanup our RobocodeEngine
	engine.close();
//	System.out.println(Arrays.toString(BattlefieldSize));
//	System.out.println(Arrays.toString(GunCoolingRate));
	System.out.println(Arrays.toString(FinalScore1));
	System.out.println(Arrays.toString(FinalScore2));
	
	//Create the training dataset for the neural network
	double[][] RawInputs = new double[NUMSAMPLES*2/3][NUM_NN_INPUTS];
	double[][] RawOutputs = new double[NUMSAMPLES*2/3][1];
	double[][] trainingSetInputs = new double[NUMSAMPLES-NUMSAMPLES*2/3][NUM_NN_INPUTS];
	double[][] trainingSetOutputs = new double[NUMSAMPLES-NUMSAMPLES*2/3][1];
	int NdxSample;
	for( NdxSample=0; NdxSample < NUMSAMPLES*2/3; NdxSample++){
	// IMPORTANT:normalize the inputs and the outputs to the interval [0,1]
		RawInputs[NdxSample][0] = BattlefieldSize[NdxSample]/MAXBATTLEFIELDSIZE;
		RawInputs[NdxSample][1] = GunCoolingRate[NdxSample]/MAXGUNCOOLINGRATE;
		RawOutputs[NdxSample][0] = FinalScore1[NdxSample]/250;
	}
	int i=0;
	while (NdxSample < NUMSAMPLES) {
		trainingSetInputs[i][0] = BattlefieldSize[NdxSample]/MAXBATTLEFIELDSIZE;
		trainingSetInputs[i][1] = GunCoolingRate[NdxSample]/MAXGUNCOOLINGRATE;
		trainingSetOutputs[i][0] = FinalScore1[NdxSample]/250;
		NdxSample++;
		i++;
	}
	
	
	
	
	BasicNeuralDataSet MyDataSet = new BasicNeuralDataSet(RawInputs, RawOutputs);
	BasicNeuralDataSet MyTrainingSet = new BasicNeuralDataSet(trainingSetInputs,trainingSetOutputs);
	//Create and train the neural network... 
	BasicNetwork  nn = new BasicNetwork();
	nn.addLayer(new BasicLayer(null, true, NUM_NN_INPUTS));
	nn.addLayer(new BasicLayer(new ActivationSigmoid(), true, NUM_NN_HIDDEN_UNITS));
	nn.addLayer(new BasicLayer(new ActivationSigmoid(), false, 1));
	nn.getStructure().finalizeStructure();
	nn.reset();
	
	System.out.println("Training network...");
	//TODO NO IDEA WHICH TRAINING FUNCTION TO USE
		MLTrain train = new ResilientPropagation(nn, MyDataSet);
		
	int epoch = 1 ;
	double previousVer= Double.MAX_VALUE;
	int limit = 10;
	do {
	train.iteration();
	//System.out.println("Epoch #" + epoch + " Error : "+ train.getError());
	epoch++;
	
	double err =nn.calculateError(MyTrainingSet);
	if(err>previousVer){
		limit--;
		
	}else{
		limit = 3;
	}
	System.out.println("Epoch #" + epoch + " Diff : "+ (err-previousVer));
	previousVer = err;
	
	} while ( epoch<MIN_TRAINING_EPOCHS||(epoch<NUM_TRAINING_EPOCHS && limit >0)) ;
	
	System.out.println("Trainingcompleted.");
	System.out.println("Testingnetwork...");
	
	//Generate test samples to build an output image
	int[] OutputRGBint = new int[NUMBATTLEFIELDSIZES*NUMCOOLINGRATES];
	Color MyColor;
	double MyValue = 0;
	double[][] MyTestData = new double[NUMBATTLEFIELDSIZES * NUMCOOLINGRATES][NUM_NN_INPUTS];

	for(int NdxBattleSize = 0; NdxBattleSize < NUMBATTLEFIELDSIZES; NdxBattleSize++){
		for(int NdxCooling = 0; NdxCooling < NUMCOOLINGRATES; NdxCooling++){
			MyTestData[NdxCooling + NdxBattleSize * NUMCOOLINGRATES][0] = 0.1+0.9*((double) NdxBattleSize)/NUMBATTLEFIELDSIZES;
			MyTestData[NdxCooling + NdxBattleSize * NUMCOOLINGRATES ][1]= 0.1+0.9*((double)NdxCooling)/NUMCOOLINGRATES;
		}
	}
	
	//Simulate the neural network with the test samples and fill a matrix
	for(int NdxBattleSize = 0; NdxBattleSize < NUMBATTLEFIELDSIZES; NdxBattleSize++){
		for(int NdxCooling = 0; NdxCooling < NUMCOOLINGRATES; NdxCooling++){
			double output[]=new double[1];
			nn.compute(MyTestData[NdxCooling + NdxBattleSize * NUMCOOLINGRATES], output);
			double MyResult = output[0];
			MyValue = ClipColor(MyResult);
			MyColor = new Color((float)MyValue, (float)MyValue, (float)MyValue);
			OutputRGBint[NdxCooling + NdxBattleSize * NUMCOOLINGRATES] = MyColor.getRGB();
		}
	}
	System.out.println("Testingcompleted.");
	
	//Plot the training samples7
	for(NdxSample = 0; NdxSample < NUMSAMPLES; NdxSample++){
		MyValue = ClipColor(FinalScore1[NdxSample]/250);
		MyColor = new Color((float)MyValue, (float)MyValue, (float) MyValue);
		int MyPixelIndex = (int)(Math.round(NUMCOOLINGRATES * ((GunCoolingRate[NdxSample]/MAXGUNCOOLINGRATE) - 0.1)/0.9) + Math.round(NUMBATTLEFIELDSIZES * ((BattlefieldSize[NdxSample]/MAXBATTLEFIELDSIZE) - 0.1)/0.9) * NUMCOOLINGRATES);
		if((MyPixelIndex>=0) && (MyPixelIndex < NUMCOOLINGRATES * NUMBATTLEFIELDSIZES)){
			OutputRGBint[MyPixelIndex] = MyColor.getRGB();
		}
	}
	
	BufferedImage img = new BufferedImage(NUMCOOLINGRATES, NUMBATTLEFIELDSIZES, BufferedImage.TYPE_INT_RGB);
	img.setRGB(0, 0, NUMCOOLINGRATES, NUMBATTLEFIELDSIZES, OutputRGBint, 0, NUMCOOLINGRATES);
	File f = new File("hello.png");
	
	try{
		ImageIO.write(img, "png", f);
	}
	catch (IOException e){
		e.printStackTrace();
	}
	
	System.out.println("Imagegenerated.");
	//Make sure that the Java VM is shut down properly
	System.exit(0);
}

	/*
	 *
	 * Clip a color value (double precision) to lie in the valid range [0,1]
	 */
	public static double ClipColor(double Value) {
		if (Value < 0.0) {
			Value = 0.0;
		}
		if (Value > 1.0) {
			Value = 1.0;
		}
		return Value;
	}
	
	//
	// Our private battle listener for handling the battle event we are interested in.
	//
	static class BattleObserver extends BattleAdaptor {
		//Called when the battle is completed successfully with battle results
		public void onBattleCompleted(BattleCompletedEvent e){
		//	System.out.println("-- Battle has completed --");
			//Get the indexed battle results
			BattleResults[] results = e.getIndexedResults();
			//Print out the indexed results with the robot names
		//	System.out.println("Battleresults:");
			
		//	for (BattleResults result : results){
		//		System.out.println(" " + result.getTeamLeaderName() + ": " + result.getScore());
		//	}
			
			//Store the scores of the robots
			BattlefieldParameterEvaluator.FinalScore1[NdxBattle] = results[0].getScore();
			BattlefieldParameterEvaluator.FinalScore2[NdxBattle] = results[1].getScore();
		}

		//Called when the game sends out an information message during the battle

		public void onBattleMessage(BattleMessageEvent e){
			//System.out.println("Msg>" + e.getMessage());
		}
		
		//Called when the game sends out an error message during the battle
		
		public void onBattleError(BattleErrorEvent e){
			System.out.println("Err>"+ e.getError());
		}
	}}
