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

package io.engineblock.activityapi.core;

import java.util.concurrent.atomic.AtomicLong;

public class BaseOpContext implements OpContext {

    private final static Sink NULLSINK = new NullSink();
    private static AtomicLong idgen = new AtomicLong(0L);
    public final long ctxid = idgen.getAndIncrement();
    private long usages = 0L;

    private Sink sink = NULLSINK;

    private long delayNanos;
    private long startedAtNanos = 0L;
    private long endedAtNanos = 0L;

    private long cycle;
    private int result;

    private int tries=0;

    public BaseOpContext() {
    }

    @Override
    public OpContext reset() {
        startedAtNanos=0L;
        return this;
    }

    @Override
    public OpContext init(Sink sink, long cycle, long delayNanos) {
        this.sink = sink==null? NULLSINK : sink;
        this.endedAtNanos = Long.MIN_VALUE;
        this.cycle = cycle;
        this.delayNanos = delayNanos;
        this.startedAtNanos = System.nanoTime();
        usages++;
        return this;
    }



    @Override
    public OpContext start() {
        this.endedAtNanos = Long.MIN_VALUE;
        this.startedAtNanos = System.nanoTime();
        tries=1;
        usages++;
        return this;
    }

    public OpContext restart() {
        this.startedAtNanos = System.nanoTime();
        this.endedAtNanos = Long.MIN_VALUE;
        usages++;
        tries++;
        return this;
    }

    @Override
    public OpContext stop(int result) {
        this.endedAtNanos = System.nanoTime();
        this.result = result;
        this.sink.handle(this);
        synchronized(this) {
            notifyAll();
        }
        return this;
    }

    @Override
    public long getServiceTime() {
        return (endedAtNanos - startedAtNanos);
    }

    @Override
    public long getCumulativeServiceTime() {
        return System.nanoTime() - startedAtNanos;
    }

    @Override
    public long getFinalResponseTime() {
        return delayNanos + (endedAtNanos - startedAtNanos);
    }

    @Override
    public long getCumulativeResponseTime() {
        return delayNanos + (System.nanoTime() - startedAtNanos);

    }

    @Override
    public long getWaitTime() {
        return delayNanos;
    }

    @Override
    public int getResult() {
        return result;
    }

    @Override
    public int getTries() {
        return tries;
    }

    @Override
    public long getCycle() {
        return cycle;
    }

    @Override
    public long getCtxId() {
        return ctxid;
    }

    @Override
    public boolean isRunning() {
        return endedAtNanos < 0L;
    }

    @Override
    public String toString() {
        return "BasicOpContext{" +
                "(S-state,I-id,C-cycle,T-tries,U-use)=(S-" +
                (startedAtNanos<=0 ? "RESET" : (endedAtNanos<=0 ? "RUNNING" : "STOPPED") ) +
                ",I-" + ctxid +
                ",C-" + cycle +
                ",T-" + tries +
                ",U-" + usages +
                ")" +
                ", result=" + result +
                ", delayNanos=" + delayNanos +
                ", startedAtNanos=" + startedAtNanos +
                ", endedAtNanos=" + endedAtNanos +
                ", usages=" + usages +
                '}';
    }

    private final static class NullSink implements Sink {
        @Override
        public void handle(OpContext opc) {
        }
    }
}