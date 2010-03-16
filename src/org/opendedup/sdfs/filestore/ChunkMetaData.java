package org.opendedup.sdfs.filestore;

import java.nio.ByteBuffer;

/**
 * 
 * @author sam silverberg Chunk block meta data is as follows: [mark of deletion
 *         (1 byte)|hash lenth(2 bytes)|hash(32 bytes)|date added (8 bytes)|date
 *         last claimed (8 bytes)| number of times claimed (8 bytes)|chunk len
 *         (4 bytes)|chunk position (8 bytes)]
 * 
 */
public class ChunkMetaData {
	public static final int RAWDL = 1 + 2 + 32 + 8 + 8 + 8 + 4 + 8;
	public static final int CLAIMED_OFFSET = 1 + 2 + 32 + 8;
	private boolean mDelete = false;
	private short hashLen = 0;
	private byte[] hash = null;
	private long added = 0;
	private long lastClaimed = 0;
	private long numClaimed = 0;
	private int cLen = 0;
	private long cPos = 0;

	public ChunkMetaData(long cPos) {
		this.cPos = cPos;
		this.mDelete = true;
	}

	public ChunkMetaData(byte[] rawData) {
		ByteBuffer buf = ByteBuffer.wrap(rawData);
		byte del = buf.get();
		if (del == 0)
			this.mDelete = false;
		else
			this.mDelete = true;
		this.hashLen = buf.getShort();
		this.hash = new byte[hashLen];
		buf.get(hash);
		buf.get(new byte[32 - hashLen]);
		added = buf.getLong();
		lastClaimed = buf.getLong();
		this.numClaimed = buf.getLong();
		cLen = buf.getInt();
		cPos = buf.getLong();
	}

	public ChunkMetaData(byte[] hash, int chunkLen, long chunkPos) {
		long tm = System.currentTimeMillis();
		this.added = tm;
		this.lastClaimed = tm;
		this.numClaimed = 1;
		this.mDelete = false;
		this.hashLen = (short) hash.length;
		this.hash = hash;
		this.cLen = chunkLen;
		this.cPos = chunkPos;
	}

	public ByteBuffer getBytes() {
		ByteBuffer buf = ByteBuffer.allocateDirect(RAWDL);
		if (this.mDelete) {
			buf.put(new byte[RAWDL]);
			buf.position(0);
			return buf;
		} else {
			buf.put((byte) 0);
			buf.putShort(this.hashLen);
			buf.put(hash);
			buf.put(new byte[32 - this.hashLen]);
			buf.putLong(this.added);
			buf.putLong(this.lastClaimed);
			buf.putLong(this.numClaimed);
			buf.putInt(this.cLen);
			buf.putLong(cPos);
			buf.position(0);
			return buf;
		}
	}

	public boolean ismDelete() {
		return mDelete;
	}

	public void setmDelete(boolean mDelete) {
		this.mDelete = mDelete;
	}

	public long getLastClaimed() {
		return lastClaimed;
	}

	public void setLastClaimed(long lastClaimed) {
		this.lastClaimed = lastClaimed;
	}

	public long getNumClaimed() {
		return this.numClaimed;
	}

	public void updateNumClaimed(int num) {
		this.numClaimed += num;
	}

	public long getcPos() {
		return cPos;
	}

	public void setcPos(long cPos) {
		this.cPos = cPos;
	}

	public short getHashLen() {
		return hashLen;
	}

	public byte[] getHash() {
		return hash;
	}

	public long getAdded() {
		return added;
	}

	public int getcLen() {
		return cLen;
	}
}
