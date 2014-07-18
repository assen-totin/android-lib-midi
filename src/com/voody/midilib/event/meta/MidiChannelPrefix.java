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

public class MidiChannelPrefix extends MetaEvent {

	private int mChannel;
	
	public MidiChannelPrefix(long tick, long delta, int channel) {
		super(tick, delta, MetaEvent.MIDI_CHANNEL_PREFIX, new VariableLengthInt(4));
		
		mChannel = channel;
	}
	
	public void setChannel(int c) {
		mChannel = c;
	}
	public int getChannel() {
		return mChannel;
	}

	@Override
	protected int getEventSize() {
		return 4;
	}

	@Override
	public void writeToFile(OutputStream out) throws IOException {
		super.writeToFile(out);
		
		out.write(1);
		out.write(mChannel);
	}

	public static MidiChannelPrefix parseMidiChannelPrefix(long tick, long delta, InputStream in) throws IOException {
		
		in.read();		// Size = 1;
		int channel = in.read();
		
		return new MidiChannelPrefix(tick, delta, channel);
	}

	@Override
	public int compareTo(MidiEvent other) {
		
		if(mTick != other.getTick()) {
			return mTick < other.getTick() ? -1 : 1;
		}
		if(mDelta.getValue() != other.getDelta()) {
			return mDelta.getValue() < other.getDelta() ? 1 : -1;
		}
		
		if(!(other instanceof MidiChannelPrefix)) {
			return 1;
		}
		
		MidiChannelPrefix o = (MidiChannelPrefix)other;

		if(mChannel != o.mChannel) {
			return mChannel < o.mChannel ? -1 : 1;
		}
		return 0;
	}
}
