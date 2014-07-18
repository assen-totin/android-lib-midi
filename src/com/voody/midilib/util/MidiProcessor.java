//////////////////////////////////////////////////////////////////////////////
//	Copyright 2011 Alex Leffelman
//	
//	Licensed under the Apache License, Version 2.0 (the "License");
//	you may not use this file except in compliance with the License.
//	You may obtain a copy of the License at
//	
//	http://www.apache.org/licenses/LICENSE-2.0
//	
//	Unless required by applicable law or agreed to in writing, software
//	distributed under the License is distributed on an "AS IS" BASIS,
//	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//	See the License for the specific language governing permissions and
//	limitations under the License.
//////////////////////////////////////////////////////////////////////////////

package com.voody.midilib.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.voody.midilib.MidiFile;
import com.voody.midilib.MidiTrack;
import com.voody.midilib.event.MidiEvent;
import com.voody.midilib.event.meta.Tempo;
import com.voody.midilib.event.meta.TimeSignature;

public class MidiProcessor {

	private static final int PROCESS_RATE_MS = 8;
	
	private HashMap<Class<? extends MidiEvent>, ArrayList<MidiEventListener>> mEventListenerMap;
	private ArrayList<MidiEventListener> mListenersToAll;
	
	private MidiFile mMidiFile;
	private boolean mRunning;
	private double mTicksElapsed;
	private long mMsElapsed;
	
	private int mMPQN;
	private int mPPQ;
	
	private double mMetronomeProgress;
	private int mMetronomeFrequency;
	
	public MidiProcessor(MidiFile input) {
		mMidiFile = input;
		
		mRunning = false;
		mTicksElapsed = 0;
		mMsElapsed = 0;
		
		mMPQN = Tempo.DEFAULT_MPQN;
		mPPQ = mMidiFile.getResolution();
		
		mMetronomeProgress = 0;
		setMetronomeFrequency(TimeSignature.DEFAULT_METER);
		
		mEventListenerMap = new HashMap<Class<? extends MidiEvent>, ArrayList<MidiEventListener>>();
		mListenersToAll = new ArrayList<MidiEventListener>();
	}
	
	public synchronized void start() {
		if(mRunning) return;
		
		mRunning = true;
		new Thread(new Runnable() {
			public void run() {
				process();
			}
		}).start();
	}
	
	public void stop() {
		mRunning = false;
	}
	
	public void reset() {
		mRunning = false;
		mTicksElapsed = 0;
	}
	
	public boolean isStarted() {
		return mTicksElapsed > 0;
	}
	public boolean isRunning() {
		return mRunning;
	}
	
	protected void onStart(boolean fromBeginning) {
		Iterator<Class<? extends MidiEvent>> it = mEventListenerMap.keySet().iterator();
		
		while(it.hasNext()) {
			ArrayList<MidiEventListener> listeners = mEventListenerMap.get(it.next());
			
			if(listeners == null) {
				continue;
			}
			for(MidiEventListener mel : listeners) {
				mel.onStart(fromBeginning);
			}
		}
		
		for(MidiEventListener mel : mListenersToAll) {
			mel.onStart(fromBeginning);
		}
	}
	
	protected void onStop(boolean finished) {
		
		Iterator<Class<? extends MidiEvent>> it = mEventListenerMap.keySet().iterator();
		
		while(it.hasNext()) {
			ArrayList<MidiEventListener> listeners = mEventListenerMap.get(it.next());
			
			if(listeners == null) {
				continue;
			}
			for(MidiEventListener mel : listeners) {
				mel.onStop(finished);
			}
		}
		
		for(MidiEventListener mel : mListenersToAll) {
			mel.onStart(finished);
		}
	}
	
	public void registerListenerForAllEvents(MidiEventListener mel) {
		mListenersToAll.add(mel);
	}
	public void unregisterListenerForAllEvents(MidiEventListener mel) {
		mListenersToAll.remove(mel);
	}
	
	public void registerEventListener(MidiEventListener mel, Class<? extends MidiEvent> event) {
		
		ArrayList<MidiEventListener> listeners = mEventListenerMap.get(event);
		if(listeners == null) {
			
			listeners = new ArrayList<MidiEventListener>();
			listeners.add(mel);
			mEventListenerMap.put(event, listeners);
		}
		else {
			listeners.add(mel);
		}
	}
	
	public void unregisterEventListener(MidiEventListener mel, Class<? extends MidiEvent> event) {
		
		ArrayList<MidiEventListener> listeners = mEventListenerMap.get(event);
		if(listeners != null) {
			listeners.remove(mel);
		}
	}
	
	public void unregisterAllEventListeners() {
		mEventListenerMap.clear();
		mListenersToAll.clear();
	}
	
	protected void dispatch(MidiEvent event) {
		
		// Tempo and Time Signature events are always needed by the processor
		if(event.getClass().equals(Tempo.class)) {
			mMPQN = ((Tempo)event).getMpqn();
		}
		else if(event.getClass().equals(TimeSignature.class)) {
			setMetronomeFrequency(((TimeSignature)event).getMeter());
		}

		for(MidiEventListener mel : mListenersToAll) {
			mel.onEvent(event, mMsElapsed);
		}
		
		// Retrieve all listeners associated with this event and send them the event
		ArrayList<MidiEventListener> listeners = mEventListenerMap.get(event.getClass());
		
		if(listeners == null) {
			return;
		}
		
		for(MidiEventListener mel : listeners) {
			mel.onEvent(event, mMsElapsed);
		}
	}
	
	private void setMetronomeFrequency(int meter) {
		
		switch(meter) {
		case TimeSignature.METER_EIGHTH:
			mMetronomeFrequency = mPPQ / 2;
			break;
		case TimeSignature.METER_QUARTER:
			mMetronomeFrequency = mPPQ;
			break;
		case TimeSignature.METER_HALF:
			mMetronomeFrequency = mPPQ * 2;
			break;
		case TimeSignature.METER_WHOLE:
			mMetronomeFrequency = mPPQ * 4;
			break;
		}
	}
	
	private void process() {
		
		onStart(mTicksElapsed < 1);
		
		ArrayList<MidiTrack> tracks = mMidiFile.getTracks();
		ArrayList<Iterator<MidiEvent>> iterators = new ArrayList<Iterator<MidiEvent>>();
		MidiEvent[] currEvents = new MidiEvent[tracks.size()];
		
		for(int i = 0; i < tracks.size(); i++) {
			iterators.add(tracks.get(i).getEvents().iterator());
			if(iterators.get(i).hasNext()) {
				currEvents[i] = iterators.get(i).next();
			}
		}
		
		long lastMs = System.currentTimeMillis();
		
		boolean finished = false;
		
		top:
		while(mRunning) {
			long now = System.currentTimeMillis();
			long msElapsed = now - lastMs;
			if(msElapsed < PROCESS_RATE_MS) {
				continue;
			}
			
			double ticksElapsed = MidiUtil.msToTicks(msElapsed, mMPQN, mPPQ);
			
			if(ticksElapsed < 1) {
				continue;
			}
			
			mMetronomeProgress += ticksElapsed;
			if(mMetronomeProgress >= mMetronomeFrequency) {
				mMetronomeProgress %= mMetronomeFrequency;
				dispatch(MetronomeTick.getInstance());
			}
			
			lastMs = now;
			mMsElapsed += msElapsed;
			mTicksElapsed += ticksElapsed;

			for(int i = 0; i < tracks.size(); i++) {
				
				while(currEvents[i] != null && currEvents[i].getTick() <= mTicksElapsed) {

					dispatch(currEvents[i]);
					
					if(iterators.get(i).hasNext()) {
						currEvents[i] = iterators.get(i).next();
					} else {
						currEvents[i] = null;
						break;
					}
				}
			}
			
			for(int i = 0; i < tracks.size(); i++) {
				if(iterators.get(i).hasNext()) {
					continue top;
				}
			}

			finished = true;
			break;
		}
		
		mRunning = false;
		onStop(finished);
	}
	
	/**
	 * An event specifically for the processor to broadcast metronome ticks
	 * so that observers need not rely on time conversions.
	 */
	public static class MetronomeTick extends MidiEvent {

		private static MetronomeTick instance = new MetronomeTick();
		public static MetronomeTick getInstance() {
			return instance;
		}
		private MetronomeTick() {
			super(0, 0);
		}

		@Override
		public int compareTo(MidiEvent o) {
			return 0;
		}

		@Override
		protected int getEventSize() {
			return 0;
		}
		
		@Override
		public int getSize() {
			return 0;
		}
	}
}
