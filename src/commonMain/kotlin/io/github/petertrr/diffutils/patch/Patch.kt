/*
 * Copyright 2021 Peter Trifanov.
 * Copyright 2009-2017 java-diff-utils.
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

import io.github.petertrr.diffutils.algorithm.Change
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * Describes the patch holding all deltas between the original and revised texts.
 *
 * @param conflictOutput Alter normal conflict output behaviour to e.g. include
 *   some conflict statements in the result, like Git does it.
 * @param T The type of the compared elements in the 'lines'.
 */
public class Patch<T>(private var conflictOutput: ConflictOutput<T> = ExceptionProducingConflictOutput()) {
    public var deltas: MutableList<Delta<T>> = ArrayList()
        get() {
            field.sortBy { it.source.position }
            return field
        }

    /**
     * Apply this patch to the given target list, returning a new list.
     *
     * @return The patched text
     * @throws PatchFailedException If the patch cannot be applied
     */
    public fun applyTo(target: List<T>): List<T> {
        val result = target.toMutableList()
        applyToExisting(result)
        return result
    }

    /**
     * Apply this patch to the given target list, directly modifying it.
     *
     * @return The patched text
     * @throws PatchFailedException If the patch cannot be applied
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public fun applyToExisting(target: MutableList<T>) {
        val it = deltas.listIterator(deltas.size)

        while (it.hasPrevious()) {
            val delta = it.previous()
            val verifyChunk = delta.verifyAndApplyTo(target)
            conflictOutput.processConflict(verifyChunk, delta, target)
        }
    }

    /**
     * Creates a new list, containing the restored state of the given target list.
     * Opposite of the [applyTo] method.
     *
     * @param target The given target
     * @return The restored text
     */
    public fun restore(target: List<T>): List<T> {
        val result = target.toMutableList()
        restoreToExisting(result)
        return result
    }

    /**
     * Restores all changes within the given target list.
     * Opposite of the [applyToExisting] method.
     *
     * @param target The given target
     * @return The restored text
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public fun restoreToExisting(target: MutableList<T>) {
        val it = deltas.listIterator(deltas.size)

        while (it.hasPrevious()) {
            val delta = it.previous()
            delta.restore(target)
        }
    }

    /**
     * Add the given delta to this patch.
     *
     * @param delta The delta to add
     */
    public fun addDelta(delta: Delta<T>): Boolean =
        deltas.add(delta)

    override fun toString(): String =
        "Patch{deltas=$deltas}"

    public fun withConflictOutput(conflictOutput: ConflictOutput<T>) {
        this.conflictOutput = conflictOutput
    }

    public companion object {
        @JvmStatic
        @JvmOverloads
        public fun <T> generate(
            original: List<T>,
            revised: List<T>,
            changes: List<Change>,
            includeEquals: Boolean = false,
        ): Patch<T> {
            val patch = Patch<T>()
            var startOriginal = 0
            var startRevised = 0

            val adjustedChanges = if (includeEquals) {
                changes.sortedBy(Change::startOriginal)
            } else {
                changes
            }

            for (change in adjustedChanges) {
                if (includeEquals && startOriginal < change.startOriginal) {
                    patch.addDelta(
                        EqualDelta(
                            buildChunk(startOriginal, change.startOriginal, original),
                            buildChunk(startRevised, change.startRevised, revised),
                        )
                    )
                }

                val orgChunk = buildChunk(change.startOriginal, change.endOriginal, original)
                val revChunk = buildChunk(change.startRevised, change.endRevised, revised)

                when (change.deltaType) {
                    DeltaType.DELETE -> patch.addDelta(DeleteDelta(orgChunk, revChunk))
                    DeltaType.INSERT -> patch.addDelta(InsertDelta(orgChunk, revChunk))
                    DeltaType.CHANGE -> patch.addDelta(ChangeDelta(orgChunk, revChunk))
                    DeltaType.EQUAL -> {}
                }

                startOriginal = change.endOriginal
                startRevised = change.endRevised
            }

            if (includeEquals && startOriginal < original.size) {
                patch.addDelta(
                    EqualDelta(
                        buildChunk(startOriginal, original.size, original),
                        buildChunk(startRevised, revised.size, revised),
                    )
                )
            }

            return patch
        }

        private fun <T> buildChunk(start: Int, end: Int, data: List<T>): Chunk<T> =
            Chunk(start, data.slice(start..<end))
    }
}
