/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */

package io.engineblock.activityapi.cycletracking.outputs.cyclelog;

import io.engineblock.activityapi.Activity;
import io.engineblock.activityapi.cycletracking.buffers.results.CycleResult;
import io.engineblock.activityapi.cycletracking.buffers.results.CycleResultsSegment;
import io.engineblock.activityapi.cycletracking.buffers.results_rle.CycleResultsRLEBufferTarget;
import io.engineblock.activityapi.cycletracking.buffers.results_rle.CycleSpanResults;
import io.engineblock.activityapi.output.Output;
import io.engineblock.util.SimpleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

/**
 * A {@link Output} that writes cycles and results to an RLE-based file format.
 *
 * This marker creates a file on disk and appends one or more (long,long,byte)
 * tuples to it as buffering extents are filled. This tuple format represents
 * the closed-open interval of cycles and the result associated with them.
 * The file is expected to contain only cycle ranges in order.
 *
 * <p>It <em>is</em> valid for RLE segments to be broken apart into contiguous
 * ranges. Any implementation should treat this as normal.
 */
public class CycleLogOutput implements Output {

    // For use in allocating file data, etc
    private final static Logger logger = LoggerFactory.getLogger(CycleLogOutput.class);
    private MappedByteBuffer mbb;
    private RandomAccessFile file;
//    private FileBufferConfig config;
    private CycleResultsRLEBufferTarget targetBuffer;
    private int extentSizeInSpans;
    private File outputFile;

    public CycleLogOutput(Activity activity) {

        SimpleConfig conf = new SimpleConfig(activity,"output");
        this.extentSizeInSpans = conf.getInteger("extentSize").orElse(1000);
        this.outputFile = new File(conf.getString("file").orElse(activity.getAlias())+".cyclelog");


        targetBuffer = new CycleResultsRLEBufferTarget(extentSizeInSpans);
        removeIfPresent(outputFile);
    }

    public CycleLogOutput(File outputFile, int extentSizeInSpans) {
        this.extentSizeInSpans = extentSizeInSpans;
        this.outputFile = outputFile;
        targetBuffer = new CycleResultsRLEBufferTarget(extentSizeInSpans);
        removeIfPresent(outputFile);
    }

    private void removeIfPresent(File filename) {
        try {
            if (Files.deleteIfExists(filename.toPath())) {
                logger.warn("removed extant file '" + filename + "'");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public boolean onCycleResult(long completedCycle, int result) {
        onCycleResultSegment(new CycleSpanResults(completedCycle,completedCycle+1,result));
        return true;
    }

    @Override
    public void onCycleResultSegment(CycleResultsSegment segment) {
        for (CycleResult cycleResult : segment) {
            boolean buffered = targetBuffer.onCycleResult(cycleResult);
            if (!buffered) {
                flush();
                targetBuffer = new CycleResultsRLEBufferTarget(extentSizeInSpans);
                boolean bufferedAfterFlush = targetBuffer.onCycleResult(cycleResult);
                if (!bufferedAfterFlush) {
                    throw new RuntimeException("Failed to record result in new target buffer");
                }
            }
        }
    }

    private void flush() {
        ByteBuffer nextFileExtent = targetBuffer.toByteBuffer();
        logger.debug("RLE result extent is " + nextFileExtent.remaining() + " bytes ("
                + (nextFileExtent.remaining() / CycleResultsRLEBufferTarget.BYTES)
                + ") tuples");
        int targetCapacity = (mbb == null ? 0 : mbb.capacity()) + nextFileExtent.remaining();
        logger.trace("ensuring capacity for " + targetCapacity);
        this.ensureCapacity(targetCapacity);
        mbb.put(nextFileExtent);
        logger.trace("extent appended");
        logger.trace("mbb position now at " + mbb.position());

    }

    @Override
    public void close() throws Exception {
        try {
        flush();
        if (mbb!=null) {
            mbb.force();
            mbb=null;
        }
        if (file != null) {
            file.getFD().sync();
            file.close();
            file = null;
        }
        } catch (Throwable t) {
            logger.error("Error while closing CycleLogOutput: " + t, t);
            throw t;
        }

    }

    private synchronized void ensureCapacity(long newCapacity) {
        try {
            logger.info("resizing marking file from " + (mbb == null ? 0 : mbb.capacity()) + " to " + newCapacity);
            if (file == null) {
                file = new RandomAccessFile(outputFile, "rw");
                file.seek(0);
                mbb = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, newCapacity);
            } else {
                int pos = mbb.position();
                file.setLength(newCapacity);
                file.seek(pos);
                mbb = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, newCapacity);
                mbb.position(pos);
            }
            logger.trace("mbb position now at " + mbb.position());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "CycleLogOutput{" +
                "mbb=" + mbb +
                ", file=" + file +
                ", mbb=" + (mbb==null ? "null" : "(pos=" + mbb.position() +", limit=" + mbb.limit() +", capacity=" + mbb.capacity()+")") +
                '}';
    }

//    private class FileBufferConfig {
//        public final String filename; // Where the
//        public final int extentSize; // logical chunksize boundary to fill to on overrun
//
//        public FileBufferConfig(ActivityDef activityDef) {
//            Optional<String> marker = activityDef.getParams().getOptionalString("output");
//            marker.orElseThrow(() -> new RuntimeException("marker parameter is missing?"));
//            logger.debug("parsing marker config:" + marker.get());
//            Map<String, String> params =
//                    Arrays.stream(marker.get().split(",", 2)[1].split(","))
//                            .map(s -> s.split("="))
//                            .collect(Collectors.toMap(o -> o[0], o -> o[1]));
//            this.filename = params.getOrDefault("file", activityDef.getAlias()) + ".cyclelog";
//            int suggested_extentsize = Integer.valueOf(params.getOrDefault("extentSize", String.valueOf(1024 * 1024)));
//            extentSize = suggested_extentsize * CycleResultsRLEBufferTarget.BYTES;
//
//        }
//
//        @Override
//        public String toString() {
//            return "FileBufferConfig{" +
//                    "filename='" + outputFile + '\'' +
//                    ", extentSizeInSpans=" + extentSizeInSpans+
//                    '}';
//        }
//    }
}
