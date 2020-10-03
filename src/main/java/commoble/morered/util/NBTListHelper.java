package commoble.morered.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

/**
 * 
The MIT License (MIT)

Copyright (c) 2019 Joseph Bettendorff aka "Commoble"

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

/**
 * Helper class for writing a List<T> into a CompoundNBT
 * example usage for use in e.g. a WorldSavedData, a TileEntity, etc:
 * 
 * private static final String POSITIONS = "positions";
 * private static final String BLOCKPOS = "blockpos";
 * 
 * private List<BlockPos> list = new ArrayList<>();
 * 
 * private static final NBTListHelper<BlockPos> BLOCKPOS_LISTER = new NBTListHelper<>(
 * 		POSITIONS,
 * 		(nbt, blockPos) -> nbt.put(BLOCKPOS, NBTUtil.writeBlockPos(blockPos)),
 * 		nbt -> NBTUtil.readBlockPos(nbt.getCompound(BLOCKPOS))
 * );
 * 
 * 	@Override
 *	public void read(CompoundNBT nbt)
 *	{
 *		this.list = BLOCKPOS_LISTER.read(nbt);
 *	}
 *	@Override
 *	public CompoundNBT write(CompoundNBT compound)
 *	{
 *		BLOCKPOS_LISTER.write(this.list, compound);
 *		return compound;
 *	}
 * @author Joseph
 *
 * @param <T>
 */
public class NBTListHelper<T>
{
	private final String name;
	private final Function<T, CompoundNBT> elementWriter;
	private final Function<CompoundNBT, T> elementReader;
	
	/**
	 * @param name A unique identifier for the list's representation in the CompoundNBT
	 * @param elementWriter A function that, given a list element, creates a new NBT tag containing that element
	 * @param elementReader A function that, given a CompoundNBT, returns the list element that was written into that NBT
	 */
	public NBTListHelper(
			String name,
			Function<T, CompoundNBT> elementWriter,
			Function<CompoundNBT, T> elementReader)
	{
		this.name = name;
		this.elementWriter = elementWriter;
		this.elementReader = elementReader;
	}
	
	/**
	 * Reconstructs and returns a List<T> from a CompoundNBT
	 * If the nbt used was given by this.write(list), the list returned will be a reconstruction of the original List
	 * @param compound The CompoundNBT to read and construct the List from
	 * @return A List that the data contained in the CompoundNBT represents
	 */
	public List<T> read(final CompoundNBT compound)
	{
		final List<T> newList = new ArrayList<>();
		
		final ListNBT listNBT = compound.getList(this.name, 10);
		if (listNBT == null)
			return newList;
		
		final int listSize = listNBT.size();
		
		if (listSize <= 0)
			return newList;
		
		IntStream.range(0, listSize).mapToObj(i -> listNBT.getCompound(i))
			.forEach(nbt -> newList.add(this.elementReader.apply(nbt)));
		
		return newList;
	}
	
	/**
	 * Given a list and a CompoundNBT,writes the contents of that list into the NBT
	 * The same CompoundNBT can be given to this.read to retrieve that map 
	 * @param list A List<T> to write into the nbt
	 * @param compound A CompoundNBT to write the list into
	 * @return A CompoundNBT that, when used as the argument to this.read(nbt), will cause that function to reconstruct and return a copy of the original list
	 */
	public CompoundNBT write(final List<T> list, final CompoundNBT compound)
	{
		final ListNBT nbtList = new ListNBT();
		
		list.forEach(element -> nbtList.add(this.elementWriter.apply(element)));
		
		compound.put(this.name, nbtList);
		
		return compound;
	}
}