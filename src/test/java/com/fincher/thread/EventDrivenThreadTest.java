package com.fincher.thread;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

/** Test the EventDrivenThread */
public class EventDrivenThreadTest implements DataHandlerIfc<String> {
	
	private List<String> queue = new ArrayList<String>();
	
	
	/** Method name is self explanatory 
	 * @throws InterruptedException
	 * */
	@Test
	public void testEventDrivenThread() throws InterruptedException {
		EventDrivenThread<String> edt = EventDrivenThread.getEventDrivenThread("Test", this);
		edt.start();
		
		for (int i = 0; i < 10; i++)
			edt.submit("Hello World " + i);
		
		Thread.sleep(100);
		
		int size = queue.size();
		if (size != 10) {
			Assert.fail("Queue size = " + size);
		}
	}
	
	@Override
	public void handleMessage(String message) {
		queue.add(message);
	}

}
