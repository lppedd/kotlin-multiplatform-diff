/*
 * Copyright 2021 Peter Trifanov.
 * Copyright 2021 java-diff-utils.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file has been modified by Peter Trifanov when porting from Java to Kotlin.
 */
package io.github.petertrr.diffutils.patch

public class ExceptionProducingConflictOutput<T> : ConflictOutput<T> {
    override fun processConflict(verifyChunk: VerifyChunk, delta: Delta<T>, result: MutableList<T>) {
        if (verifyChunk != VerifyChunk.OK) {
            throw PatchFailedException("Could not apply patch due to: $verifyChunk")
        }
    }
}
