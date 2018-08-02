package queues;
import constants.QueueType;

public class SJFQueue extends Queue{
	
	public SJFQueue(int level){
		super(level);
		this.queueType = QueueType.SJF;
	}
					
	public void run(){
		queueStartTime = clockTime;	
		
		while(clockTime != -1 && getNextArrivalTime() == clockTime) {
			getNextProcess();
		}
		
		for(int ctr = 0; ctr < totalBurstTime; ctr++){
			timeNow = queueStartTime + ctr;
			
			if((currProcess = peekHead()) != null){	
				if(prevQueue != null && prevQueue instanceof RoundRobin) {
					currProcess.setStartTime(prevTimeQuantum);
					if(currProcess.preemptedFlag) {
						currProcess.setTimeResumed(prevTimeQuantum);
						currProcess.preemptedFlag = false;
					}
				}else {
					if(currProcess.getResponseTime() < 0) {
						if(currProcess.getArrivalTime() <= prevTimeQuantum) {
							currProcess.setStartTime(prevTimeQuantum);
							currProcess.setFirstStartTime(prevTimeQuantum);
						}else {
							currProcess.setStartTime(queueStartTime + ctr);
							currProcess.setFirstStartTime(queueStartTime + ctr);
						}					
						currProcess.setResponseTime();
					}
					if(currProcess.preemptedFlag) {						
						if(currProcess.getArrivalTime() <= prevTimeQuantum) {
							currProcess.setStartTime(prevTimeQuantum);
							currProcess.setTimeResumed(prevTimeQuantum);
						}else {
							currProcess.setStartTime(queueStartTime + ctr);
							currProcess.setTimeResumed(queueStartTime + ctr);
						}
						currProcess.preemptedFlag = false;
					}
				}
				prevProcess = currProcess;
				
				int burstLeft = currProcess.getBurstTime() - 1;
				currProcess.setBurstTime(burstLeft);
				
				System.out.println("[SJF:] Level = " + 
						level + 
						" executing P" + 
						currProcess.getId() + 
						" startTime = " + 
						currProcess.getStartTime() +
						" burstLeft = " +
						burstLeft +
						" timeNow = " + timeNow);
							
				if(burstLeft <= 0){		
					currProcess.setEndTime(timeNow);
					dequeue();									
					System.out.println("[SJF:] p" + currProcess.getId() + " Done executing. prevBurstPreempted = " + currProcess.getPrevBurstPreempted());
					currProcess.preemptedFlag = false;
					prevProcess = currProcess;					
					prevTimeQuantum = timeNow;
				}
				while(clockTime != -1 && getNextArrivalTime() == clockTime) {
					getNextProcess();
				}				
				clockTime++;
			}
			stopThread();
		}
	}
}
