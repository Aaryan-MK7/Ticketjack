package com.example.fairchance;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import java.util.concurrent.Executor;

public class FakeTask<T> extends Task<T> {

    private final boolean success;
    private final boolean canceled;
    @Nullable private final T result;
    @Nullable private final Exception exception;

    private FakeTask(boolean success, boolean canceled, @Nullable T result, @Nullable Exception exception) {
        this.success = success;
        this.canceled = canceled;
        this.result = result;
        this.exception = exception;
    }

    public static <T> FakeTask<T> success(@Nullable T value) {
        return new FakeTask<>(true, false, value, null);
    }

    public static <T> FakeTask<T> failure(@NonNull Exception e) {
        return new FakeTask<>(false, false, null, e);
    }

    public static <T> FakeTask<T> cancelled() {
        return new FakeTask<>(false, true, null, null);
    }

    // ---------- Task state ----------
    @Override public boolean isComplete() { return true; }
    @Override public boolean isSuccessful() { return success; }
    @Override public boolean isCanceled() { return canceled; }

    @Nullable @Override public T getResult() { return result; }

    @Override
    public @Nullable <X extends Throwable> T getResult(@NonNull Class<X> aClass) throws X {
        if (success) return result;
        //noinspection unchecked
        throw (X) exception;
    }

    @Nullable @Override public Exception getException() { return exception; }

    // ---------- Helper ----------
    private static void run(@Nullable Executor executor, @NonNull Runnable r) {
        if (executor != null) executor.execute(r);
        else r.run();
    }

    // ---------- Core listener overloads ----------
    @NonNull
    @Override
    public Task<T> addOnSuccessListener(@NonNull OnSuccessListener<? super T> listener) {
        if (success) listener.onSuccess(result);
        return this;
    }

    @NonNull
    @Override
    public Task<T> addOnFailureListener(@NonNull OnFailureListener listener) {
        if (!success && !canceled && exception != null) listener.onFailure(exception);
        return this;
    }

    @NonNull
    @Override
    public Task<T> addOnCanceledListener(@NonNull OnCanceledListener listener) {
        if (canceled) listener.onCanceled();
        return this;
    }

    @NonNull
    @Override
    public Task<T> addOnCompleteListener(@NonNull OnCompleteListener<T> listener) {
        listener.onComplete(this);
        return this;
    }

    // ---------- Activity overloads ----------
    @NonNull
    @Override
    public Task<T> addOnSuccessListener(@NonNull Activity activity,
                                        @NonNull OnSuccessListener<? super T> listener) {
        return addOnSuccessListener(listener);
    }

    @NonNull
    @Override
    public Task<T> addOnFailureListener(@NonNull Activity activity,
                                        @NonNull OnFailureListener listener) {
        return addOnFailureListener(listener);
    }

    @NonNull
    @Override
    public Task<T> addOnCanceledListener(@NonNull Activity activity,
                                         @NonNull OnCanceledListener listener) {
        return addOnCanceledListener(listener);
    }

    @NonNull
    @Override
    public Task<T> addOnCompleteListener(@NonNull Activity activity,
                                         @NonNull OnCompleteListener<T> listener) {
        return addOnCompleteListener(listener);
    }

    @NonNull
    @Override
    public Task<T> addOnSuccessListener(@NonNull Executor executor,
                                        @NonNull OnSuccessListener<? super T> listener) {
        if (success) run(executor, () -> listener.onSuccess(result));
        return this;
    }

    @NonNull
    @Override
    public Task<T> addOnFailureListener(@NonNull Executor executor,
                                        @NonNull OnFailureListener listener) {
        if (!success && !canceled && exception != null) run(executor, () -> listener.onFailure(exception));
        return this;
    }

    @NonNull
    @Override
    public Task<T> addOnCanceledListener(@NonNull Executor executor,
                                         @NonNull OnCanceledListener listener) {
        if (canceled) run(executor, listener::onCanceled);
        return this;
    }

    @NonNull
    @Override
    public Task<T> addOnCompleteListener(@NonNull Executor executor,
                                         @NonNull OnCompleteListener<T> listener) {
        run(executor, () -> listener.onComplete(this));
        return this;
    }
}
