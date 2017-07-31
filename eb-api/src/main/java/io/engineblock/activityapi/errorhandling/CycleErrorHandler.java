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

package io.engineblock.activityapi.errorhandling;

public interface CycleErrorHandler<T extends Throwable, R> {

    default R handleError(long cycle, T error) {
        return handleError(cycle, error, error.getMessage());
    }
    R handleError(long cycle, T error, String errMsg);

    public static class Triple<T,R> {
        public T error;
        public long cycle;
        public String msg;
        public R result;
        public Triple(T error, long cycle, String msg, R result) {
            this.error = error;
            this.cycle = cycle;
            this.msg = msg;
            this.result = result;
        }
    }


}