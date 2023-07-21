package com.github.securityfilter.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class AccessUserCompletableFuture<T> extends CompletableFuture<T> {
    private final Object accessUser;

    public AccessUserCompletableFuture(Object accessUser) {
        this.accessUser = accessUser;
    }

    public AccessUserCompletableFuture() {
        this.accessUser = AccessUserUtil.getAccessUserIfExist();
    }

    public AccessUserCompletableFuture(CompletionStage<T> stage) {
        this.accessUser = AccessUserUtil.getAccessUserIfExist();
        stage.whenComplete((t, throwable) -> {
            if (throwable != null) {
                completeExceptionally(throwable);
            } else {
                complete(t);
            }
        });
    }

    public AccessUserCompletableFuture(Object accessUser, CompletionStage<T> stage) {
        this.accessUser = accessUser;
        stage.whenComplete((t, throwable) -> {
            if (throwable != null) {
                completeExceptionally(throwable);
            } else {
                complete(t);
            }
        });
    }

    public AccessUserCompletableFuture(Callable<? extends CompletionStage> futureSupplier) {
        this.accessUser = AccessUserUtil.getAccessUserIfExist();
        try {
            CompletionStage<T> future = futureSupplier.call();
            future.whenComplete((t, throwable) -> {
                if (throwable != null) {
                    completeExceptionally(throwable);
                } else {
                    complete(t);
                }
            });
        } catch (Throwable t) {
            PlatformDependentUtil.sneakyThrows(t);
        }
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return wrap(super.thenApply(runOnAccessUser(fn)));
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return wrap(super.thenApplyAsync(runOnAccessUser(fn)));
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return wrap(super.thenApplyAsync(runOnAccessUser(fn), executor));
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return wrap(super.thenAccept(runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return wrap(super.thenAcceptAsync(runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return wrap(super.thenAcceptAsync(runOnAccessUser(action), executor));
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return wrap(super.thenRun(runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return wrap(super.thenRunAsync(runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return wrap(super.thenRunAsync(runOnAccessUser(action), executor));
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return wrap(super.thenCombine(other, runOnAccessUserCombine(fn)));
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return wrap(super.thenCombineAsync(other, runOnAccessUserCombine(fn)));
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return wrap(super.thenCombineAsync(other, runOnAccessUserCombine(fn), executor));
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return wrap(super.thenAcceptBoth(other, runOnAccessUserBoth(action)));
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return wrap(super.thenAcceptBothAsync(other, runOnAccessUserBoth(action)));
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        return wrap(super.thenAcceptBothAsync(other, runOnAccessUserBoth(action), executor));
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return wrap(super.runAfterBoth(other, runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return wrap(super.runAfterBothAsync(other, runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return wrap(super.runAfterBothAsync(other, runOnAccessUser(action), executor));
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return wrap(super.applyToEither(other, runOnAccessUser(fn)));
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return wrap(super.applyToEitherAsync(other, runOnAccessUser(fn)));
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        return wrap(super.applyToEitherAsync(other, runOnAccessUser(fn), executor));
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return wrap(super.acceptEither(other, runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return wrap(super.acceptEitherAsync(other, runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        return wrap(super.acceptEitherAsync(other, runOnAccessUser(action), executor));
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return wrap(super.runAfterEither(other, runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return wrap(super.runAfterEitherAsync(other, runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return wrap(super.runAfterEitherAsync(other, runOnAccessUser(action), executor));
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrap(super.thenCompose((Function) runOnAccessUserCompose(fn)));
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrap(super.thenComposeAsync((Function) runOnAccessUserCompose(fn)));
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return wrap(super.thenComposeAsync((Function) runOnAccessUserCompose(fn), executor));
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return wrap(super.whenComplete(runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return wrap(super.whenCompleteAsync(runOnAccessUser(action)));
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return wrap(super.whenCompleteAsync(runOnAccessUser(action), executor));
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrap(super.handle(runOnAccessUser(fn)));
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrap(super.handleAsync(runOnAccessUser(fn)));
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return wrap(super.handleAsync(runOnAccessUser(fn), executor));
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return wrap(super.exceptionally(runOnAccessUserExceptionally(fn)));
    }

    @Override
    public boolean complete(T value) {
        return Boolean.TRUE.equals(AccessUserUtil.runOnAccessUser(accessUser, () -> super.complete(value)));
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        return Boolean.TRUE.equals(AccessUserUtil.runOnAccessUser(accessUser, () -> super.completeExceptionally(ex)));
    }

    @Override
    public void obtrudeException(Throwable ex) {
        AccessUserUtil.runOnAccessUser(accessUser, () -> super.obtrudeException(ex));
    }

    @Override
    public void obtrudeValue(T value) {
        AccessUserUtil.runOnAccessUser(accessUser, () -> super.obtrudeValue(value));
    }

    protected AccessUserCompletableFuture wrap(CompletableFuture future) {
        return new AccessUserCompletableFuture<>(accessUser, future);
    }

    protected Runnable runOnAccessUser(Runnable action) {
        return () -> AccessUserUtil.runOnAccessUser(accessUser, action::run);
    }

    protected Consumer<? super T> runOnAccessUser(Consumer<? super T> action) {
        return t -> AccessUserUtil.runOnAccessUser(accessUser, () -> action.accept(t));
    }

    protected <U> Function<? super T, ? extends U> runOnAccessUser(Function<? super T, ? extends U> fn) {
        return t -> AccessUserUtil.runOnAccessUser(accessUser, () -> fn.apply(t));
    }

    protected <U> Function<? super T, ? extends CompletionStage<U>> runOnAccessUserCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return t -> AccessUserUtil.runOnAccessUser(accessUser, () -> fn.apply(t));
    }

    protected <U> BiFunction<? super T, Throwable, ? extends U> runOnAccessUser(BiFunction<? super T, Throwable, ? extends U> fn) {
        return (t, u) -> AccessUserUtil.runOnAccessUser(accessUser, () -> fn.apply(t, u));
    }

    protected BiConsumer<? super T, ? super Throwable> runOnAccessUser(BiConsumer<? super T, ? super Throwable> fn) {
        return (t, u) -> AccessUserUtil.runOnAccessUser(accessUser, () -> fn.accept(t, u));
    }

    protected <U, V> BiFunction<? super T, ? super U, ? extends V> runOnAccessUserCombine(BiFunction<? super T, ? super U, ? extends V> fn) {
        return (t, u) -> AccessUserUtil.runOnAccessUser(accessUser, () -> fn.apply(t, u));
    }

    protected <U> BiConsumer<? super T, ? super U> runOnAccessUserBoth(BiConsumer<? super T, ? super U> fn) {
        return (t, u) -> AccessUserUtil.runOnAccessUser(accessUser, () -> fn.accept(t, u));
    }

    protected Function<Throwable, ? extends T> runOnAccessUserExceptionally(Function<Throwable, ? extends T> fn) {
        return (t) -> AccessUserUtil.runOnAccessUser(accessUser, () -> fn.apply(t));
    }

}
