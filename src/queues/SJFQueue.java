package queues;
import constants.QueueType;
import process.IOBoundProcess;
import scheduler.Scheduler;

public class SJFQueue extends Queue{
	
	public SJFQueue(int level){
		super(level);
		this.queueType = QueueType.SJF;
	}
					
	public void run(){
		queueStartTime = clockTime;
		
		System.out.println("[SJF:] Inside run method");
		System.out.println("[SJF:] queueStartTime: " + queueStartTime);
		
		while(getNextArrivalTime() == clockTime) {
			getNextProcess();
		}
		
		for(int ctr = 0; ctr < totalBurstTime; ctr++){		
			if((currProcess = peekHead()) != null){										
				if(currProcess.getResponseTime() < 0) {
					currProcess.setStartTime(queueStartTime + ctr);
					currProcess.setFirstStartTime(queueStartTime + ctr);
					currProcess.setResponseTime();	
				}
				if(currProcess.preemptedFlag) {						
					currProcess.setStartTime(queueStartTime);
					currProcess.setTimeResumed(queueStartTime);						
					currProcess.preemptedFlag = false;
				}				
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
						" timeNow = " + (queueStartTime + ctr));
				timeNow = queueStartTime + ctr;				
				if(burstLeft <= 0){								
					dequeue();									
					System.out.println("[SJF:] p" + currProcess.getId() + " Done executing. prevBurstPreempted = " + currProcess.getPrevBurstPreempted());
					currProcess.preemptedFlag = false;
					prevProcess = currProcess;					
					prevTimeQuantum = timeNow;
				}
				while(getNextArrivalTime() == clockTime) {
					getNextProcess();
				}
				clockTime++;			
			}
			stopThread();
		}
	}
}
