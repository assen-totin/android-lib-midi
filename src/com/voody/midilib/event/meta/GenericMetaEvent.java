package com.voody.midilib.event.meta;

import java.io.IOException;
import java.io.OutputStream;

import com.voody.midilib.event.MidiEvent;
import com.voody.midilib.util.VariableLengthInt;

public class GenericMetaEvent extends MetaEvent {

	private byte[] mData;
	
	protected GenericMetaEvent(long tick, long delta, int type,	VariableLengthInt length, byte[] data) {
		super(tick, delta, type, length);
		
		mData = data;
	}

	@Override
	protected int getEventSize() {
		return 1 + 1 + mLength.getByteCount() + mLength.getValue();
	}
	
	@Override
	protected void writeToFile(OutputStream out) throws IOException {
		super.writeToFile(out);
		out.write(mLength.getBytes());
		out.write(mData);
	}

	@Override
	public int compareTo(MidiEvent other) {
		
		if(mTick != other.getTick()) {
			return mTick < other.getTick() ? -1 : 1;
		}
		if(mDelta.getValue() != other.getDelta()) {
			return mDelta.getValue() < other.getDelta() ? 1 : -1;
		}
		
		return 1;
	}

	
}
