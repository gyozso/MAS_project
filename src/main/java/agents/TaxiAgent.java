package agents;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import ants.ClientPath;
import ants.ExplorationAnt;


import messages.BidMessage;
import messages.BroadcastMessage;
import messages.ConfirmationMessage;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.lab.common.trucks.Truck;

public class TaxiAgent extends Agent implements TickListener {

	private Truck truck;
	private boolean foundAgent;
    private boolean hasAgent;
    private boolean shouldPickup;
    private boolean shouldDeliver;
    private String packageId;
    private Point destination;
    private Agency agency;
	
	public TaxiAgent(Truck truck, Agency agency, double radius, double reliability){
		super(radius,reliability);
		this.truck = truck;
		this.hasAgent = false;
		this.foundAgent = false;
		this.shouldDeliver = false;
		this.shouldPickup = false;
		this.agency = agency;
	}
	
	@Override
	public void tick(long currentTime, long timeStep) {
		if(!hasAgent){
			foundAgent = false;
			Queue<Message> messages = mailbox.getMessages();
			ClientPath closestClient = null;
			for(Message message : messages){
				if(!hasAgent && message instanceof ConfirmationMessage){
					ConfirmationMessage cm = (ConfirmationMessage) message;
					ClientAgent client = cm.getClosestClient().getClient();
					this.packageId = client.getClient().getPackageID();
					this.destination = client.getClient().getDeliveryLocation();
					this.path = cm.getClosestClient().getPath();
					this.hasAgent = true;
					System.out.println("[" + truck.getTruckID() + "] I made a contract with: " + this.packageId);
					this.shouldDeliver = true;
					this.shouldPickup = true;
				}
				else if(!hasAgent && message instanceof BroadcastMessage){
					BroadcastMessage bm = (BroadcastMessage) message;
					ExplorationAnt eAnt= new ExplorationAnt(this, getPosition(), bm.getClients());
					eAnt.initRoadUser(truck.getRoadModel());
					closestClient = eAnt.lookForClient(getPosition());
					if(closestClient != null){
						foundAgent = true;
					}
				}
			}
			if(!hasAgent && foundAgent){
				System.out.println("[" + truck.getTruckID() + "] I found a client.");
				System.out.println(closestClient.getPath().size());
				System.out.println(closestClient.getTravelTime());
				agency.receive(new BidMessage(this,closestClient));
			}
		}
		if(path == null || path.isEmpty()){
			if(truck.tryPickup()){
				this.shouldPickup = false;
				System.out.println("[" + truck.getTruckID() + "] I picked up " + this.packageId);
				this.path = new LinkedList<Point>(truck.getRoadModel().getShortestPathTo(truck, destination));
			}
			if(truck.tryDelivery()){
				this.shouldDeliver = false;
				System.out.println("[" + truck.getTruckID() + "] I delivered " + this.packageId);
				hasAgent = false;
				foundAgent = false;
			}
			if(!shouldPickup && !shouldDeliver){
				destination = truck.getRoadModel().getGraph().getRandomNode(simulator.getRandomGenerator());
				this.path = new LinkedList<Point>(truck.getRoadModel().getShortestPathTo(truck, destination));
			}
		}else if(hasAgent){
			truck.drive(path, timeStep);
		}
	}
	
	@Override
	public Point getPosition() {
		return this.truck.getPosition();
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void receive(Message message) {
		if(!hasAgent){
			super.receive(message);
		}
	}
	
	public Truck getTruck() {
		return truck;
	}

}
