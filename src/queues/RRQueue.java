package queues;
import java.util.ArrayList;

import constants.SchedulingAlgorithm;
import datastructure.PseudoArray;
import gui.GanttChart;
import process.CPUBoundProcess;
import process.IOBoundProcess;
import scheduler.Scheduler;

public class RRQueue extends Queue{
		
	private PseudoArray array = new PseudoArray(1000);
	private CPUBoundProcess currProcess;
	private boolean running = false;
	private int numOfProcesses;
	private int quantum = 0;
	private long timeStart;
	private long timeEnd;
	private CPUBoundProcess prevProcess;
	private byte level = -1;
	
	private Object prevQueue;
	private Object nextQueue;

	public byte allProcessesDone = 1;
	public byte prevQueueDone = 1;

	public boolean executing = false;
	
	public static ArrayList<CPUBoundProcess> processList = new ArrayList<CPUBoundProcess>();
	
	public RRQueue(int level, int quantum){
		this.level = (byte)level;
		this.quantum = quantum;
	}	
	
	public void startThread(){
		running = true;
		RRThread.start();
		System.out.println("RRThread started!");
	}
	
	public void stopThread(){
		RRThread.interrupt();
		running = false;
	}
	
	public void enqueue(CPUBoundProcess newProcess){
		//System.out.println("level = " + level + " enter p" + newProcess.getId());
		
		array.add(newProcess);
		
		if(!processList.contains(newProcess)) {
			processList.add(newProcess);
		}
		
		allProcessesDone = 0;
		
		if(prevQueue != null) {
			int queueSize = 0;
	
			if(prevQueue instanceof RRQueue) {
				queueSize = ((RRQueue)(prevQueue)).getSize();		
			}else if(prevQueue instanceof FCFSQueue) {
				queueSize = ((FCFSQueue)(prevQueue)).getSize();
			}else if(prevQueue instanceof SJFQueue) {
				queueSize = ((SJFQueue)(prevQueue)).getSize();
			}else if(prevQueue instanceof SRTFQueue) {
				queueSize = ((SRTFQueue)(prevQueue)).getSize();
			}else if(prevQueue instanceof NonPQueue) {
				queueSize = ((NonPQueue)(prevQueue)).getSize();
			}else if(prevQueue instanceof PQueue) {
				queueSize = ((PQueue)(prevQueue)).getSize();
			}
			
			/*
			 * Stop execution of this queue if higher priority queue
			 * still have pending processes to execute.
			 * 
			 * */
			if(queueSize > 0) {				
				prevQueueDone = 0;
				return;
			}		
		}
		
		/*
		 * Conditional below determines if the new process is IO-bound and if
		 * the queue is executing a CPU-bound process when the new process arrived.
		 * If yes, we preempt it by displaying process in Gantt chart and shiftingIoBoundsToFront()
		 * 
		 * */
		if(newProcess instanceof IOBoundProcess) {
			if(currProcess != null  && currProcess instanceof CPUBoundProcess && currProcess.getPrevBurstPreempted()-currProcess.getBurstTime() > 0) {
				System.out.println("| !! p" + currProcess.getId() + " was executing when p" + newProcess.getId() + " burstLeft = " + currProcess.getBurstTime());
				
				long timeNow = Scheduler.clockTime;
				prevTimeQuantum = timeNow; 
									
				// TODO: Make sure to setEndTime the preempted process on all instances where you put preemption.
				
				currProcess.setPreempted();
				currProcess.setTimePreempted(timeNow);
				currProcess.setEndTime(timeNow);
				currProcess.preemptedFlag = true;
				prevProcess = currProcess;				
				
				int burstExecuted = (int) (currProcess.getEndTime()-currProcess.getStartTime());
				System.out.println("| burstExecuted = " + burstExecuted);			
				
				currProcess.setPrevBurstPreempted(currProcess.getBurstTime());			
				GanttChart.addExecutingProcess(level, currProcess.getId(), burstExecuted, (int)timeNow, SchedulingAlgorithm.RR);
				//currProcess = newProcess;
				//currProcess.setStartTime(timeNow);
				
				if(getSize() > 1) {
					shiftIoBoundsToFront();
				}
			}
		}
		
		startExecution();

		if(nextQueue != null) {
			if(nextQueue instanceof RRQueue) {
				((RRQueue)(nextQueue)).stopExecution();
			}else if(nextQueue instanceof SRTFQueue) {
				((SRTFQueue)(nextQueue)).stopExecution();
			}else if(nextQueue instanceof PQueue) {
				((PQueue)(nextQueue)).stopExecution();
			}else if(nextQueue instanceof FCFSQueue) {
				((FCFSQueue)(nextQueue)).stopExecution();
			}else if(nextQueue instanceof SJFQueue) {
				((SJFQueue)(nextQueue)).stopExecution();
			}else if(nextQueue instanceof NonPQueue) {
				((NonPQueue)(nextQueue)).stopExecution();
			}
		}
		
		running = true;
	}
	
	private void shiftIoBoundsToFront() {
		array.givePriorityToIoBounds();
	}

	public void reenqueue(CPUBoundProcess newProcess){		
		array.add(newProcess);		
		allProcessesDone = 0;		
	}	
	
	public CPUBoundProcess dequeue(){
					
		CPUBoundProcess prc = array.remove();											
		return prc;
	}
	
	public CPUBoundProcess peekHead(){
		if(array.getHead() == null) {
			return null;
		}else {
			return array.getHead().getValue();
		}
	}
	
	/*
	public CPUBoundProcess peekTail(){
		return array.get(getSize()-1).getValue(); 		
	}*/
	
	public int getSize(){
		return array.getSize();
	}
	
	public void startExecution() {
		if(prevQueue != null) {
			int size = 0;
			if(prevQueue instanceof RRQueue) {
				size = ((RRQueue)(prevQueue)).getSize();
			}else if(prevQueue instanceof FCFSQueue) {
				size = ((FCFSQueue)(prevQueue)).getSize();
			}else if(prevQueue instanceof SJFQueue) {
				size = ((SJFQueue)(prevQueue)).getSize();
			}else if(prevQueue instanceof SRTFQueue) {
				size = ((SRTFQueue)(prevQueue)).getSize();
			}else if(prevQueue instanceof NonPQueue) {
				size = ((NonPQueue)(prevQueue)).getSize();
			}else if(prevQueue instanceof PQueue) {
				size = ((PQueue)(prevQueue)).getSize();
			}
			
			if(size > 0) return;
		}

		if(getSize() > 0) {
			//System.out.println("| Level = " + level + " starting execution...");
			restart();
			prevQueueDone = 1;
		}
	}
	
	public void stopExecution() {
		System.out.println("	level = " + level + " stopping execution...");
		prevQueueDone = 0;
		
		if(nextQueue != null) {
			if(nextQueue instanceof RRQueue) {
				((RRQueue)(nextQueue)).stopExecution();
			}else if(nextQueue instanceof SRTFQueue) {
				((SRTFQueue)(nextQueue)).stopExecution();
			}else if(nextQueue instanceof PQueue) {
				((PQueue)(nextQueue)).stopExecution();
			}else if(nextQueue instanceof FCFSQueue) {
				((FCFSQueue)(nextQueue)).stopExecution();
			}else if(nextQueue instanceof SJFQueue) {
				((SJFQueue)(nextQueue)).stopExecution();
			}else if(nextQueue instanceof NonPQueue) {
				((NonPQueue)(nextQueue)).stopExecution();
			}			
		}
		
		/*
		 * Conditional below determines if this Queue is preempted
		 * by a higher priority queue.
		 * 
		 * It indicates that this queue was executing when
		 * a new process arrive at a higher queue, thus preempting the process.
		 * We update the prevQuantumTime to the time the process is preempted
		 * so the timer starts counting at that time.
		 * 
		 * */
		if(currProcess != null && currProcess.getPrevBurstPreempted()-currProcess.getBurstTime() > 0) {
			long timeNow = Scheduler.clockTime; 	
			prevTimeQuantum = timeNow;
			
			// Indicates that current processes is preempted.
			currProcess.setPreempted();
			currProcess.setTimePreempted(timeNow);
			currProcess.setEndTime(timeNow);
			currProcess.preemptedFlag = true;
			prevProcess = currProcess;
		
			int burstExecuted = (int) (currProcess.getEndTime()-currProcess.getStartTime());
			System.out.println("| burstExecuted = " + burstExecuted);
			
			currProcess.setPrevBurstPreempted(currProcess.getBurstTime());
			GanttChart.addExecutingProcess(level, currProcess.getId(), burstExecuted, (int)timeNow, SchedulingAlgorithm.RR);

		}
	}
	
	Thread RRThread = new Thread(){				
		public void run(){
			while(running){
				if(prevQueueDone == 1 && peekHead() != null){
					currProcess = peekHead();
					if(timeStart < 0){
						if(timeEnd != 0)						
							timeStart = timeEnd;
						else
							timeStart = Scheduler.clockTime;						
					}					
					
					if(currProcess.getResponseTime() < 0) {
						// TODO: Make sure to set prevProcess = something to all instances where you put preemption
						
						if(prevProcess != null && prevProcess.preemptedFlag) {
							long startTime = prevProcess.getTimePreempted(prevProcess.getTimesPreempted()-1);
							currProcess.setStartTime(startTime);
							currProcess.setFirstStartTime(startTime);
							currProcess.setResponseTime(startTime-currProcess.getArrivalTime());
						}else {
							long startTime = Scheduler.clockTime;
							currProcess.setStartTime(startTime);
							currProcess.setFirstStartTime(startTime);
							currProcess.setResponseTime(startTime-currProcess.getArrivalTime());
						}
					}
					
					if(currProcess.preemptedFlag) {
						//prevTimeQuantum = Scheduler.clockTime;
						long timeStart = Scheduler.clockTime;
						
						System.out.println("resuming p" + currProcess.getId() + "..... timeStart = " + timeStart);
						
						currProcess.setStartTime(timeStart);
						currProcess.setTimeResumed(timeStart);						
						currProcess.preemptedFlag = false;
						
						//currProcess.tempStartTime = (int)Scheduler.clockTime;
						//System.out.println("**** p" + currProcess.getId() + " tempStartTime = " + currProcess.getId());
					}
					
					long timeNow = Scheduler.clockTime;	
					if(prevTime < timeNow){				
						System.out.println("| Level = " + level + " executing p" + currProcess.getId() + " startTime = " + currProcess.getStartTime() + " timeNow = " + timeNow);
						
						int lapse = (int)(timeNow - prevTime);
						int burstLeft = currProcess.getBurstTime() - lapse;					
						currProcess.setBurstTime(burstLeft);																	
											
						/*
						 * Conditional immediately below relinquishes IOBound process
						 * for IO operation.
						 * 
						 * */
						if(currProcess instanceof IOBoundProcess) {							
							if(timeNow == prevTimeQuantum + (quantum-1)){
								System.out.println("Just before quantum expires....");
								prevProcess = currProcess;							
								GanttChart.addExecutingProcess(level, currProcess.getId(), quantum-1, (int)timeNow, SchedulingAlgorithm.RR);
								currProcess.setArrivalTime(timeNow + ((IOBoundProcess)(currProcess)).getIoSpeed());
								
								if(burstLeft > 0){						
									insertToReadyQueue((IOBoundProcess)currProcess);
									dequeue();
								}
							
								prevTimeQuantum = timeNow;
							}							
						}else {
							System.out.println("==p" + currProcess.getId() + " not IOBound.");
						}
						
						
						if(timeNow == prevTimeQuantum + quantum){
							System.out.println("}}} prevTimeQuantum = " + prevTimeQuantum);
							System.out.println("| -- Quantum time is done timeNow = " + timeNow);
							
							GanttChart.addExecutingProcess(level, currProcess.getId(), quantum, (int)timeNow, SchedulingAlgorithm.RR);
							
							if(burstLeft > 0){													
								currProcess.setPreempted();
								currProcess.setTimePreempted(timeNow);
								currProcess.preemptedFlag = true;

								prevProcess = currProcess;
								
								int burstPreempted = currProcess.getBurstTime();
								currProcess.setPrevBurstPreempted(burstPreempted);
								if(nextQueue == null) {
									retain();
								} else {
									demote(currProcess);
									dequeue();
								}
							}
							
							prevTimeQuantum = timeNow;
						}
						
						if(burstLeft <= 0){		
							currProcess.setWaitTimePreemptive();
							int s = currProcess.getTimesPreempted();
										
							if(currProcess.getPrevBurstPreempted() < quantum){						
								GanttChart.addExecutingProcess(level, currProcess.getId(), currProcess.getPrevBurstPreempted(), (int)timeNow, SchedulingAlgorithm.RR);								
							}
							
							dequeue();													
							System.out.println("p" + currProcess.getId() + " Done executing.");
							
							//currProcess.setTimeResumed(timeNow);
							currProcess.preemptedFlag = false;
							prevProcess = currProcess;
							
							timeEnd = Scheduler.clockTime;
							prevTimeQuantum = timeNow;
							timeStart = -1;							
						}
																			
					}					
					prevTime = timeNow;					
					
				}else{										
				
//					/System.out.println("======================");
					/*if(getSize() < 0) {
						System.out.println("size was zero");
					}else if (prevQueueDone != 1) {
						System.out.println("prevQueeue not done");
					}else if(peekHead() == null) {
						System.out.println("peekhead is null");
					}*/
					
					if (allProcessesDone == 0 && getSize() == 0){
						//GanttChart.addLastCompletionTime(level, SchedulingAlgorithm.RR);		
						allProcessesDone = 1;
						if(nextQueue != null) {
							if(nextQueue instanceof RRQueue) {
								((RRQueue)(nextQueue)).startExecution();
							}else if (nextQueue instanceof SRTFQueue) {
								((SRTFQueue)(nextQueue)).startExecution();								
							}else if (nextQueue instanceof FCFSQueue) {
								((FCFSQueue)(nextQueue)).startExecution();								
							}else if (nextQueue instanceof PQueue) {
								((PQueue)(nextQueue)).startExecution();								
							}else if (nextQueue instanceof SJFQueue) {
								((SJFQueue)(nextQueue)).startExecution();								
							}else if (nextQueue instanceof NonPQueue) {
								((NonPQueue)(nextQueue)).startExecution();								
							}
						}
						
						if(level == Scheduler.getMaxLevelOfQueues() && Scheduler.processes.size() == 0) {
							System.out.println("Allprocessdon size zero.. stopping simulation...");
							simulationDone();
						}
					}			
					
					/*
					if(numOfProcesses <= 0){						
						if(nextQueue != null) {
							allProcessesDone = 1;
							if(nextQueue instanceof RRQueue) {
								((RRQueue)(nextQueue)).startExecution();
							}else if (nextQueue instanceof SRTFQueue) {
								((SRTFQueue)(nextQueue)).startExecution();								
							}
						}
						
						int s = Scheduler.processes.length;
						Process[] p = Scheduler.processes;
						
						double totalRT = 0;
						double totalWT = 0;
						double totalTT = 0;
						
						for(int i = 0; i < s; i++) {
							GanttChart.addTimesInformation(p[i].getId(), p[i].getResponseTime(), p[i].getWaitTime(), p[i].getTurnaroundTime());
							totalRT += p[i].getResponseTime();
							totalWT += p[i].getWaitTime();
					
							totalTT += p[i].getTurnaroundTime();
						}						
						GanttChart.addTimeAverages(totalRT/s, totalWT/s, totalTT/s);
					}*/
				}
			}
		}
	};
	
	public void simulationDone(){
		// TODO: Print each processes' wait, response, and turnaround time;
		ArrayList<CPUBoundProcess> temp = processList;
		int count =  temp.size();
		
		double avgResponse = 0;
		double avgWait = 0;
		double avgTurnaround = 0;
		
		for(int i = 0; i < count; i++) {
			temp.get(i).setWaitTimePreemptive();
			
			System.out.print("[p" + temp.get(i).getId() + "]: ");
			System.out.println("timesPreempted = " + temp.get(i).timePreempted.size() + " timesResumed = " + temp.get(i).timeResumed.size() 
					+ " waitTime: " + temp.get(i).getWaitTime() + " responseTime: " + temp.get(i).getResponseTime() + " turnAround: " + temp.get(i).getTurnaroundTime());
			
			avgResponse += temp.get(i).getResponseTime();
			avgWait += temp.get(i).getWaitTime();
			avgTurnaround += temp.get(i).getTurnaroundTime();
			
			//int c = temp.get(i).getTimesPreempted();			
			/*for(int j = 0; j < c; j++) {
				System.out.print(temp.get(i).timePreempted.get(j) + " -> ");
				System.out.print(temp.get(i).timeResumed.get(j) + " | ");
			}*/
		}
		
		avgResponse = avgResponse/count;
		avgWait = avgWait/count;
		avgTurnaround = avgTurnaround/count;
		
		System.out.println("avgResponse = " + avgResponse + " avgWait = " + avgWait + " avgTurnaround = " + avgTurnaround);
		
		GanttChart.simulationDone(this);
	}
	
	protected void insertToReadyQueue(IOBoundProcess process) {
		System.out.println("Insert p" + process.getId() + " with new arrival time = " + process.getArrivalTime());
		Scheduler.enqueue(process);
	}

	protected void retain() {
		enqueue(dequeue());
	}

	protected void demote(CPUBoundProcess process) {
		
		if(nextQueue == null) return;
		
		System.out.println("level = " + level + " demote p" + process.getId() + " burstLeft = " + process.getBurstTime() + " size = " + getSize() + " np = " + numOfProcesses);
		
		if(nextQueue instanceof FCFSQueue) {
			((FCFSQueue)nextQueue).enqueue(process);
		}else if(nextQueue instanceof SJFQueue){
			((SJFQueue)nextQueue).enqueue(process);
		}else if(nextQueue instanceof SRTFQueue) {
			((SRTFQueue)nextQueue).enqueue(process);
		}else if(nextQueue instanceof NonPQueue) {
			((NonPQueue)nextQueue).enqueue(process);
		}else if(nextQueue instanceof PQueue) {
			((PQueue)nextQueue).enqueue(process);
		}else if(nextQueue instanceof RRQueue) {
			((RRQueue)nextQueue).enqueue(process);
		}
	}
	
	public void setNumberOFProcesses(int length) {
		this.numOfProcesses = length;
	}
	
	public void restart() {
		running = true;
	}
	
	public void setPrevQueue(Object prevQueue) {
		this.prevQueue = prevQueue;
		if(prevQueue == null) {
			prevQueueDone = 1;
		}
		
	}
	
	public void setNextQueue(Object nextQueue){
		this.nextQueue = nextQueue;
	}
	
	public Object getNextQueue(){
		return nextQueue;
	}
	
	public Object getPrevQueue() {
		return prevQueue;
	}
	
	public long getQuantum(){
		return quantum;
	}
	
}
