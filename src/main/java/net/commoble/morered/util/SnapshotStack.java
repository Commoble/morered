package net.commoble.morered.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class SnapshotStack<T> extends SnapshotJournal<T>
{
	private T currentValue;
	private final UnaryOperator<T> copyFunction;
	private final BiConsumer<T,T> onRootCommit;
	
	private SnapshotStack(T value, UnaryOperator<T> copyFunction, BiConsumer<T,T> onRootCommit)
	{
		this.currentValue = value;
		this.copyFunction = copyFunction;
		this.onRootCommit = onRootCommit;
	}
	
	public static <T> SnapshotStack<T> of(T initialValue, UnaryOperator<T> copyFunction)
	{
		return SnapshotStack.of(initialValue, copyFunction, Lambdas.emptyBiConsumer());
	}
	
	public static <T> SnapshotStack<T> of(T initialValue, UnaryOperator<T> copyFunction, BiConsumer<T,T> onRootCommit)
	{
		return new SnapshotStack<>(initialValue, copyFunction, onRootCommit);
	}
	
	public T get()
	{
		return this.currentValue;
	}
	
	public void set(T value)
	{
		this.currentValue = value;
	}
	
	public <R> R apply(Function<T,R> function)
	{
		return function.apply(this.currentValue);
	}
	
	public void update(Consumer<T> consumer)
	{
		consumer.accept(this.currentValue);
	}
	
	public void setAndTakeSnapshot(T value, TransactionContext context)
	{
		this.updateSnapshots(context);
		this.set(value);
	}
	
	public <R> R applyAndTakeSnapshot(Function<T,R> function, TransactionContext context)
	{
		this.updateSnapshots(context);
		R result = this.apply(function);
		return result;
	}
	
	public void updateAndTakeSnapshot(Consumer<T> consumer, TransactionContext context)
	{
		this.updateSnapshots(context);
		this.update(consumer);
	}
	
	@Override
	protected T createSnapshot()
	{
		return this.copyFunction.apply(this.currentValue);
	}

	@Override
	protected void revertToSnapshot(T snapshot)
	{
		this.currentValue = snapshot;
	}

	@Override
	protected void onRootCommit(T originalState)
	{
		super.onRootCommit(originalState);
		this.onRootCommit.accept(originalState, this.currentValue);
	}	
}
