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

package com.voody.midilib.event.meta;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.voody.midilib.event.MidiEvent;
import com.voody.midilib.util.VariableLengthInt;

public class KeySignature extends MetaEvent {

	public static final int SCALE_MAJOR = 0;
	public static final int SCALE_MINOR = 1;
	
	private int mKey;
	private int mScale;
	
	public KeySignature(long tick, long delta, int key, int scale) {
		super(tick, delta, MetaEvent.KEY_SIGNATURE, new VariableLengthInt(2));
		
		mKey = key;
		mScale = scale;
	}
	
	public void setKey(int key) {
		mKey = key;
	}
	public int getKey() {
		return mKey;
	}
	
	public void setScale(int scale) {
		mScale = scale;
	}
	public int getScale() {
		return mScale;
	}

	@Override
	protected int getEventSize() {
		return 5;
	}
	
	@Override
	public void writeToFile(OutputStream out) throws IOException {
		super.writeToFile(out);
		
		out.write(2);
		out.write(mKey);
		out.write(mScale);
	}

	public static KeySignature parseKeySignature(long tick, long delta, InputStream in) throws IOException {
		
		in.read();		// Size = 2;
		int key = in.read();
		int scale = in.read();
		
		return new KeySignature(tick, delta, key, scale);
	}

	@Override
	public int compareTo(MidiEvent other) {
		
		if(mTick != other.getTick()) {
			return mTick < other.getTick() ? -1 : 1;
		}
		if(mDelta.getValue() != other.getDelta()) {
			return mDelta.getValue() < other.getDelta() ? 1 : -1;
		}
		
		if(!(other instanceof KeySignature)) {
			return 1;
		}
		
		KeySignature o = (KeySignature)other;
		if(mKey != o.mKey) {
			return mKey < o.mKey ? -1 : 1;
		}
		
		if(mScale != o.mScale) {
			return mKey < o.mScale ? -1 : 1;
		}
		
		return 0;
	}
}
