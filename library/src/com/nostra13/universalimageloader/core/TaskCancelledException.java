package com.nostra13.universalimageloader.core;

/**
 * Exceptions for case when task is cancelled (thread is interrupted, image view is reused for another task, view is
 * collected by GC).
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.9.1
 */
class TaskCancelledException extends Exception {
}
