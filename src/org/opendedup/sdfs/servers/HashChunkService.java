package org.opendedup.sdfs.servers;

import java.io.IOException;
import java.util.ArrayList;

import org.opendedup.collections.AbstractHashesMap;
import org.opendedup.collections.DataArchivedException;
import org.opendedup.collections.HashtableFullException;
import org.opendedup.collections.InsertRecord;
import org.opendedup.hashing.LargeFileBloomFilter;
import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.filestore.AbstractChunkStore;
import org.opendedup.sdfs.filestore.ConsistancyCheck;
import org.opendedup.sdfs.filestore.DSECompaction;
import org.opendedup.sdfs.filestore.FileChunkStore;
import org.opendedup.sdfs.filestore.HashChunk;
import org.opendedup.sdfs.filestore.HashStore;
import org.opendedup.sdfs.filestore.cloud.AbstractCloudFileSync;
import org.opendedup.sdfs.filestore.cloud.RemoteVolumeInfo;
import org.opendedup.sdfs.network.HashClient;
import org.opendedup.sdfs.notification.SDFSEvent;

public class HashChunkService implements HashChunkServiceInterface {

	private double kBytesRead;
	private double kBytesWrite;
	private final long KBYTE = 1024L;
	private long chunksRead;
	private long chunksWritten;
	private long chunksFetched;
	private double kBytesFetched;
	private int unComittedChunks;
	private int MAX_UNCOMITTEDCHUNKS = 100;
	private HashStore hs = null;
	private AbstractChunkStore fileStore = null;

	// private HashClientPool hcPool = null;

	/**
	 * @return the chunksFetched
	 */
	public long getChunksFetched() {
		return chunksFetched;

	}

	public HashChunkService() {
		try {
			fileStore = (AbstractChunkStore) Class
					.forName(Main.chunkStoreClass).newInstance();
			fileStore.init(Main.chunkStoreConfig);
		} catch (Throwable e) {
			SDFSLogger.getLog().fatal("Unable to initiate ChunkStore", e);
			System.err.println("Unable to initiate ChunkStore");
			e.printStackTrace();
			System.exit(-1);
		}
		try {
			hs = new HashStore(this);
		} catch (Exception e) {
			SDFSLogger.getLog().fatal("unable to start hashstore", e);
			System.err.println("Unable to initiate hashstore");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private long dupsFound;

	public AbstractChunkStore getChuckStore() {
		return fileStore;
	}

	public AbstractHashesMap getHashesMap() {
		return hs.bdb;
	}

	public InsertRecord writeChunk(byte[] hash, byte[] aContents,
			boolean compressed) throws IOException, HashtableFullException {
		if (aContents.length > Main.chunkStorePageSize)
			throw new IOException("content size out of bounds ["
					+ aContents.length + "] > [" + Main.chunkStorePageSize
					+ "]");
		chunksRead++;
		InsertRecord written = hs.addHashChunk(new HashChunk(hash, aContents,
				compressed));
		if (written.getInserted()) {
			unComittedChunks++;
			chunksWritten++;
			kBytesWrite = kBytesWrite + (aContents.length / KBYTE);
			if (unComittedChunks > MAX_UNCOMITTEDCHUNKS) {
				commitChunks();
			}
		} else {
			dupsFound++;
		}
		return written;
	}

	public void setReadSpeed(int speed) {
		fileStore.setReadSpeed((int) speed);
	}

	public void setWriteSpeed(int speed) {
		fileStore.setWriteSpeed((int) speed);
	}

	public void setCacheSize(long sz) throws IOException {
		fileStore.setCacheSize(sz);
	}

	public void remoteFetchChunks(ArrayList<String> al, String server,
			String password, int port, boolean useSSL) throws IOException,
			HashtableFullException {
		HCServer hserver = new HCServer(server, port, false, false, useSSL);
		HashClient hc = new HashClient(hserver, "replication", password,
				(byte) 0);
		try {
			ArrayList<HashChunk> hck = hc.fetchChunks(al);
			for (int i = 0; i < hck.size(); i++) {
				HashChunk _hc = hck.get(i);
				writeChunk(_hc.getName(), _hc.getData(), false);
			}
		} finally {
			hc.close();
		}
	}

	public long hashExists(byte[] hash) throws IOException,
			HashtableFullException {
		return hs.hashExists(hash);
	}

	public HashChunk fetchChunk(byte[] hash,long pos) throws IOException,
			DataArchivedException {
		HashChunk hashChunk = hs.getHashChunk(hash,pos);
		byte[] data = hashChunk.getData();
		kBytesFetched = kBytesFetched + (data.length / KBYTE);
		chunksFetched++;
		this.kBytesRead = kBytesFetched;
		this.chunksRead = this.chunksFetched;
		return hashChunk;
	}

	public void cacheChunk(byte[] hash,long pos) throws IOException,
			DataArchivedException {
		hs.cacheChunk(hash,pos);
	}

	public byte getHashRoute(byte[] hash) {
		byte hashRoute = (byte) (hash[1] / (byte) 16);
		if (hashRoute < 0) {
			hashRoute += 1;
			hashRoute *= -1;
		}
		return hashRoute;
	}

	public void processHashClaims(SDFSEvent evt) throws IOException {
		hs.processHashClaims(evt);
	}

	public long processHashClaims(SDFSEvent evt, LargeFileBloomFilter bf)
			throws IOException {
		return hs.processHashClaims(evt, bf);
	}

	public void commitChunks() {
		// H2HashStore.commitTransactions();
		unComittedChunks = 0;
	}

	public long getSize() {
		return hs.getEntries();
	}

	public long getMaxSize() {
		return hs.getMaxEntries();
	}

	public int getPageSize() {
		return Main.chunkStorePageSize;
	}

	public long getChunksRead() {
		return chunksRead;
	}

	public long getChunksWritten() {
		return chunksWritten;
	}

	public double getKBytesRead() {
		return kBytesRead;
	}

	public double getKBytesWrite() {
		return kBytesWrite;
	}

	public long getDupsFound() {
		return dupsFound;
	}

	public void close() {
		SDFSLogger.getLog().info("Closing Block Store");
		fileStore.close();
		SDFSLogger.getLog().info("Closing Hash Store");
		hs.close();
	}

	public void init() throws IOException {
		if (Main.runCompact) {
			DSECompaction.runCheck(hs.bdb,
					(FileChunkStore) this.getChuckStore());
			SDFSLogger.getLog().info("Finished compaction");

		}
	}

	@Override
	public void runConsistancyCheck() {
		SDFSLogger.getLog().info(
				"DSE did not close gracefully, running consistancy check");
		ConsistancyCheck.runCheck(hs.bdb, getChuckStore());

	}

	@Override
	public void sync() throws IOException {
		fileStore.sync();

	}

	@Override
	public long getCacheSize() {
		return fileStore.getCacheSize();
	}

	@Override
	public long getMaxCacheSize() {
		return fileStore.getMaxCacheSize();
	}

	@Override
	public int getReadSpeed() {
		return fileStore.getReadSpeed();
	}

	@Override
	public int getWriteSpeed() {
		return fileStore.getWriteSpeed();
	}

	@Override
	public String restoreBlock(byte[] hash) throws IOException {
		return hs.restoreBlock(hash);
	}

	@Override
	public boolean blockRestored(String id) throws IOException {
		return hs.blockRestored(id);
	}

	@Override
	public RemoteVolumeInfo[] getConnectedVolumes() throws IOException {
		if (fileStore instanceof AbstractCloudFileSync) {
			AbstractCloudFileSync af = (AbstractCloudFileSync)fileStore;
			return af.getConnectedVolumes();
		} else
			return null;
	}

}
