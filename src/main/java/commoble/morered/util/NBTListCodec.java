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


package commoble.morered.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * Helper class for converting lists or list-like collections of arbitrary data to NBT and back.
 * 
 * @param <ENTRY> the data type in the actual java list, i.e. {@literal List<ENTRY>}
 * @param <RAW> either a primitive or an NBT collection type, see ListTagType instances
 */
public class NBTListCodec<ENTRY, RAW>
{
	private final String name;
	private final ListTagType<RAW> type;
	private final Function<ENTRY, Tag> elementWriter;
	private final Function<RAW, ENTRY> elementReader;
	
	/**
	 * 
	 * @param name A string to use as the field name when we write the list into existing nbt
	 * @param type A ListTagType that describes how to get data out of the list and convert primitives to NBT
	 * @param elementWriter A function that converts an entry instance to the intermediate raw type
	 * @param elementReader A function that converts the raw instance back into an entry instance
	 */
	public NBTListCodec(
			String name,
			ListTagType<RAW> type,
			Function<ENTRY, RAW> elementWriter,
			Function<RAW, ENTRY> elementReader)
	{
		this.name = name;
		this.type = type;
		this.elementWriter = elementWriter.andThen(this.type.serializer);
		this.elementReader = elementReader;
	}
	
	/**
	 * Reconstructs and returns a {@literal List<ENTRY>} from a CompoundTag
	 * If the nbt used was given by this.write(list), the list returned will be a reconstruction of the original List
	 * @param compound The CompoundTag to read and construct the List from
	 * @return A List that the data contained in the CompoundTag represents
	 */
	public List<ENTRY> read(final CompoundTag compound)
	{
		final List<ENTRY> newList = new ArrayList<>();
		
		final ListTag listTag = compound.getList(this.name, this.type.tagID);
		if (listTag == null)
			return newList;
		
		final int listSize = listTag.size();
		
		if (listSize <= 0)
			return newList;
		
		IntStream.range(0, listSize).mapToObj(i -> this.type.listReader.apply(listTag, i))
			.forEach(nbt -> newList.add(this.elementReader.apply(nbt)));
		
		return newList;
	}
	
	/**
	 * Given a list and a CompoundTag,writes the contents of that list into the NBT
	 * The same CompoundTag can be given to this.read to retrieve that map 
	 * @param list A {@literal List<ENTRY>} to write into the nbt
	 * @param compound A CompoundTag to write the list into
	 * @return A CompoundTag that, when used as the argument to this.read(nbt), will cause that function to reconstruct and return a copy of the original list
	 */
	public CompoundTag write(final List<ENTRY> list, final CompoundTag compound)
	{
		final ListTag nbtList = new ListTag();
		
		list.forEach(element -> nbtList.add(this.elementWriter.apply(element)));
		
		compound.put(this.name, nbtList);
		
		return compound;
	}
	
	/**
	 * Represents a function that takes a list NBT and an index and returns the object in the list at that index.
	 * T must match the NBT type of the ListTag's element or the type of the object they represent,
	 * e.g. Integer, String, CompoundTag, etc
	 */
	@FunctionalInterface
	public static interface ListReader<T>
	{
		/**
		 * 
		 * @param list a list nbt
		 * @param index an index in the list
		 * @return an nbt element at the given index in the list
		 */
		public T apply(ListTag list, int index);
	}
	
	/**
	 * @param <T> tag type
	 */
	public static class ListTagType<T>
	{
		/** byte tag type **/
		public static final ListTagType<Byte> BYTE = new ListTagType<>(Tag.TAG_BYTE, (list, i) -> (byte)(list.getInt(i)), ByteTag::valueOf);
		/** short tag type **/
		public static final ListTagType<Short> SHORT = new ListTagType<>(Tag.TAG_SHORT, ListTag::getShort, ShortTag::valueOf);
		/** int tag type **/
		public static final ListTagType<Integer> INTEGER = new ListTagType<>(Tag.TAG_INT, ListTag::getInt, IntTag::valueOf);
		/** float tag type **/
		public static final ListTagType<Float> FLOAT = new ListTagType<>(Tag.TAG_FLOAT, ListTag::getFloat, FloatTag::valueOf);
		/** double tag type **/
		public static final ListTagType<Double> DOUBLE = new ListTagType<>(Tag.TAG_DOUBLE, ListTag::getDouble, DoubleTag::valueOf);
		/** string tag type **/
		public static final ListTagType<String> STRING = new ListTagType<>(Tag.TAG_STRING, ListTag::getString, StringTag::valueOf);
		/** list tag type **/
		public static final ListTagType<ListTag> LIST = new ListTagType<>(Tag.TAG_LIST, ListTag::getList, x->x);
		/** map tag type **/
		public static final ListTagType<CompoundTag> COMPOUND = new ListTagType<>(Tag.TAG_COMPOUND, ListTag::getCompound, x->x);
		
		/** see Tag's constants, needed for ListTags to work safely **/
		final int tagID;
		final ListReader<T> listReader;
		final Function<T, Tag> serializer;
		
		/**
		 * @param tagID int id of the nbt type
		 * @param listReader ListReader for indexing the list
		 * @param serializer Function to serialize a T to an nbt Tag
		 */
		public ListTagType(int tagID, ListReader<T> listReader, Function<T, Tag> serializer)
		{
			this.tagID = tagID;
			this.listReader = listReader;
			this.serializer = serializer;
		}
	}
}